package gr.unfold.android.tsibato.listeners;

import gr.unfold.android.tsibato.data.Deal;

public interface OnDealSelectedListener {
	/** Called when a deal is selected from either the DealsList or the DealsMap */
	public void onDealSelected(Deal deal);
	
}
