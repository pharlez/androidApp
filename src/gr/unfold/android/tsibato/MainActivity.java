package gr.unfold.android.tsibato;

import java.util.ArrayList;

import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
import gr.unfold.android.tsibato.views.ViewPagerNonSwipeable;
import gr.unfold.android.tsibato.wsclient.GetDealsTask;
import gr.unfold.android.tsibato.wsclient.SearchDealsTask;
import gr.unfold.android.tsibato.search.DealSuggestionsProvider;
import gr.unfold.android.tsibato.util.Utils;

public class MainActivity extends FragmentActivity
		implements OnDealSelectedListener, OnScrollUpOrDownListener, OnDealsChangedListener {
	
	private static final String TAG = MainActivity.class.getName();
	private static final int NUM_ITEMS = 2;
	
	private ProgressDialog progressDialog;
	private MenuItem mSearchMenuItem;
	
	private DealsListFragment mListFragment;
	private DealsMapFragment mMapFragment;
	
	private ListMapPagerAdapter mPagerAdapter;
    private ViewPagerNonSwipeable mPager;
	
	private ArrayList<Deal> mDeals;
	private String mQuery;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		setProgressDialog();
		
		// Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {
        	
        	mPagerAdapter = new ListMapPagerAdapter(getSupportFragmentManager());
    		mPager = (ViewPagerNonSwipeable) findViewById(R.id.fragment_container);
            
    		Button listButton = (Button)findViewById(R.id.button_list);
    		listButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	selectListOrMap(0);
                    mPager.setCurrentItem(0);
                }
            });
            Button mapButton = (Button)findViewById(R.id.button_map);
            mapButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	selectListOrMap(NUM_ITEMS - 1);
                    mPager.setCurrentItem(NUM_ITEMS - 1);
                }
            });
        	
            // However, if we're being restored from a previous state,
            // we should load the data from bundle
            if (savedInstanceState != null) {
            	mDeals = savedInstanceState.getParcelableArrayList("DEALS_PARCEL_ARRAY");
            	mQuery = savedInstanceState.getString("SEARCH_QUERY");
            	
            	onSearchQueryChanged(mQuery);
            	mPager.setAdapter(mPagerAdapter);
            	
            	int activeViewPage = savedInstanceState.getInt("ACTIVE_VIEW_PAGE");
            	mPager.setCurrentItem(activeViewPage);
            	selectListOrMap(activeViewPage);
            	
            	return;
            }
            
            Intent intent = getIntent();
            
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            	String query = intent.getStringExtra(SearchManager.QUERY);
            	int activeView = intent.getIntExtra("ACTIVE_VIEW_PAGE", 0);
            	
            	searchDeals(query, activeView);
            	
            	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
            			DealSuggestionsProvider.AUTHORITY, DealSuggestionsProvider.MODE);
                suggestions.saveRecentQuery(query, null);
                
            } else {
            	getDeals();
            }
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
	
	private void searchDeals(final String query, final int activeView) {
		SearchDealsTask task = new SearchDealsTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {
			
			@Override
			public void onTaskCompleteSuccess(ArrayList<Deal> result) {
				mDeals = result;
				mQuery = query;
				displayDeals(result, activeView);
				onSearchQueryChanged(query);
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
		mPager.setAdapter(mPagerAdapter);
		selectListOrMap(0);
	}
	
	private void displayDeals(ArrayList<Deal> results, int activeViewPage) {
		mPager.setAdapter(mPagerAdapter);
		mPager.setCurrentItem(activeViewPage);
		selectListOrMap(activeViewPage);
	}
	
	public void onDealSelected(Deal deal) {
		Intent intent = new Intent(this, DealActivity.class);
		intent.putExtra("DEAL_PARCEL", deal);
		startActivity(intent);
	}
	
	public void onDealsDataChanged(ArrayList<Deal> deals) {
		mDeals = deals;
	}
	
	public void selectListOrMap(int currentPagerItem) {
		switch (currentPagerItem) {
			case 0:
				findViewById(R.id.button_list).setSelected(true);
				findViewById(R.id.button_map).setSelected(false);
				return;
			case 1:
				findViewById(R.id.button_map).setSelected(true);
				findViewById(R.id.button_list).setSelected(false);
				return;
		}
	}
	
	public void onListMapVisibilityChanged(boolean isListVisible) {
		if (isListVisible) {
			findViewById(R.id.button_list).setSelected(true);
			findViewById(R.id.button_map).setSelected(false);
			//this.isListVisible = true;
		} else {
			findViewById(R.id.button_map).setSelected(true);
			findViewById(R.id.button_list).setSelected(false);
			//this.isListVisible = false;
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onSearchQueryChanged(String query) {
		if (Utils.hasHoneycomb()) {
			ActionBar actionBar = this.getActionBar();
			if (query != null) {
				actionBar.setDisplayShowTitleEnabled(true);
				actionBar.setTitle(query);
			} else {
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setTitle("");
			}
		}
	}
	
	public void onScrollUp() {
		findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
	}
	
	public void onScrollDown() {
		LinearLayout toolbar = (LinearLayout) findViewById(R.id.toolbar);
		toolbar.setVisibility(View.GONE);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putParcelableArrayList("DEALS_PARCEL_ARRAY", mDeals);
	  savedInstanceState.putString("SEARCH_QUERY", mQuery);
	  savedInstanceState.putInt("ACTIVE_VIEW_PAGE", mPager.getCurrentItem());
	  super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void startActivity(Intent intent) {      
	    // check if search intent
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	        intent.putExtra("ACTIVE_VIEW_PAGE", mPager.getCurrentItem());
	    }
	    super.startActivity(intent);
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
	
	public class ListMapPagerAdapter extends FragmentPagerAdapter {
        public ListMapPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
        	switch (position) {
	        	case 0:
	        		if (mListFragment == null) {
	        			mListFragment = DealsListFragment.newInstance(mDeals);
	        		}
	        		return mListFragment;
	        	default:
	        		if (mMapFragment == null) {
	        			mMapFragment = DealsMapFragment.newInstance(mDeals);
	        		}
	        		return mMapFragment;
        	}
        }
    }

}
