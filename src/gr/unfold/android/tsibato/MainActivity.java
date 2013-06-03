package gr.unfold.android.tsibato;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.widget.Toast;

import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;

import gr.unfold.android.tsibato.async.AsyncTaskListener;
import gr.unfold.android.tsibato.async.IProgressTracker;
import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.wsclient.GetDealsTask;
import gr.unfold.android.tsibato.util.Utils;

public class MainActivity extends FragmentActivity
		implements OnDealSelectedListener {
	
	private static final String TAG = MainActivity.class.getName();
	
	private ProgressDialog progressDialog;
	
	private ArrayList<Deal> mDeals;

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
                showToastMessage(2);
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
	
	private void displayDeals(ArrayList<Deal> results) {
		// Create an instance of DealsListFragment
        DealsListFragment listFragment = new DealsListFragment();
        
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("DEALS_PARCEL_ARRAY", results);
        
        listFragment.setArguments(bundle);
        
        // Add the fragment to the 'fragment_container' FrameLayout
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, listFragment).commit();
	}
	
	public void onDealSelected(int position) {
		Intent intent = new Intent(this, DealActivity.class);
		intent.putExtra("DEAL_PARCEL", mDeals.get(position));
		startActivity(intent);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
