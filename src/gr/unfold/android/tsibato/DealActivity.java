package gr.unfold.android.tsibato;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import gr.unfold.android.tsibato.data.Deal;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class DealActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_deal);
		
		Intent intent = getIntent();
		Deal deal = intent.getParcelableExtra("DEAL_PARCEL");
		
		TextView price = (TextView) findViewById(R.id.dealprice);
		TextView value = (TextView) findViewById(R.id.dealvalue);
		TextView discount = (TextView) findViewById(R.id.dealdiscount);
		
		DecimalFormat nf_el = (DecimalFormat) NumberFormat.getInstance(new Locale("el"));
		nf_el.setMinimumFractionDigits(2);
		nf_el.setMaximumFractionDigits(2);
		
		price.setText(nf_el.format(deal.price) + getString(R.string.euro_symbol));
		value.setText(getString(R.string.from_value) + " " + nf_el.format(deal.value) + getString(R.string.euro_symbol));
		discount.setText("-" + deal.discount + getString(R.string.percent));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // This is called when the Home (Up) button is pressed
	            // in the Action Bar.
	            Intent parentActivityIntent = new Intent(this, MainActivity.class);
	            parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	            startActivity(parentActivityIntent);
	            finish();
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
}
