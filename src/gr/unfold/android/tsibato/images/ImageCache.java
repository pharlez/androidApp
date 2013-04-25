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

import gr.unfold.android.tsibato.BuildConfig;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;

import gr.unfold.android.tsibato.util.DiskLruCache;
import gr.unfold.android.tsibato.util.Utils;
import gr.unfold.android.tsibato.drawable.RecyclingBitmapDrawable;

public class ImageCache {
	private static final String TAG = "ImageCache";
	
	// Default memory cache size in kilobytes
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 5; // 5MB
    
 // Default disk cache size in bytes
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    
 // Compression settings when writing images to disk cache
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;
    private static final int DISK_CACHE_INDEX = 0;

    // Constants to easily toggle various caches
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = false;
    
    private DiskLruCache mDiskLruCache;
    private LruCache<String, BitmapDrawable> mMemoryCache;
    private ImageCacheParams mCacheParams;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    
    private HashSet<SoftReference<Bitmap>> mReusableBitmaps;
    
    private ImageCache(ImageCacheParams cacheParams) {
    	init(cacheParams);
    }
    
    public static ImageCache getInstance(FragmentManager fragmentManager, ImageCacheParams cacheParams) {
    	final RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager);
    	
    	ImageCache imageCache = (ImageCache) mRetainFragment.getObject();
    	
    	if (imageCache == null) {
    		imageCache = new ImageCache(cacheParams);
    		mRetainFragment.setObject(imageCache);
    	}
    	
