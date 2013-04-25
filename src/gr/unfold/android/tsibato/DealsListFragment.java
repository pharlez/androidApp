package gr.unfold.android.tsibato;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import gr.unfold.android.tsibato.images.ImageFetcher;
import gr.unfold.android.tsibato.images.ImageCache.ImageCacheParams;
import gr.unfold.android.tsibato.adapter.DealsAdapter;
import gr.unfold.android.tsibato.data.Deal;

public class DealsListFragment extends ListFragment {
	private static final String TAG = "ImageGridFragment";
    private static final String IMAGE_CACHE_DIR = "images";
    
	private int mImageHeight;
	private int mImageWidth;
	
	private ImageFetcher mImageFetcher;
	private DealsAdapter mAdapter;
	
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
		// have to get data into an ArrayList<Deal> to pass to the list adapter
		setListAdapter(mAdapter);
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
		
		// logic for onAttach here
	}
	
}
