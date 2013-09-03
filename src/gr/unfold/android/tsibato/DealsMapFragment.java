package gr.unfold.android.tsibato;

import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.listeners.OnDealSelectedListener;
import gr.unfold.android.tsibato.listeners.OnDealsChangedListener;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
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
	
	private double mSelectedCityLong;
	private double mSelectedCityLat;
	private double mSelectedCityMapZoom;
	
	private LocationManager mLocationManager;
	private LocationListener mLocationListener;
	
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
		
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			mDeals = bundle.getParcelableArrayList("DEALS_PARCEL_ARRAY");
			mSelectedCityLong = bundle.getDouble("SELECTED_CITY_LONG");
			mSelectedCityLat = bundle.getDouble("SELECTED_CITY_LAT");
			mSelectedCityMapZoom = bundle.getDouble("SELECTED_CITY_MAPZOOM");
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
        	mMap.setMyLocationEnabled(false);
        	
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
			else {
				centerMap();
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
	
	private void updateMap(double mapLong, double mapLat, double mapZoom) {
		// Do a null check to confirm that we have not already instantiated the map.
		mMap = this.getMap();
		mSelectedCityLong = mapLong;
		mSelectedCityLat = mapLat;
		mSelectedCityMapZoom = mapZoom;
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
			
			centerMap();
		}
	}
	
	public void updateDeals(ArrayList<Deal> deals) {
		mDeals = deals;
		updateMap();
	}
	
	public void updateDeals(ArrayList<Deal> deals, double mapLong, double mapLat, double mapZoom) {
		mDeals = deals;
		updateMap(mapLong, mapLat, mapZoom);
		//reCenterMap(mapLong, mapLat, mapZoom);
	}
	
	protected void centerMap() {
		mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		
		mLocationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		    	// Called when a new location is found by the network location provider.
		    	LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinate, (float) 12));
				if (mLocationManager != null) {
					mLocationManager.removeUpdates(mLocationListener);
				}
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		};
		
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		
		final Handler handler = new Handler();
	    handler.postDelayed(new Runnable() {
	      @Override
	      public void run() {
	    	  if (mLocationManager != null) {
	    		  mLocationManager.removeUpdates(mLocationListener);
	    	  }
	      }
	    }, 3000);
		
		Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
		if (location != null) {
			LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinate, (float) 12));
		} else {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mSelectedCityLat, mSelectedCityLong), (float) mSelectedCityMapZoom));			
		}
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
		if (mMap != null) {	
			savedInstanceState.setClassLoader(mMap.getCameraPosition().getClass().getClassLoader());
			savedInstanceState.putParcelable("MAP_CAMERA_POSITION", mMap.getCameraPosition());
		}
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
	
	private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getActivity());
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
        	if (AppConfig.DEBUG) {
        		Log.d("Location Updates", "Google Play services is available.");
        	}
            return true;
        // Google Play services was not available for some reason
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this.getActivity(), 0);
            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                errorDialog.show();
            }
            return false;
        }
    }
	
}
