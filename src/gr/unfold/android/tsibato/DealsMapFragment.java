package gr.unfold.android.tsibato;

import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.listeners.OnDealSelectedListener;
import gr.unfold.android.tsibato.listeners.OnDealsChangedListener;
import gr.unfold.android.tsibato.listeners.OnScrollUpOrDownListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPositionCreator;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class DealsMapFragment extends SupportMapFragment
		implements OnInfoWindowClickListener {
	
	private static final String TAG = "DealsMapFragment";
	
    protected OnDealSelectedListener mCallback;
    protected OnDealsChangedListener mUpdater;
    
	private GoogleMap mMap;
	private ArrayList<Marker> mMarkers;
	
	private ArrayList<Deal> mDeals;
	private String mQuery;
	
	private double mSelectedCityLong;
	private double mSelectedCityLat;
	private double mSelectedCityMapZoom;
	
	public DealsMapFragment() {	}
	
	public static DealsMapFragment newInstance(ArrayList<Deal> deals, double mapLong, double mapLat, double mapZoom) {
		DealsMapFragment mapFragment = new DealsMapFragment();
		
		Bundle bundle = new Bundle();
		bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", deals);
		bundle.putDouble("SELECTED_CITY_LONG", mapLong);
		bundle.putDouble("SELECTED_CITY_LAT", mapLat);
		bundle.putDouble("SELECTED_CITY_MAPZOOM", mapZoom);
		
		mapFragment.setArguments(bundle);
		
		return mapFragment;
	}
	
	public static DealsMapFragment newInstance(ArrayList<Deal> deals, String query) {
		DealsMapFragment mapFragment = new DealsMapFragment();
		
		Bundle bundle = new Bundle();
		bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", deals);
		bundle.putString("SEARCH_QUERY", query);
		
		mapFragment.setArguments(bundle);
		
		return mapFragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			mDeals = bundle.getParcelableArrayList("DEALS_PARCEL_ARRAY");
			mSelectedCityLong = bundle.getDouble("SELECTED_CITY_LONG");
			mSelectedCityLat = bundle.getDouble("SELECTED_CITY_LAT");
			mSelectedCityMapZoom = bundle.getDouble("SELECTED_CITY_MAPZOOM");
			String query = bundle.getString("SEARCH_QUERY");
			if (query != null) {
				mQuery = query;
			}
		}
		
	}
	
	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
	    View v = super.onCreateView(arg0, arg1, arg2);
	    
	    FrameLayout frameLayout = new FrameLayout(getActivity());
	    frameLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
	    ((ViewGroup) v).addView(frameLayout, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	    
	    //setUpMapIfNeeded();
	    
	    return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		CameraPosition camPosition = null;
		if (savedInstanceState != null) {
			savedInstanceState.setClassLoader(CameraPositionCreator.class.getClassLoader());
			camPosition = (CameraPosition) savedInstanceState.getParcelable("MAP_CAMERA_POSITION");
		} 
		setUpMapIfNeeded(camPosition);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		mUpdater.onDealsDataChanged(mDeals);
		
	}
	
	private void setUpMapIfNeeded(CameraPosition position) {
	    // Do a null check to confirm that we have not already instantiated the map.
		mMap = this.getMap();
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            // The Map is verified. It is now safe to manipulate the map.
        	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        	mMap.getUiSettings().setZoomControlsEnabled(false);
        	
        	mMarkers = new ArrayList<Marker>();
        	LatLngBounds.Builder builder = new LatLngBounds.Builder();
        	
        	mMap.clear();
        	
			for (Deal deal : mDeals) {
	    		LatLng location = new LatLng(deal.lat, deal.lon);
	    		
	    		Marker marker = mMap.addMarker(new MarkerOptions().position(location)
	    						.title(deal.getSmallTitle(30))
	    						.snippet(deal.getSmallTitle(50)));
	    		
	    		builder.include(marker.getPosition());
	    		mMarkers.add(marker);
	    	}
			
			mMap.setOnInfoWindowClickListener(this);
			
			if (position != null) {
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
			}
//			else if (mMarkers.size() > 0) {
//				LatLngBounds bounds = builder.build();
//				mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 500, 500, 0));
//			}
			else {
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mSelectedCityLat, mSelectedCityLong), (float) mSelectedCityMapZoom));
			}
        }
	}
	
	private void updateMap() {
		// Do a null check to confirm that we have not already instantiated the map.
		mMap = this.getMap();
		// Check if we were successful in obtaining the map.
		if (mMap != null) {
			mMap.clear();
			mMarkers.clear();
			for (Deal deal : mDeals) {
	    		LatLng location = new LatLng(deal.lat, deal.lon);
	    		Marker marker = mMap.addMarker(new MarkerOptions().position(location)
	    						.title(deal.getSmallTitle(30))
	    						.snippet(deal.getSmallTitle(50)));
	    		
	    		mMarkers.add(marker);
	    	}
		}
	}
	
	public void updateDeals(ArrayList<Deal> deals) {
		mDeals = deals;
		updateMap();
	}
	
	public void reCenterMap(double mapLong, double mapLat, double mapZoom) {
		mSelectedCityLong = mapLong;
		mSelectedCityLat = mapLat;
		mSelectedCityMapZoom = mapZoom;
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mSelectedCityLat, mSelectedCityLong), (float) mSelectedCityMapZoom));
	}
	
	public void onInfoWindowClick(Marker marker) {
		Deal deal = mDeals.get(mMarkers.indexOf(marker));
		mCallback.onDealSelected(deal);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.setClassLoader(mMap.getCameraPosition().getClass().getClassLoader());
	  savedInstanceState.putParcelable("MAP_CAMERA_POSITION", mMap.getCameraPosition());
	  super.onSaveInstanceState(savedInstanceState);
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
