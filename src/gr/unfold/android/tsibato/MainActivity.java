package gr.unfold.android.tsibato;

import java.util.ArrayList;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;

import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.Toast;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import gr.unfold.android.tsibato.async.AsyncTaskListener;
import gr.unfold.android.tsibato.async.IProgressTracker;
import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.listeners.OnDealSelectedListener;
import gr.unfold.android.tsibato.listeners.OnDealsChangedListener;
import gr.unfold.android.tsibato.listeners.OnScrollUpOrDownListener;
import gr.unfold.android.tsibato.wsclient.GetDealsTask;
import gr.unfold.android.tsibato.wsclient.SearchDealsTask;
import gr.unfold.android.tsibato.search.DealSuggestionsProvider;
import gr.unfold.android.tsibato.util.Utils;

public class MainActivity extends FragmentActivity
		implements OnDealSelectedListener, OnScrollUpOrDownListener, OnDealsChangedListener {
	
	private static final String TAG = MainActivity.class.getName();
	
	private ProgressDialog progressDialog;
	private MenuItem mSearchMenuItem;
	
	private ArrayList<Deal> mDeals;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) {
            //Utils.enableStrictMode();
        }
		Log.d(TAG, "On Create");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		setProgressDialog();
		
		//onSearchRequested();
		
		// Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
            	return;
            }
            
            getDeals();
            
        }
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        	String query = intent.getStringExtra(SearchManager.QUERY);
        	searchDeals(query);
        	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
        			DealSuggestionsProvider.AUTHORITY, DealSuggestionsProvider.MODE);
            suggestions.saveRecentQuery(query, null);
        } else {
        	
        }
	}
	
	private void getDeals() {
		GetDealsTask task = new GetDealsTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {

			@Override
            public void onTaskCompleteSuccess(ArrayList<Deal> result) {
				mDeals = result;
                displayDeals(result);
            }

            @Override
            public void onTaskFailed(Exception cause) {
                Log.e(TAG, cause.getMessage(), cause);
                showToastMessage(R.string.failed_msg);
            }
		});
		
		task.setProgressTracker(new IProgressTracker() {
			
			@Override
		    public void onStartProgress() {
		        progressDialog.show();
		    }

		    @Override
		    public void onStopProgress() {
		        progressDialog.dismiss();
		    }
		});
		
		task.execute(GetDealsTask.createRequest());
	}
	
	private void searchDeals(final String query) {
		SearchDealsTask task = new SearchDealsTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {
			
			@Override
			public void onTaskCompleteSuccess(ArrayList<Deal> result) {
				mDeals = result;
				displaySearchDeals(query, result);
			}
			
			@Override
			public void onTaskFailed(Exception cause) {
				Log.e(TAG, cause.getMessage(), cause);
				showToastMessage(R.string.failed_msg);
			}
		});
		
		task.setProgressTracker(new IProgressTracker() {
			
			@Override
			public void onStartProgress() {
				progressDialog.show();
			}
			
			@Override
			public void onStopProgress() {
				progressDialog.dismiss();
			}
		});
		
		task.execute(SearchDealsTask.createRequest(query));
	}
	
	private void displayDeals(ArrayList<Deal> results) {
		// Create an instance of DealsListFragment
        DealsListFragment listFragment = new DealsListFragment();
        
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", results);
        
        listFragment.setArguments(bundle);
        
        // Add the fragment to the 'fragment_container' FrameLayout
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, listFragment);
        transaction.commit();
	}
	
	private void displaySearchDeals(String query, ArrayList<Deal> results) {
		// Create an instance of DealsListFragment
        DealsListFragment listFragment = new DealsListFragment();
        
        Bundle bundle = new Bundle();
        bundle.putString("SEARCH_QUERY", query);
        bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", results);
        
        listFragment.setArguments(bundle);
        
        // Add the fragment to the 'fragment_container' FrameLayout
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, listFragment);
        transaction.addToBackStack(null);
        transaction.commit();
	}
	
	public void onListSelected(View view) {
		
	}
	
	public void onMapSelected(View view) {		
		DealsMapFragment mapFragment = DealsMapFragment.newInstance(mDeals);
		
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_container, mapFragment);
		transaction.addToBackStack(null);
		transaction.commit();
	}
	
	public void onDealSelected(Deal deal) {
		Intent intent = new Intent(this, DealActivity.class);
		intent.putExtra("DEAL_PARCEL", deal);
		startActivity(intent);
	}
	
	public void onDealsDataChanged(ArrayList<Deal> deals) {
		mDeals = deals;
	}
	
	public void onListMapVisibilityChanged(boolean isListVisible) {
		if (isListVisible) {
			findViewById(R.id.button_list).setSelected(true);
			findViewById(R.id.button_map).setSelected(false);
		} else {
			findViewById(R.id.button_map).setSelected(true);
			findViewById(R.id.button_list).setSelected(false);
		}
	}
	
	public void onScrollUp() {
		findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
	}
	
	public void onScrollDown() {
		LinearLayout toolbar = (LinearLayout) findViewById(R.id.toolbar);
		/*Animation slideoutAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out);
		toolbar.setAnimation(slideoutAnimation);*/
		toolbar.setVisibility(View.GONE);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putParcelableArrayList("DEALS_PARCEL_ARRAY", mDeals);
	  
	  super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	  mDeals = savedInstanceState.getParcelableArrayList("DEALS_PARCEL_ARRAY");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "On Resume");
        if (progressDialog == null) {
            setProgressDialog();
        }
    }

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		mSearchMenuItem = menu.findItem(R.id.search);
		
		if (Utils.hasHoneycomb()) {
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setOnQueryTextListener(new OnQueryTextListener() {
				@Override
		        public boolean onQueryTextSubmit(String query) {
		            //Do nothing, results for the string supplied are already shown.
		            //Just collapse search widget
		        	closeSearch();
		            return false;
		        }
		        @Override
		        public boolean onQueryTextChange(String newText) {
		             return false;
		        }
		    });
			searchView.setOnSuggestionListener(new OnSuggestionListener() {
				@Override
				public boolean onSuggestionClick(int position) {
					closeSearch();
					return false;
				}
				@Override
				public boolean onSuggestionSelect(int position) {
					return false;
				}
			});
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.search:
	        	if (!Utils.hasHoneycomb()) {
	        		onSearchRequested();
	        	}
	            return true;
	        case R.id.clear_history:
	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	            builder.setMessage(getString(R.string.clear_history_dialog))
	                   .setCancelable(false)
	                   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                       public void onClick(DialogInterface dialog, int id) {
	                    	   clearSearchSuggestions();
	                    	   dialog.dismiss();
	                       }
	                   })
	                   .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	                       public void onClick(DialogInterface dialog, int id) {
	                            dialog.cancel();
	                       }
	                   });
	            AlertDialog alert = builder.create();
	            alert.show();
	        	return true;
	        default:
	            return false;
	    }
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	 public boolean onSearchRequested() {
		if (Utils.hasHoneycomb()) {
			if (Utils.hasIceCreamSandwich()) {
				mSearchMenuItem.expandActionView();
			} else {
				mSearchMenuItem.getActionView().requestFocus();
			}
			return false;  // don't go ahead and show the search box
		} else {
			super.onSearchRequested();
			return true;
		}
	 }
	
	private void setProgressDialog() {
    	this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setCancelable(false);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setMessage(getString(R.string.loading_msg));
    }
	
	private void showToastMessage(int messageId) {
		Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void closeSearch() {
		if (Utils.hasIceCreamSandwich()) {
    		mSearchMenuItem.collapseActionView();
    	} else {
    		mSearchMenuItem.getActionView().clearFocus();
    	}
	}
	
	private void clearSearchSuggestions() {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
				DealSuggestionsProvider.AUTHORITY, DealSuggestionsProvider.MODE);
		suggestions.clearHistory();
	}

}
