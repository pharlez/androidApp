/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.unfold.android.tsibato.images;

import java.lang.ref.WeakReference;

import gr.unfold.android.tsibato.BuildConfig;
import gr.unfold.android.tsibato.images.ImageCache;
import gr.unfold.android.tsibato.util.Utils;
import gr.unfold.android.tsibato.drawable.RecyclingBitmapDrawable;

import android.os.AsyncTask;
import android.os.Build;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.ImageView;


/**
* This class wraps up completing some arbitrary long running work when loading a bitmap to an
* ImageView. It handles things like using a memory and disk cache, running the work in a background
* thread and setting a placeholder image.
*/
public abstract class ImageWorker {
   private static final String TAG = "ImageWorker";
   private static final int FADE_IN_TIME = 200;

   private ImageCache mImageCache;
   private ImageCache.ImageCacheParams mImageCacheParams;
   private Bitmap mLoadingBitmap;
   private boolean mFadeInBitmap = true;
   private boolean mExitTasksEarly = false;
   protected boolean mPauseWork = false;
   private final Object mPauseWorkLock = new Object();

   protected Resources mResources;

   private static final int MESSAGE_CLEAR = 0;
   private static final int MESSAGE_INIT_DISK_CACHE = 1;
   private static final int MESSAGE_FLUSH = 2;
   private static final int MESSAGE_CLOSE = 3;

   protected ImageWorker(Context context) {
       mResources = context.getResources();
   }
   
   /**
    * Load an image specified by the data parameter into an ImageView using a memory and
    * disk cache. If the image is found in the memory cache, it is set immediately, otherwise an {@link AsyncTask}
    * will be created to asynchronously load the bitmap.
    */
   public void loadImage(Object data, ImageView imageView) {
	   if (data == null) {
		   return;
	   }
	   
	   BitmapDrawable value = null;
	   
	   if (mImageCache != null) {
		   value = mImageCache.getBitmapFromMemCache(String.valueOf(data));
	   }
	   
	   if (value != null) {
		   imageView.setImageDrawable(value);
	   } else if (cancelPotentialWork(data, imageView)) {
		   final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
           final AsyncDrawable asyncDrawable =
                   new AsyncDrawable(mResources, mLoadingBitmap, task);
           imageView.setImageDrawable(asyncDrawable);
           
           task.execute(data);
	   }
   }
   
   public void setLoadingImage(int resId) {
	   mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
   }
   
   public void addImageCache(FragmentManager fragmentManager, ImageCache.ImageCacheParams cacheParams) {
	   mImageCacheParams = cacheParams;
	   mImageCache = ImageCache.getInstance(fragmentManager, mImageCacheParams);
	   new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
   }
   
   public void setExitTasksEarly(boolean exitTasksEarly) {
       mExitTasksEarly = exitTasksEarly;
       setPauseWork(false);
   }
   
  /** Main method for subclasses to override to define any processing that must happen to produce
    * the final bitmap. This will be executed in a background thread and be long running. */
   protected abstract Bitmap processBitmap(Object data);
   
   protected ImageCache getImageCache() {
       return mImageCache;
   }
   
  /** Returns true if the current work has been cancelled or if there is no work in progress, 
    * false if the work in progress deals with the same data. */
   public static boolean cancelPotentialWork(Object data, ImageView imageView) {
	   final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

       if (bitmapWorkerTask != null) {
           final Object bitmapData = bitmapWorkerTask.data;
           if (bitmapData == null || !bitmapData.equals(data)) {
               bitmapWorkerTask.cancel(true);
               if (BuildConfig.DEBUG) {
                   Log.d(TAG, "cancelPotentialWork - cancelled work for " + data);
               }
           } else {
               // The same work is already in progress.
               return false;
           }
       }
       return true;
   }
   
