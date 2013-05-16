package gr.unfold.android.tsibato;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.images.ImageFetcher;
import gr.unfold.android.tsibato.images.ImageCache.ImageCacheParams;
import gr.unfold.android.tsibato.views.RecyclingImageView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class DealActivity extends FragmentActivity {
	private static final String TAG = "DealActivity";
    private static final String IMAGE_CACHE_DIR = "images";
	
	private int mImageHeight;
	private int mImageWidth;
	
	private ImageFetcher mImageFetcher;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mImageHeight = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_height);
		mImageWidth = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_width);
		
		ImageCacheParams cacheParams = new ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory
        
        mImageFetcher = new ImageFetcher(this, mImageWidth, mImageHeight);
        mImageFetcher.setLoadingImage(R.drawable.ic_launcher);
        mImageFetcher.addImageCache(this.getSupportFragmentManager(), cacheParams);
		
		setContentView(R.layout.item_deal);
		
		Intent intent = getIntent();
		Deal deal = intent.getParcelableExtra("DEAL_PARCEL");
		
		TextView price = (TextView) findViewById(R.id.dealprice);
		TextView value = (TextView) findViewById(R.id.dealvalue);
		TextView discount = (TextView) findViewById(R.id.dealdiscount);
		
		ImageView imageView = (ImageView) findViewById(R.id.dealimage);
		if (imageView == null) {
			imageView = new RecyclingImageView(this);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		}
		
		TextView title = (TextView) findViewById(R.id.dealtitle);
		
		DecimalFormat nf_el = (DecimalFormat) NumberFormat.getInstance(new Locale("el"));
		nf_el.setMinimumFractionDigits(2);
		nf_el.setMaximumFractionDigits(2);
		
		price.setText(nf_el.format(deal.price) + getString(R.string.euro_symbol));
		value.setText(getString(R.string.from_value) + " " + nf_el.format(deal.value) + getString(R.string.euro_symbol));
		discount.setText("-" + deal.discount + getString(R.string.percent));
		
		mImageFetcher.loadImage(deal.thumbnail, imageView);
		
		title.setText(deal.title);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // This is called when the Home (Up) button is pressed in the Action Bar.
	            Intent parentActivityIntent = new Intent(this, MainActivity.class);
	            parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	            startActivity(parentActivityIntent);
	            finish();
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
}
