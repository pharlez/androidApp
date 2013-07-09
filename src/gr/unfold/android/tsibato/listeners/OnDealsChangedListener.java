package gr.unfold.android.tsibato.listeners;

import gr.unfold.android.tsibato.data.Deal;

import java.util.ArrayList;

public interface OnDealsChangedListener {
	
	public void onDealsDataChanged(ArrayList<Deal> deals);
	
	public void onListMapVisibilityChanged(boolean isListVisible);
	
}