   /** Retrieve the currently active work task (if any) associated with this imageView, or null if there is no such task. */
   private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
       if (imageView != null) {
           final Drawable drawable = imageView.getDrawable();
           if (drawable instanceof AsyncDrawable) {
               final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
               return asyncDrawable.getBitmapWorkerTask();
           }
       }
       return null;
   }
   
   /** The actual AsyncTask that will asynchronously process the image. */
   private class BitmapWorkerTask extends AsyncTask<Object, Void, BitmapDrawable> {
	   private Object data;
	   private final WeakReference<ImageView> imageViewReference;
	   
	   public BitmapWorkerTask(ImageView imageView) {
           imageViewReference = new WeakReference<ImageView>(imageView);
       }
	   
	   @Override
	   protected BitmapDrawable doInBackground(Object... params) {
		   if (BuildConfig.DEBUG) {
               Log.d(TAG, "doInBackground - starting work");
           }
		   
		   data = params[0];
		   final String dataString = String.valueOf(data);
		   Bitmap bitmap = null;
		   BitmapDrawable drawable = null;
		   
		   synchronized (mPauseWorkLock) {
			   while (mPauseWork && !isCancelled()) {
				   try {
					   mPauseWorkLock.wait();
				   } catch (InterruptedException e) {}
			   }
		   }
		   
		   // If the image cache is available and task has not been cancelled and the ImageView is still
		   // bound back to this task and our "exit early" flag is not set then fetch the bitmap from the disk cache
		   if (mImageCache != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
			   bitmap = mImageCache.getBitmapFromDiskCache(dataString);
		   }
		   
		   // If the image not found in cache and task has not been cancelled and the ImageView is still
		   // bound back to this task and our "exit early" flag is not set then fetch call the main process method
		   if (bitmap == null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
			   bitmap = processBitmap(params[0]);
		   }
		   
		   if (bitmap != null) {
			   if (Utils.hasHoneycomb()) {
				   drawable = new BitmapDrawable(mResources, bitmap);
			   } else {
				   // Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable which will recycle automatically
				   drawable = new RecyclingBitmapDrawable(mResources, bitmap);
			   }
			   
			   if (mImageCache != null) {
				   mImageCache.addBitmapToCache(dataString, drawable);
			   }
		   }
		   
		   if (BuildConfig.DEBUG) {
               Log.d(TAG, "doInBackground - finished work");
           }

           return drawable;
	   }
	   
	   @Override
       protected void onPostExecute(BitmapDrawable value) {
           // if cancel was called on this task or the "exit early" flag is set then we're done
           if (isCancelled() || mExitTasksEarly) {
               value = null;
           }

           final ImageView imageView = getAttachedImageView();
           if (value != null && imageView != null) {
               if (BuildConfig.DEBUG) {
                   Log.d(TAG, "onPostExecute - setting bitmap");
               }
               setImageDrawable(imageView, value);
           }
       }
	   
	   @Override
	   protected void onCancelled() {
		   super.onCancelled();
           synchronized (mPauseWorkLock) {
               mPauseWorkLock.notifyAll();
           }
	   }
	   
	   @Override
       protected void onCancelled(BitmapDrawable value) {
           super.onCancelled();
           synchronized (mPauseWorkLock) {
               mPauseWorkLock.notifyAll();
           }
       }
	   
	  /** Returns the ImageView associated with this task as long as the ImageView's task still
        * points to this task as well. Returns null otherwise. */
       private ImageView getAttachedImageView() {
           final ImageView imageView = imageViewReference.get();
           final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

           if (this == bitmapWorkerTask) {
               return imageView;
           }

           return null;
       }
   }
   
   /**
    * A custom Drawable that will be attached to the imageView while the work is in progress.
    * Contains a reference to the actual worker task, so that it can be stopped if a new binding is
    * required, and makes sure that only the last started worker process can bind its result,
    * independently of the finish order.
    */
   private static class AsyncDrawable extends BitmapDrawable {
       private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

       public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
           super(res, bitmap);
           bitmapWorkerTaskReference =
               new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
       }

       public BitmapWorkerTask getBitmapWorkerTask() {
           return bitmapWorkerTaskReference.get();
       }
   }
   
   @TargetApi(16)
   @SuppressWarnings("deprecation")
   private void setImageDrawable(ImageView imageView, Drawable drawable) {
       if (mFadeInBitmap) {
           // Transition drawable with a transparent drawable and the final drawable
           final TransitionDrawable td =
                   new TransitionDrawable(new Drawable[] {
                           new ColorDrawable(android.R.color.transparent),
                           drawable
                   });
           // Set background to loading bitmap
           if (Utils.hasJellyBean()) {
        	   imageView.setBackground(new BitmapDrawable(mResources, mLoadingBitmap));
           } else {
        	   imageView.setBackgroundDrawable(new BitmapDrawable(mResources, mLoadingBitmap));
           }           

           imageView.setImageDrawable(td);
           td.startTransition(FADE_IN_TIME);
       } else {
           imageView.setImageDrawable(drawable);
       }
   }
   
   public void setPauseWork(boolean pauseWork) {
       synchronized (mPauseWorkLock) {
           mPauseWork = pauseWork;
           if (!mPauseWork) {
               mPauseWorkLock.notifyAll();
           }
       }
   }
   
   protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

       @Override
       protected Void doInBackground(Object... params) {
           switch ((Integer)params[0]) {
               case MESSAGE_CLEAR:
                   clearCacheInternal();
                   break;
               case MESSAGE_INIT_DISK_CACHE:
                   initDiskCacheInternal();
                   break;
               case MESSAGE_FLUSH:
                   flushCacheInternal();
                   break;
               case MESSAGE_CLOSE:
                   closeCacheInternal();
                   break;
           }
           return null;
       }
   }
   
   protected void initDiskCacheInternal() {
       if (mImageCache != null) {
           mImageCache.initDiskCache();
       }
   }

   protected void clearCacheInternal() {
       if (mImageCache != null) {
           mImageCache.clearCache();
       }
   }

   protected void flushCacheInternal() {
       if (mImageCache != null) {
           mImageCache.flush();
       }
   }

   protected void closeCacheInternal() {
       if (mImageCache != null) {
           mImageCache.close();
           mImageCache = null;
       }
   }

   public void clearCache() {
       new CacheAsyncTask().execute(MESSAGE_CLEAR);
   }

   public void flushCache() {
       new CacheAsyncTask().execute(MESSAGE_FLUSH);
   }

   public void closeCache() {
	   new CacheAsyncTask().execute(MESSAGE_CLOSE);
   }
   
}