    	return imageCache;
    }
    
    /** Initialise the cache, providing all parameters. */
    private void init(ImageCacheParams cacheParams) {
    	mCacheParams = cacheParams;
    	
    	if (mCacheParams.memoryCacheEnabled) {
    		if (BuildConfig.DEBUG) {
    			Log.d(TAG, "Memory cache created (size = " + mCacheParams.memCacheSize + ")");
    		}
    		
    		if (Utils.hasHoneycomb()) {
    			mReusableBitmaps = new HashSet<SoftReference<Bitmap>>();
    		}
    		
    		mMemoryCache = new LruCache<String, BitmapDrawable>(mCacheParams.memCacheSize) {
    			@Override
    			protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
    				if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
                        // The removed entry is a recycling drawable, so notify it that it has been removed from the memory cache
                        ((RecyclingBitmapDrawable) oldValue).setIsCached(false);
                    } else {
                        // The removed entry is a standard BitmapDrawable
                        if (Utils.hasHoneycomb()) {
                            // We're running on Honeycomb or later, so add the bitmap to a SoftRefrence set for possible use with inBitmap later
                            mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
                        }
                    }
    			}
    			
    			@Override
    			protected int sizeOf(String key, BitmapDrawable value) {
    				final int bitmapSize = getBitmapSize(value) / 1024;
    				return bitmapSize == 0 ? 1 : bitmapSize;
    			}
    		};
    	}
    	
    	if (cacheParams.initDiskCacheOnCreate) {
    		initDiskCache();
    	}
    }
    
    /** Initialises the disk cache.  Note that this includes disk access so it should not be invoked from a background thread. */
    public void initDiskCache() {
    	synchronized (mDiskCacheLock) {
    		if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
    			File diskCacheDir = mCacheParams.diskCacheDir;
    			if (mCacheParams.diskCacheEnabled && diskCacheDir != null) {
    				if (!diskCacheDir.exists()) {
    					diskCacheDir.mkdirs();
    				}
    				if (getUsableSpace(diskCacheDir) > mCacheParams.diskCacheSize) {
    					try {
    						mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, mCacheParams.diskCacheSize);
    						if (BuildConfig.DEBUG) {
    							Log.d(TAG, "Disk cache initialized");
    						}
    					} catch (final IOException e) {
    						mCacheParams.diskCacheDir = null;
    						Log.e(TAG, "initDiskCache - " + e);
    					}
    				}
    			}
    		}
    		mDiskCacheStarting = false;
    		mDiskCacheLock.notifyAll();
    	}
    }
    
    /** Adds a bitmap to both memory and disk cache. */
    public void addBitmapToCache(String data, BitmapDrawable value) {
    	if (data == null || value == null) {
    		return;
    	}
    	
    	if (mMemoryCache != null) {
    		if (RecyclingBitmapDrawable.class.isInstance(value)) {
                // The removed entry is a recycling drawable, so notify it that it has been added into the memory cache
                ((RecyclingBitmapDrawable) value).setIsCached(true);
            }
    		mMemoryCache.put(data, value);
    	}
    	
    	synchronized (mDiskCacheLock) {
    		if (mDiskLruCache != null ) {
    			final String key = hashKeyForDisk(data);
    			OutputStream out = null;
    			try {
    				DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
    				if (snapshot == null) {
    					final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
    					if (editor != null) {
    						out = editor.newOutputStream(DISK_CACHE_INDEX);
    						value.getBitmap().compress(mCacheParams.compressFormat, mCacheParams.compressQuality, out);
    						editor.commit();
    						out.close();
    					}
    				}
    			} catch (final IOException e) {
    				Log.e(TAG, "addBitmapToCache - " + e);
    			} catch (Exception e) {
    				Log.e(TAG, "addBitmapToCache - " + e);
    			} finally {
    				try {
    					if (out != null) {
    						out.close();
    					}
    				} catch (IOException e) {}
    			}
    		}
    	}
    }
    
    public BitmapDrawable getBitmapFromMemCache(String data) {
    	BitmapDrawable memValue = null;
    	
    	if (mMemoryCache != null) {
    		memValue = mMemoryCache.get(data);
    	}
    	
    	if (BuildConfig.DEBUG && memValue != null) {
            Log.d(TAG, "Memory cache hit");
        }
    	
    	return memValue;
    }
    
    public Bitmap getBitmapFromDiskCache(String data) {
    	final String key = hashKeyForDisk(data);
    	Bitmap bitmap = null;
    	
    	synchronized (mDiskCacheLock) {
    		while (mDiskCacheStarting) {
    			try {
    				mDiskCacheLock.wait();
    			} catch (InterruptedException e) {}
    		}
    		if (mDiskLruCache != null) {
    			InputStream inputStream = null;
    			try {
    				final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
    				if (snapshot != null) {
    					if (BuildConfig.DEBUG) {
    						Log.d(TAG, "Disk cache hit");
    					}
    					inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
    					if (inputStream != null) {
    						FileDescriptor fd = ((FileInputStream) inputStream).getFD();
    						
    						// Decode bitmap from descriptor
    					}
    				}
    			} catch (final IOException e) {
    				Log.e(TAG, "getBitmapFromDiskCache - " + e);
    			} finally {
    				try {
    					if (inputStream != null) {
    						inputStream.close();
    					}
    				} catch (IOException e) {}
    			}
    		}
    		return bitmap;
    	}
    }
    
    protected Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
    	Bitmap bitmap = null;
    	
    	if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
    		final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
    		Bitmap item;
    		
    		while (iterator.hasNext()) {
    			item = iterator.next().get();
    			
    			if (item != null && item.isMutable()) {
    				if (canUseForInBitmap(item, options)) {
    					bitmap = item;
    					
    					iterator.remove();
    					break;
    				}
    			} else {
    				iterator.remove();
    			}
    		}
    	}
    	
    	return bitmap;
    }
    
    public void clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Memory cache cleared");
            }
        }

        synchronized (mDiskCacheLock) {
            mDiskCacheStarting = true;
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    mDiskLruCache.delete();
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Disk cache cleared");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "clearCache - " + e);
                }
                mDiskLruCache = null;
                initDiskCache();
            }
        }
    }
    
    public void flush() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    mDiskLruCache.flush();
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Disk cache flushed");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "flush - " + e);
                }
            }
        }
    }
    
    public void close() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    if (!mDiskLruCache.isClosed()) {
                        mDiskLruCache.close();
                        mDiskLruCache = null;
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Disk cache closed");
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close - " + e);
                }
            }
        }
    }
    
    @TargetApi(12)
    public static int getBitmapSize(BitmapDrawable value) {
        Bitmap bitmap = value.getBitmap();

        if (Utils.hasHoneycombMR1()) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
    
    @TargetApi(9)
    public static long getUsableSpace(File path) {
        if (Utils.hasGingerbread()) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }
    
    /** A hashing method that changes a string (like a URL) into a hash suitable for using as a disk filename.*/
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }
    
    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    
    private static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options options) {
    	int width = options.outWidth / options.inSampleSize;
    	int height = options.outHeight / options.inSampleSize;
    	
    	return candidate.getWidth() == width && candidate.getHeight() == height;
    }
    
     /** Get a usable cache directory (external if available, internal otherwise). */
    public static File getDiskCacheDir(Context context, String uniqueName) {
		//If storage built-in or media mounted use external storage, otherwise use internal
		final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || 
				!isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() : context.getCacheDir().getPath();
				
		return new File(cachePath + File.separator + uniqueName);		
	}
	
    @TargetApi(9)
	public static boolean isExternalStorageRemovable() {
		if (Utils.hasGingerbread()) {
			return Environment.isExternalStorageRemovable();
		}
		return true;
	}
	
	@TargetApi(8)
	public static File getExternalCacheDir(Context context) {
		if (Utils.hasFroyo()) {
			return context.getExternalCacheDir();
		}
		
		final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
		return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
	}
	
	private static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
		RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);
		
		if (mRetainFragment == null) {
			mRetainFragment = new RetainFragment();
			fm.beginTransaction().add(mRetainFragment, TAG).commitAllowingStateLoss();
		}
		
		return mRetainFragment;
	}
    
    public static class ImageCacheParams {
    	public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
    	public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
    	public File diskCacheDir;
    	public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
    	public int compressQuality = DEFAULT_COMPRESS_QUALITY;
    	public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
    	public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
    	public boolean initDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;
    	
    	public ImageCacheParams(Context context, String diskCacheDirectoryName) {
    		diskCacheDir = getDiskCacheDir(context, diskCacheDirectoryName);
    	}
    	
    	public void setMemCacheSizePercent(float percent) {
            if (percent < 0.05f || percent > 0.8f) {
                throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                        + "between 0.05 and 0.8 (inclusive)");
            }
            memCacheSize = Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
        }
    	
    }
    
    public static class RetainFragment extends Fragment {
    	private Object mObject;
    	
    	public RetainFragment() {}
    	
    	@Override
    	public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		
    		setRetainInstance(true);
    	}
    	
    	public void setObject(Object object) {
    		mObject = object;
    	}
    	
    	public Object getObject() {
    		return mObject;
    	}
    }
}
