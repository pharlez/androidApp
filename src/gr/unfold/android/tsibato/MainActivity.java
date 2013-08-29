package gr.unfold.android.tsibato;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.Toast;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
	
	private int mSelectedCity;
	private ArrayList<Integer> mSelectedCategories;
	private double mSelectedCityLong;
	private double mSelectedCityLat;
	private double mSelectedCityMapZoom;
	
	private boolean isUpdating;
	private boolean noMoreDeals;
	
	public int mDealsPage;
	
	protected Dialog mSplashDialog;

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
	            mDealsPage = savedInstanceState.getInt("DEALS_PAGE_LOADED");
	            mSelectedCity = savedInstanceState.getInt("DEALS_SELECTED_CITY");
	            mSelectedCityLong = savedInstanceState.getDouble("DEALS_SELECTED_CITY_LONG");
	            mSelectedCityLat = savedInstanceState.getDouble("DEALS_SELECTED_CITY_LAT");
	            mSelectedCityMapZoom = savedInstanceState.getDouble("DEALS_SELECTED_CITY_MAPZOOM");
	            mSelectedCategories = savedInstanceState.getIntegerArrayList("DEALS_SELECTED_CATEGORIES");
	            	
	            if (mDeals != null) {
	            	onSearchQueryChanged(mQuery);
	            	mPager.setAdapter(mPagerAdapter);
	            	
	            	int activeViewPage = savedInstanceState.getInt("ACTIVE_VIEW_PAGE");
	            	mPager.setCurrentItem(activeViewPage);
	            	selectListOrMap(activeViewPage);
	            	
	            	return;
            	}
            }
            
            getSettings();
            
            Intent intent = getIntent();
            
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            	String query = intent.getStringExtra(SearchManager.QUERY);
            	int activeView = intent.getIntExtra("ACTIVE_VIEW_PAGE", 0);
            	
				mQuery = query;
            	
            	searchDeals(query, activeView);
            	
            	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
            			DealSuggestionsProvider.AUTHORITY, DealSuggestionsProvider.MODE);
                suggestions.saveRecentQuery(query, null);
                
                setHomeOnSearch();
                
            } else {
            	getDeals();
            }
        }
	}
	
	private void getSettings() {
		SharedPreferences prefs = getSharedPreferences("gr.unfold.android.tsibato.settings", MODE_PRIVATE);
        
        mSelectedCity = prefs.getInt("SELECTED_CITY_ID", 0);
        mSelectedCityLong = (double) prefs.getFloat("SELECTED_CITY_LONG", 0.0f);
        mSelectedCityLat = (double) prefs.getFloat("SELECTED_CITY_LAT", 0.0f);
        mSelectedCityMapZoom = (double) prefs.getFloat("SELECTED_CITY_MAPZOOM", -1.0f);
        
        String selectedCategories = prefs.getString("SELECTED_CATEGORIES_IDS", "");
        StringTokenizer strToken = new StringTokenizer(selectedCategories, ",");
        mSelectedCategories = new ArrayList<Integer>();
        while (strToken.hasMoreTokens()) {
        	int categoryId = Integer.parseInt(strToken.nextToken());
        	mSelectedCategories.add(categoryId);
        }
        Log.d(TAG, "Selected categories: " + mSelectedCategories.toString());
	}
	
	private void getDeals() {
		GetDealsTask task = new GetDealsTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {

			@Override
            public void onTaskCompleteSuccess(ArrayList<Deal> result) {
				mDeals = result;
				mDealsPage = 1;
                displayDeals(result);
                if (result.size() == 0) {
					noMoreDeals = true;
				}
                unlockScreenOrientation();
            }

            @Override
            public void onTaskFailed(Exception cause) {
            	displayEmpty();
                Log.e(TAG, cause.getMessage(), cause);
                showToastMessage(R.string.failed_msg);
                unlockScreenOrientation();
            }
		});
		
		task.setProgressTracker(new IProgressTracker() {
			
			@Override
		    public void onStartProgress() {
				if (progressDialog != null) {
					progressDialog.show();
				}
				showSplashScreen();
		    }

		    @Override
		    public void onStopProgress() {
		    	if (progressDialog != null) {
		    		progressDialog.dismiss();
		    	}
		    	removeSplashScreen();
		    }
		});	
		
		task.execute(GetDealsTask.createRequest(1, mSelectedCity, mSelectedCategories));
		
		lockScreenOrientation();
	}
	
	protected void showSplashScreen() {
	    mSplashDialog = new Dialog(this, R.style.SplashScreen);
	    mSplashDialog.setContentView(R.layout.splash);
	    mSplashDialog.setCancelable(false);
	    mSplashDialog.show();
	     
	    // Set Runnable to remove splash screen just in case
	    final Handler handler = new Handler();
	    handler.postDelayed(new Runnable() {
	      @Override
	      public void run() {
	        removeSplashScreen();
	      }
	    }, 3000);
	}
	
	protected void removeSplashScreen() {
	    if (mSplashDialog != null) {
	        mSplashDialog.dismiss();
	        mSplashDialog = null;
	    }
	}
	
	private void searchDeals(final String query, final int activeView) {
		SearchDealsTask task = new SearchDealsTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {
			
			@Override
			public void onTaskCompleteSuccess(ArrayList<Deal> result) {
				mDeals = result;
				mDealsPage = 1;
				displayDeals(result, activeView);
				onSearchQueryChanged(query);
				if (result.size() == 0) {
					noMoreDeals = true;
				}
				unlockScreenOrientation();
			}
			
			@Override
			public void onTaskFailed(Exception cause) {
				displayEmpty();
				Log.e(TAG, cause.getMessage(), cause);
				showToastMessage(R.string.failed_msg);
				unlockScreenOrientation();
			}
		});
		
		task.setProgressTracker(new IProgressTracker() {
			
			@Override
			public void onStartProgress() {
				if (progressDialog != null) {
					progressDialog.show();
				}
			}
			
			@Override
			public void onStopProgress() {
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
			}
		});
		
		task.execute(SearchDealsTask.createRequest(query, 1));
		
		lockScreenOrientation();
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
	
	private void displayEmpty() {
		findViewById(R.id.toolbar).setVisibility(View.GONE);
		findViewById(R.id.empty).setVisibility(View.VISIBLE);
	}
	
	public void onDealSelected(Deal deal) {
		Intent intent = new Intent(this, DealActivity.class);
		intent.putExtra("DEAL_PARCEL", deal);
		startActivity(intent);
	}
	
	public void onDealsDataChanged(ArrayList<Deal> deals) {
		mDeals = deals;
	}
	
	public void refreshDeals(View view) {
		findViewById(R.id.empty).setVisibility(View.GONE);
		findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
		if (mQuery != null) {
			searchDeals(mQuery, 0);
		} else {
			getDeals();
		}		
	}
	
	public void onUpdateDeals() {
		if (isUpdating || noMoreDeals) return;
		
		if (mQuery != null) {
			SearchDealsTask task = new SearchDealsTask();
			task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {
				
				@Override
				public void onTaskCompleteSuccess(ArrayList<Deal> result) {
					updateDeals(result);
					isUpdating = false;
					if (result.size() == 0) {
						noMoreDeals = true;
					} else {
						mDealsPage += 1;
					}
					unlockScreenOrientation();
				}
				
				@Override
				public void onTaskFailed(Exception cause) {
					isUpdating = false;
					Log.e(TAG, cause.getMessage(), cause);
					showToastMessage(R.string.failed_msg);
					unlockScreenOrientation();
				}
			});
			
			task.setProgressTracker(new IProgressTracker() {
				
				@Override
				public void onStartProgress() {
					if (progressDialog != null) {
						progressDialog.show();
					}
				}
				
				@Override
				public void onStopProgress() {
					if (progressDialog != null) {
						progressDialog.dismiss();
					}
				}
			});
			
			task.execute(SearchDealsTask.createRequest(mQuery, mDealsPage + 1));
		} else {
			GetDealsTask task = new GetDealsTask();
			task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {

				@Override
	            public void onTaskCompleteSuccess(ArrayList<Deal> result) {
					updateDeals(result);
					isUpdating = false;
					if (result.size() == 0) {
						noMoreDeals = true;
					} else {
						mDealsPage += 1;
					}
					unlockScreenOrientation();
	            }

	            @Override
	            public void onTaskFailed(Exception cause) {
	            	isUpdating = false;
	                Log.e(TAG, cause.getMessage(), cause);
	                showToastMessage(R.string.failed_msg);
	                unlockScreenOrientation();
	            }
			});
			
			task.setProgressTracker(new IProgressTracker() {
				
				@Override
			    public void onStartProgress() {
					if (progressDialog != null) {
						progressDialog.show();
					}
			    }

			    @Override
			    public void onStopProgress() {
			    	if (progressDialog != null) {
			    		progressDialog.dismiss();
			    	}
			    }
			});
			
			task.execute(GetDealsTask.createRequest(mDealsPage + 1, mSelectedCity, mSelectedCategories));
		}
		isUpdating = true;
		lockScreenOrientation();
	}
	
	private void lockScreenOrientation() {
	    int currentOrientation = getResources().getConfiguration().orientation;
	    if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    } else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	    }
	}
	 
	private void unlockScreenOrientation() {
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}
	
	private void updateDeals(ArrayList<Deal> deals) {
		mDeals.addAll(deals);
		mListFragment.updateDeals(mDeals);
		//mListFragment.mAdapter.notifyDataSetChanged();
		mMapFragment.updateDeals(mDeals);
	}
	
	private void onSettingsChanged() {
		noMoreDeals = false;
		
		getSettings();
		
		GetDealsTask task = new GetDealsTask();
		task.setTaskListener(new AsyncTaskListener<ArrayList<Deal>>() {

			@Override
            public void onTaskCompleteSuccess(ArrayList<Deal> result) {
				refreshDeals(result);
				mDealsPage = 1;
				if (result.size() == 0) {
					noMoreDeals = true;
				}
				unlockScreenOrientation();
            }

            @Override
            public void onTaskFailed(Exception cause) {
            	noMoreDeals = true;
                Log.e(TAG, cause.getMessage(), cause);
                showToastMessage(R.string.failed_msg);
                unlockScreenOrientation();
            }
		});
		
		task.setProgressTracker(new IProgressTracker() {
			
			@Override
		    public void onStartProgress() {
				if (progressDialog != null) {
					progressDialog.show();
				}
		    }

		    @Override
		    public void onStopProgress() {
		    	if (progressDialog != null) {
		    		progressDialog.dismiss();
		    	}
		    }
		});
		
		task.execute(GetDealsTask.createRequest(1, mSelectedCity, mSelectedCategories));
		
		lockScreenOrientation();
	}
	
	private void refreshDeals(ArrayList<Deal> deals) {
		mDeals.clear();
		mDeals.addAll(deals);
		mListFragment.updateDeals(mDeals);
		mMapFragment.updateDeals(mDeals, mSelectedCityLong, mSelectedCityLat, mSelectedCityMapZoom);
		//mMapFragment.reCenterMap(mSelectedCityLong, mSelectedCityLat, mSelectedCityMapZoom);
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
		findViewById(R.id.toolbar).setVisibility(View.GONE);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putParcelableArrayList("DEALS_PARCEL_ARRAY", mDeals);
	  savedInstanceState.putString("SEARCH_QUERY", mQuery);
	  savedInstanceState.putInt("ACTIVE_VIEW_PAGE", mPager.getCurrentItem());
	  savedInstanceState.putInt("DEALS_PAGE_LOADED", mDealsPage);
	  savedInstanceState.putInt("DEALS_SELECTED_CITY", mSelectedCity);
  	  savedInstanceState.putDouble("DEALS_SELECTED_CITY_LONG", mSelectedCityLong);
	  savedInstanceState.putDouble("DEALS_SELECTED_CITY_LAT", mSelectedCityLat);
	  savedInstanceState.putDouble("DEALS_SELECTED_CITY_MAPZOOM", mSelectedCityMapZoom);
	  savedInstanceState.putIntegerArrayList("DEALS_SELECTED_CATEGORIES", mSelectedCategories);
//	  if (mSplashDialog != null) {
//		  savedInstanceState.putBoolean("SHOW_SPLASH_SCREEN", true);
//	  } else {
//		  
//	  }
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
	public void onPause() {
	    super.onPause();

	    if(progressDialog != null) {
	    	progressDialog.dismiss();
	    }
	    progressDialog = null;
	}
	
	@Override
    protected void onResume() {
        super.onResume();

        if (progressDialog == null) {
            setProgressDialog();
        }
    }
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHomeOnSearch() {
		if (Utils.hasHoneycomb()) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			ImageView view = (ImageView)findViewById(android.R.id.home);
			view.setPadding(getResources().getDimensionPixelSize(R.dimen.home_up_padding), 0, 0, 0);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		if (mQuery != null) {
			MenuItem settingsMenuItem = menu.findItem(R.id.settings);
			settingsMenuItem.setEnabled(false);
			settingsMenuItem.setVisible(false);
		}
		
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
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.search:
	        	if (!Utils.hasHoneycomb()) {
	        		onSearchRequested();
	        	}
	            return true;
//	        case R.id.clear_history:
//	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
//	            builder.setMessage(getString(R.string.clear_history_dialog))
//	                   .setCancelable(false)
//	                   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//	                       public void onClick(DialogInterface dialog, int id) {
//	                    	   clearSearchSuggestions();
//	                    	   dialog.dismiss();
//	                       }
//	                   })
//	                   .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//	                       public void onClick(DialogInterface dialog, int id) {
//	                            dialog.cancel();
//	                       }
//	                   });
//	            AlertDialog alert = builder.create();
//	            alert.show();
//	        	return true;
	        case R.id.settings:
	        	Intent settingsIntent = new Intent(this, SettingsActivity.class);
	        	startActivityForResult(settingsIntent, 1);
	        	return true;
	        case android.R.id.home:
	        	this.finish();
	        	return true;
//	        case R.id.count_deals:
//	        	Toast.makeText(this, "Number of deals: " + mListFragment.mAdapter.getCount(), Toast.LENGTH_LONG).show();
	        default:
	            return false;
	    }
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (progressDialog == null) {
            setProgressDialog();
        }
		
		if (requestCode == 1) {
			if(resultCode == RESULT_OK){      
				onSettingsChanged();
			}
		    if (resultCode == RESULT_CANCELED) {    
		    	//Write your code if there's no result
		    }
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
        this.progressDialog.setMessage(getString(R.string.loading_deals_msg));
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
	        			mMapFragment = DealsMapFragment.newInstance(mDeals, mSelectedCityLong, mSelectedCityLat, mSelectedCityMapZoom);
	        		}
	        		return mMapFragment;
        	}
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
        	Object object = super.instantiateItem(container, position);
        	switch (position) {
	        	case 0:
	        		DealsListFragment listFrag = (DealsListFragment) object;
	        		if (listFrag != null) {
	        			mListFragment = listFrag;
	        		}
	        		break;
	        	case 1:
	        		DealsMapFragment mapFrag = (DealsMapFragment) object;
	        		if (mapFrag != null) {
	        			mMapFragment = mapFrag;
	        		}
	        		break;
        	}	
        	return object;
        }
        
    }

}
