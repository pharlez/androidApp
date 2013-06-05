package gr.unfold.android.tsibato;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import gr.unfold.android.tsibato.images.ImageFetcher;
import gr.unfold.android.tsibato.images.ImageCache.ImageCacheParams;
import gr.unfold.android.tsibato.adapter.DealsAdapter;
import gr.unfold.android.tsibato.data.Deal;

public class DealsListFragment extends ListFragment {
	private static final String TAG = "DealsListFragment";
    private static final String IMAGE_CACHE_DIR = "images";
    
	private int mImageHeight;
	private int mImageWidth;
	
	private ImageFetcher mImageFetcher;
	private DealsAdapter mAdapter;
	
	private int mPosition = 0;
	private int mOffset = 0;
	
	protected OnDealSelectedListener mCallback;
	protected OnScrollUpOrDownListener mScrollUpDown;
	
	public DealsListFragment() { }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mImageHeight = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_height);
		mImageWidth = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_width);
		
		ArrayList<Deal> deals = new ArrayList<Deal>();
		
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			deals = bundle.getParcelableArrayList("DEALS_PARCEL_ARRAY");
		}
		
		ImageCacheParams cacheParams = new ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory
        
        mImageFetcher = new ImageFetcher(getActivity(), mImageWidth, mImageHeight);
        mImageFetcher.setLoadingImage(R.drawable.ic_launcher);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
		
        mAdapter = new DealsAdapter(getActivity(), deals, mImageFetcher);
		
		//setListAdapter(mAdapter);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
	    super.onActivityCreated(savedInstanceState);
	    
	    setListAdapter(mAdapter);
	    
	    getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    mImageFetcher.setPauseWork(true);
                } else {
                    mImageFetcher.setPauseWork(false);
                }
                /*View child = view.getChildAt(0);
				int topOffset = child.getTop();
				if (topOffset < mPreviousTopOffset) {
					mScrollUpDown.onScrollDown();
					Log.i(TAG, "down");
				} else if (topOffset > mPreviousTopOffset) {
					mScrollUpDown.onScrollUp();
					Log.i(TAG, "up");
				}
				mPreviousTopOffset = topOffset;*/
                
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				
				int position = view.getFirstVisiblePosition();
		        View v = view.getChildAt(0);
		        int offset = (v == null) ? 0 : v.getTop();
		        int smoothing = getResources().getDimensionPixelSize(R.dimen.scroll_animation_smoothing);
		        //Log.i(TAG,  "position: " + position + ", offset: " + offset + ", mPosition: " + mPosition + ", mOffset: " + mOffset);
		        if (mPosition < position) {
		        	mScrollUpDown.onScrollDown();
					//Log.i(TAG, "down");
		        } else if (mPosition > position){
		        	mScrollUpDown.onScrollUp();
					//Log.i(TAG, "up");
		        } else {
		        	if (mOffset < offset - smoothing) {
		        		mScrollUpDown.onScrollUp();
		        	} else if (mOffset - smoothing > offset) {
		        		mScrollUpDown.onScrollDown();
		        	}
		        }
		        mPosition = position;
		        mOffset = offset;
			}
		});
	}
	
	 @Override
	 public void onListItemClick(ListView l, View v, int position, long id) {
		 // Notify the parent activity of selected item
	     mCallback.onDealSelected(position);
	        
	     // Set the item as checked to be highlighted when in two-pane layout
	     getListView().setItemChecked(position, true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mImageFetcher.setExitTasksEarly(false);
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.setPauseWork(false);
		mImageFetcher.setExitTasksEarly(true);
		mImageFetcher.flushCache();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
	}	
	
	@Override
	public void onStart() {
		super.onStart();
		
		// logic for onStart here
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnDealSelectedListener) activity;
            mScrollUpDown = (OnScrollUpOrDownListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDealSelectedListener and OnScrollUpOrDownListener");
        }
	}
	
}
