package gr.unfold.android.tsibato;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
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
import gr.unfold.android.tsibato.listeners.OnDealSelectedListener;
import gr.unfold.android.tsibato.listeners.OnDealsChangedListener;
import gr.unfold.android.tsibato.listeners.OnScrollUpOrDownListener;
import gr.unfold.android.tsibato.util.Utils;
import gr.unfold.android.tsibato.adapter.DealsAdapter;
import gr.unfold.android.tsibato.data.Deal;

public class DealsListFragment extends ListFragment {
	private static final String TAG = "DealsListFragment";
    private static final String IMAGE_CACHE_DIR = "images";
    
	private int mImageHeight;
	private int mImageWidth;
	
	private ArrayList<Deal> mDeals;
	
	private ImageFetcher mImageFetcher;
	public DealsAdapter mAdapter;
	
	private int mPosition = 0;
	private int mOffset = 0;
	
	protected OnDealSelectedListener mCallback;
	protected OnScrollUpOrDownListener mScrollUpDown;
	protected OnDealsChangedListener mUpdater;
	
	public static DealsListFragment newInstance(ArrayList<Deal> deals) {
		DealsListFragment listFragment = new DealsListFragment();

		Bundle bundle = new Bundle();
		bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", deals);
		listFragment.setArguments(bundle);

        return listFragment;
    }
	
	public DealsListFragment() { }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mImageHeight = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_height);
		mImageWidth = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_width);
		
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			mDeals = bundle.getParcelableArrayList("DEALS_PARCEL_ARRAY");
		}

		ImageCacheParams cacheParams = new ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory
        
        mImageFetcher = new ImageFetcher(getActivity(), mImageWidth, mImageHeight);
        mImageFetcher.setLoadingImage(R.drawable.image_loading);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
		
        if (mDeals != null) {
        	mAdapter = new DealsAdapter(getActivity(), mDeals, mImageFetcher);
        }
        
        if (savedInstanceState != null) {
        	mPosition = savedInstanceState.getInt("LIST_POSITION");
        	mOffset = savedInstanceState.getInt("LIST_OFFSET");
        }
        
        Log.d(TAG, "On Create, ListFragment: " + this.toString());
		Log.d(TAG, "On Create, Adapter: " + mAdapter.toString());
		
		//setListAdapter(mAdapter);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
	    super.onActivityCreated(savedInstanceState);
	    
	    setListAdapter(mAdapter);
	    
	    //getListView().setEmptyView(emptyView(R.layout.list_empty));
	    
	    setEmptyText(getString(R.string.list_empty_text));
	    
	    getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    mImageFetcher.setPauseWork(true);
                } else {
                    mImageFetcher.setPauseWork(false);
                }
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (view.getChildCount() == 0) {
					return;
				}
				if (view.getLastVisiblePosition() == view.getAdapter().getCount() - 1 &&
						view.getChildAt(view.getChildCount() - 1).getBottom() <= view.getHeight()) {
					// List has scrolled all the way to the bottom, so show toolbar
					mScrollUpDown.onScrollUp();
					mUpdater.onUpdateDeals();
				} else {
					int position = view.getFirstVisiblePosition();
			        View v = view.getChildAt(0);
			        int offset = (v == null) ? 0 : v.getTop();
			        int smoothing = getResources().getDimensionPixelSize(R.dimen.scroll_animation_smoothing);
			        
			        if (mPosition < position) {
			        	mScrollUpDown.onScrollDown();
			        } else if (mPosition > position){
			        	mScrollUpDown.onScrollUp();
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
			}
		});
	}
	
	 @Override
	 public void onListItemClick(ListView l, View v, int position, long id) {
		 // Notify the parent activity of selected item
	     mCallback.onDealSelected(mAdapter.getItem(position));
	        
	     // Set the item as checked to be highlighted when in two-pane layout
	     getListView().setItemChecked(position, true);
	}
	
	@Override
	public void onResume() {
		super.onResume();

		mUpdater.onDealsDataChanged(mDeals);
		
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
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    // Save UI state changes to the savedInstanceState.
	    // This bundle will be passed to onCreate if the process is
	    // killed and restarted.
		savedInstanceState.putInt("LIST_POSITION", mPosition);
		savedInstanceState.putInt("LIST_OFFSET", mOffset);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnDealSelectedListener) activity;
            mScrollUpDown = (OnScrollUpOrDownListener) activity;
            mUpdater = (OnDealsChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDealSelectedListener, OnScrollUpOrDownListener and OnDealsDataChangedListener");
        }
	}
	
	public void updateDeals(ArrayList<Deal> deals) {
		mDeals = deals;
		Log.d(TAG, "On Update, ListFragment: " + this.toString());
		Log.d(TAG, "On Update, Adapter: " + mAdapter.toString());
		mAdapter.notifyDataSetChanged();
	}
	
}
