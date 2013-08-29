package gr.unfold.android.tsibato;

import gr.unfold.android.tsibato.async.AsyncTaskListener;
import gr.unfold.android.tsibato.async.IProgressTracker;
import gr.unfold.android.tsibato.data.Category;
import gr.unfold.android.tsibato.data.City;
import gr.unfold.android.tsibato.util.Utils;
import gr.unfold.android.tsibato.wsclient.GetCategoriesTask;
import gr.unfold.android.tsibato.wsclient.GetCitiesTask;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity implements OnClickListener {
	
	private static final String TAG = SettingsActivity.class.getName();
	
	private ProgressDialog progressDialog;
	private boolean asyncCitiesRunning;
	private boolean asyncCategoriesRunning;
	
	private ArrayList<City> mCities;
	private ArrayList<Category> mCategories;
	
	private SharedPreferences mSettings;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.settings);
        
        findViewById(R.id.categorySelectAllCheck).setOnClickListener(this);
        findViewById(R.id.settingsOkBtn).setOnClickListener(this);
        findViewById(R.id.settingsCancelBtn).setOnClickListener(this);
        
        setProgressDialog();
        
        mSettings = getSharedPreferences("gr.unfold.android.tsibato.settings", MODE_PRIVATE);
        
        if (savedInstanceState != null) {
        	mCities = savedInstanceState.getParcelableArrayList("SETTINGS_CITIES");
        	mCategories = savedInstanceState.getParcelableArrayList("SETTINGS_CATEGORIES");
        	setCities();
        	setCategories();
        	return;
        }
        
        getCities();
        getCategories();
        
    }
	
	private void setCities() {
		ArrayAdapter<City> citiesAdapter = new ArrayAdapter<City>(this, android.R.layout.simple_spinner_item, mCities);
        citiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = (Spinner) findViewById(R.id.citySpinner);
        spinner.setAdapter(citiesAdapter);
        
        int selectedCity = mSettings.getInt("SELECTED_CITY_ID", -1);
        if (selectedCity > 0) {
        	spinner.setSelection(indexOfCity(mCities, selectedCity));
        }
	}
	
	private void setCategories() {
		ArrayAdapter<Category> categoriesAdapter = new ArrayAdapter<Category>(this,
                android.R.layout.simple_list_item_multiple_choice, mCategories);
        ListView listView = (ListView) findViewById(R.id.categoriesList);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(categoriesAdapter);
        
        String selectedCategories = mSettings.getString("SELECTED_CATEGORIES_IDS", "");
        StringTokenizer strToken = new StringTokenizer(selectedCategories, ",");
        int totalItemsChecked = 0;
        while (strToken.hasMoreTokens()) {
        	int categoryId = Integer.parseInt(strToken.nextToken());
        	listView.setItemChecked(indexOfCategory(mCategories, categoryId), true);
        	totalItemsChecked += 1;
        }
        CheckBox cb = (CheckBox) findViewById(R.id.categorySelectAllCheck);
        TextView selectAllTextView = (TextView) findViewById(R.id.categorySelectAllTitle);			
        if (totalItemsChecked == mCategories.size()) {
        	cb.setChecked(true);
        } else if (totalItemsChecked == 0) {
        	cb.setChecked(true);
        	selectAllCategories();
        }
	}
	
	private void getCities() {
		GetCitiesTask task = new GetCitiesTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<City>>() {

			@Override
            public void onTaskCompleteSuccess(ArrayList<City> result) {
				mCities = result;
				setCities();
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
		        asyncCitiesRunning = true;
		    }

		    @Override
		    public void onStopProgress() {
		    	asyncCitiesRunning = false;
		    	if (!asyncCategoriesRunning) {
		    		progressDialog.dismiss();
		    	}
		    }
		});
		
		task.execute(GetCitiesTask.createRequest());
	}
	
	private void getCategories() {
		GetCategoriesTask task = new GetCategoriesTask();
		
		task.setTaskListener(new AsyncTaskListener<ArrayList<Category>>() {

			@Override
            public void onTaskCompleteSuccess(ArrayList<Category> result) {
				mCategories = result;
				setCategories();
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
		        asyncCategoriesRunning = true;
		    }

		    @Override
		    public void onStopProgress() {
		    	asyncCategoriesRunning = false;
		    	if (!asyncCitiesRunning) {
		    		progressDialog.dismiss();
		    	}
		    }
		});
		
		task.execute(GetCategoriesTask.createRequest());
	}
	
	private void saveSelectedCity() {
		Spinner spinner = (Spinner) findViewById(R.id.citySpinner);
		City city = (City) spinner.getSelectedItem();
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putInt("SELECTED_CITY_ID", city.id).commit();
		editor.putFloat("SELECTED_CITY_LONG", (float) city.lon);
		editor.putFloat("SELECTED_CITY_LAT", (float) city.lat);
		editor.putFloat("SELECTED_CITY_MAPZOOM", (float) city.mapZoom);
		editor.commit();
	}
	
	private void saveSelectedCategories() {
		ListView listView = (ListView) findViewById(R.id.categoriesList);
		SparseBooleanArray checked = listView.getCheckedItemPositions();
		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < checked.size(); i++) {
			int position = checked.keyAt(i);
            if (checked.valueAt(i)) {
            	strBuilder.append(mCategories.get(position).id).append(",");
            }
		}
		mSettings.edit().putString("SELECTED_CATEGORIES_IDS", strBuilder.toString()).commit();
	}
	
	private void selectAllCategories() {
		ListView listView = (ListView) findViewById(R.id.categoriesList);
		for(int i=0; i < listView.getAdapter().getCount(); i++){
			listView.setItemChecked(i, true);
		}
	}
	
	private void deselectAllCategories() {
		ListView listView = (ListView) findViewById(R.id.categoriesList);
		for(int i=0; i < listView.getAdapter().getCount(); i++){
			listView.setItemChecked(i, false);
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private boolean anyCategoriesSelected() {
		ListView listView = (ListView) findViewById(R.id.categoriesList);
		if (Utils.hasHoneycomb()) {
			return listView.getCheckedItemCount() != 0;
		} else {
			SparseBooleanArray checked = listView.getCheckedItemPositions();
			for (int i = 0; i < checked.size(); i++) {
	            if (checked.valueAt(i)) {
	            	return true;
	            }
			}
			return false;
		}		
	}
	
	@Override
	public void onClick(View v) {
		Intent returnIntent = new Intent();
		switch (v.getId()) {
			case R.id.categorySelectAllCheck:
				boolean checked = ((CheckBox) v).isChecked();
				TextView selectAllTextView = (TextView) findViewById(R.id.categorySelectAllTitle);
				if (checked) {
					selectAllTextView.setText(getResources().getString(R.string.select_all_categories));
					selectAllCategories();
				} else {
					selectAllTextView.setText(getResources().getString(R.string.deselect_all_categories));
					deselectAllCategories();
				}
				break;
			case R.id.settingsOkBtn:
				if (!anyCategoriesSelected()) {
					showToastMessage(R.string.select_categories_alert);
					break;
				}
				saveSelectedCity();
				saveSelectedCategories();
				setResult(RESULT_OK, returnIntent);     
				finish();
				break;
			case R.id.settingsCancelBtn:
				setResult(RESULT_CANCELED, returnIntent);        
				finish();
				break;
		}
	}
	
	@Override
    protected void onResume() {
        super.onResume();

        if (progressDialog == null) {
            setProgressDialog();
        }
    }
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putParcelableArrayList("SETTINGS_CITIES", mCities);
	  savedInstanceState.putParcelableArrayList("SETTINGS_CATEGORIES", mCategories);
	  super.onSaveInstanceState(savedInstanceState);
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
	
	public static int indexOfCity(ArrayList<City> cities, int id) {
		for (int i = 0; i < cities.size(); i++) {
			City city = cities.get(i);
			if (city.id == id) {
				return i;
			}
		}
		return -1;
	}
	
	public static int indexOfCategory(ArrayList<Category> categories, int id) {
		for (int i = 0; i < categories.size(); i++) {
			Category category = categories.get(i);
			if (category.id == id) {
				return i;
			}
		}
		return -1;
	}

}
