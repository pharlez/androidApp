package gr.unfold.android.tsibato;

import gr.unfold.android.tsibato.data.Deal;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class DealActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.item_deal);
		
		Intent intent = getIntent();
		Deal deal = intent.getParcelableExtra("DEAL_PARCEL");
		
		TextView textView = new TextView(this);
		textView.setText(deal.title);
		
		setContentView(textView);
	}
	
}
