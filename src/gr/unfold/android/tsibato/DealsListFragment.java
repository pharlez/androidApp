package gr.unfold.android.tsibato;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import gr.unfold.android.tsibato.adapter.DealsAdapter;
import gr.unfold.android.tsibato.data.Deal;

public class DealsListFragment extends ListFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ArrayList<Deal> deals = new ArrayList<Deal>();
		
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			deals = bundle.getParcelableArrayList("DEALS_PARCEL_ARRAY");
		}
		
		// have to get data into an ArrayList<Deal> to pass to the list adapter
		setListAdapter(new DealsAdapter(getActivity(), deals));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// logic for onStart here
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		// logic for onAttach here
	}
	
}
