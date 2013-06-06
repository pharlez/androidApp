package gr.unfold.android.tsibato;

import java.util.ArrayList;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import gr.unfold.android.tsibato.async.AsyncTaskListener;
import gr.unfold.android.tsibato.async.IProgressTracker;
import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.wsclient.GetDealsTask;
import gr.unfold.android.tsibato.wsclient.SearchDealsTask;
import gr.unfold.android.tsibato.util.Utils;

public class MainActivity extends FragmentActivity
		implements OnDealSelectedListener, OnScrollUpOrDownListener {
	
	private static final String TAG = MainActivity.class.getName();
	
	private ProgressDialog progressDialog;
	private Menu mMenuActionBar;
	private MenuItem mSearchMenuItem;
	
	private ArrayList<Deal> mDeals;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) {
            //Utils.enableStrictMode();
        }
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

            findViewById(R.id.button_list).setSelected(true);
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
	
	private void searchDeals(String query) {
		SearchDealsTask task = new SearchDealsTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {
			
			@Override
			public void onTaskCompleteSuccess(ArrayList<Deal> result) {
				mDeals = result;
				displaySearchDeals(result);
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
	
	private void displaySearchDeals(ArrayList<Deal> results) {
		// Create an instance of DealsListFragment
        DealsListFragment listFragment = new DealsListFragment();
        
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", results);
        
        listFragment.setArguments(bundle);
        
        // Add the fragment to the 'fragment_container' FrameLayout
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, listFragment);
        transaction.addToBackStack(null);
        transaction.commit();
	}
	
	public void onDealSelected(int position) {
		Intent intent = new Intent(this, DealActivity.class);
		intent.putExtra("DEAL_PARCEL", mDeals.get(position));
		startActivity(intent);
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
			//SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
			SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setOnQueryTextListener(new OnQueryTextListener() {

		        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				@Override
		        public boolean onQueryTextSubmit(String query) {
		            //Do nothing, results for the string supplied are already shown.
		            //Just collapse search widget
		        	if (Utils.hasIceCreamSandwich()) {
		        		mSearchMenuItem.collapseActionView();
		        	}
		            return false;
		        }

		        @Override
		        public boolean onQueryTextChange(String newText) {
		            //do other stuff
		             return false;
		        }
		    });
		}
		
		//mMenuActionBar = menu;
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

}
