package gr.unfold.android.tsibato;

import java.util.List;

import gr.unfold.android.tsibato.async.AsyncTaskListener;
import gr.unfold.android.tsibato.async.IProgressTracker;
import gr.unfold.android.tsibato.data.Category;
import gr.unfold.android.tsibato.wsclient.GetCategoriesTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class KSoapActivity extends Activity {
	
	private static final String TAG = KSoapActivity.class.getName();
	
    static final String INTENT_EXTRA_DO_START_FLAG = "KSoapActivity_INTENT_EXTRA_DO_START_FLAG";
    
    private ProgressDialog progressDialog;
	
    private ListView lvCategories;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_simple_ksoap_example_3);

        setProgressDialog();

        //lvCategories = (ListView) findViewById(R.id.lvCategories);
        /*((Button) findViewById(R.id.btnTestExample3)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                getCategories();
            }
        });*/

        // invoke the web service if the activity was invoked with a flag to invoke the web service
        if (getIntent() != null && getIntent().getExtras().containsKey(INTENT_EXTRA_DO_START_FLAG)) {
            getCategories();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (progressDialog == null) {
            setProgressDialog();
        }
    }
    
    private void getCategories() {
		GetCategoriesTask task = new GetCategoriesTask();
		
		task.setTaskListener(new AsyncTaskListener<List<Category>>() {

			@Override
            public void onTaskCompleteSuccess(List<Category> result) {
                displayCategoryResults(result);
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
		
		task.execute(GetCategoriesTask.createRequest());

	}
    
    private void setProgressDialog() {
    	this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setCancelable(false);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setMessage("Hey");
    }
    
    private void displayCategoryResults(List<Category> results) {
		//lvCategories.setAdapter(new CategoriesAdapter(this, results));
		
		showToastMessage(1);
	}
    
    private void showToastMessage(int messageId) {
		Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
	}
}
