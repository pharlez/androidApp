package gr.unfold.android.tsibato;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.images.ImageFetcher;
import gr.unfold.android.tsibato.images.ImageCache.ImageCacheParams;
import gr.unfold.android.tsibato.util.Utils;
import gr.unfold.android.tsibato.views.RecyclingImageView;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
		
		shaveOffCorners((Button) findViewById(R.id.dealBtn));
		shaveOffCorners((Button) findViewById(R.id.shareBtn));
		
		//StateListDrawable stateList = (StateListDrawable) btn.getBackground();
//		LayerDrawable layer = (LayerDrawable) btn.getBackground();
//		BitmapDrawable pattern = (BitmapDrawable) layer.getDrawable(1);
//		Drawable d = new CurvedAndTiled(pattern.getBitmap(), 10);
//		layer.setDrawableByLayerId(1, d);
//		//stateList.addState(1, layer);
//		if (Utils.hasJellyBean()) {
//			btn.setBackground(layer);
//		} else {
//			btn.setBackgroundDrawable(layer);
//		}
		
	}
	
	public void goToWebPage(View view) {
		String url = "http://www.google.com/";
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}
	
	public void shareDeal(View view) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.shareTitle)));
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
	
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
            bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output ;
	}
	
	private void shaveOffCorners(final View view) {          
	      view.post(new Runnable() {

	    	  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	    	  @SuppressWarnings("deprecation")
	    	  @Override
	    	  public void run() {
	    		  view.buildDrawingCache();
	    		  // this is the exact bitmap that is currently rendered to screen.  hence, we wanted to 
	    		  // hide all the children so that they don't permanently become part of the background
	    		  final Bitmap rounded = view.getDrawingCache();

	    		  if (Utils.hasJellyBean()) {
	    			  view.setBackground(new BitmapDrawable(
	    					  getResources(), 
	    					  getRoundedCornerBitmap(rounded, getResources().getDimensionPixelSize(R.dimen.radius_corner_button))));
	    		  } else {
	    			  view.setBackgroundDrawable(new BitmapDrawable(
	    					  getResources(), 
	    					  getRoundedCornerBitmap(rounded, getResources().getDimensionPixelSize(R.dimen.radius_corner_button))));
	    		  }

	    	  }
	      });
	}
}
