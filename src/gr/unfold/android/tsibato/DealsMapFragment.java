package gr.unfold.android.tsibato;

import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.listeners.OnDealSelectedListener;
import gr.unfold.android.tsibato.listeners.OnDealsChangedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class DealsMapFragment extends SupportMapFragment
		implements OnInfoWindowClickListener {
	
	private static final String TAG = "DealsMapFragment";
	
	private double minLat = Integer.MAX_VALUE;
    private double maxLat = Integer.MIN_VALUE;
    private double minLon = Integer.MAX_VALUE;
    private double maxLon = Integer.MIN_VALUE;
	
    protected OnDealSelectedListener mCallback;
    protected OnDealsChangedListener mUpdater;
    
	private GoogleMap mMap;
	private ArrayList<Marker> mMarkers;
	
	private ArrayList<Deal> mDeals;
	
	public DealsMapFragment() {	}
	
	public static DealsMapFragment newInstance(ArrayList<Deal> deals) {
		DealsMapFragment mapFragment = new DealsMapFragment();
		
		Bundle bundle = new Bundle();
		bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", deals);
		
		mapFragment.setArguments(bundle);
		
		return mapFragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mDeals = new ArrayList<Deal>();
		
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			mDeals = bundle.getParcelableArrayList("DEALS_PARCEL_ARRAY");
		}
		
	}
	
	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
	    View v = super.onCreateView(arg0, arg1, arg2);
	    setUpMapIfNeeded();
	    return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mUpdater.onListMapVisibilityChanged(false);
		if (mMap != null) {
			LatLngBounds.Builder builder = new LatLngBounds.Builder();
			for (Marker m : mMarkers) {
			    builder.include(m.getPosition());
			}
			LatLngBounds bounds = builder.build();
			int padding = 0; // offset from edges of the map in pixels
			mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50, 50, padding));
//	    	
//	    	LatLngBounds latLonBound = new LatLngBounds(new LatLng(minLat, minLon), new LatLng(maxLat, maxLon));
//	    	mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLonBound, (int)Math.abs(maxLat-minLat), (int)Math.abs(maxLon-minLon), 10));
//			//mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng((maxLat + minLat)/2, (maxLon + minLon)/2 )));
		}
	}

	
	private void setUpMapIfNeeded() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (mMap == null) {
	        mMap = this.getMap();
	        // Check if we were successful in obtaining the map.
	        if (mMap != null) {
	            // The Map is verified. It is now safe to manipulate the map.
	        	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	        	mMap.getUiSettings().setZoomControlsEnabled(false);
	        	
	        	mMarkers = new ArrayList<Marker>();
	        	LatLngBounds.Builder builder = new LatLngBounds.Builder();
	        	
				for (Deal deal : mDeals) {
		    		LatLng location = new LatLng(deal.lat, deal.lon);
		    		
		    		Marker marker = mMap.addMarker(new MarkerOptions().position(location)
		    						.title(deal.getSmallTitle(30))
		    						.snippet(deal.getSmallTitle(50)));

		    		builder.include(marker.getPosition());
		    		mMarkers.add(marker);
		    	}
				
				mMap.setOnInfoWindowClickListener(this);
				
				LatLngBounds bounds = builder.build();
				mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10, 10, 0));
	        }
	    }
	}
	
	private void handleMapBounds(LatLng location) {
		maxLat = Math.max(location.latitude, maxLat);
        minLat = Math.min(location.latitude, minLat);
        maxLon = Math.max(location.longitude, maxLon);
        minLon = Math.min(location.longitude, minLon);
	}
	
	public void onInfoWindowClick(Marker marker) {
//		if (mMarkersMapping.containsKey(marker)) {
//			mCallback.onDealSelected(mMarkersMapping.get(marker));
//		}
		Deal deal = mDeals.get(mMarkers.indexOf(marker));
		mCallback.onDealSelected(deal);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnDealSelectedListener) activity;
            mUpdater = (OnDealsChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDealSelectedListener and OnDealsChangedListener");
        }
	}
	
}
