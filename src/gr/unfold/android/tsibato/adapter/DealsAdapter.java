package gr.unfold.android.tsibato.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import gr.unfold.android.tsibato.R;
import gr.unfold.android.tsibato.data.Deal;
import gr.unfold.android.tsibato.images.ImageFetcher;
import gr.unfold.android.tsibato.views.RecyclingImageView;

public class DealsAdapter extends ArrayAdapter<Deal> {
	
	private LayoutInflater inflater;
	private Context mContext;
	private ImageFetcher mImageFetcher; 
	
	public DealsAdapter(Context context, List<Deal> objects, ImageFetcher imageFetcher) {
		super(context, android.R.layout.simple_list_item_1, objects);
		
		mContext = context;
		mImageFetcher = imageFetcher;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.list_deal, null);			
		}
		
		ImageView imageView = (ImageView)convertView.findViewById(R.id.dealimage);
		if (imageView == null) {
			imageView = new RecyclingImageView(mContext);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		}
		
		TextView price = (TextView)convertView.findViewById(R.id.dealprice);
		TextView value = (TextView)convertView.findViewById(R.id.dealvalue);
		TextView discount = (TextView)convertView.findViewById(R.id.dealdiscount);
		TextView title = (TextView)convertView.findViewById(R.id.dealtitle);
		
		Deal deal = getItem(position);
		
		NumberFormat nf_fr = NumberFormat.getCurrencyInstance(Locale.FRANCE);
		
		BigDecimal p = deal.getPrice().setScale(2, RoundingMode.HALF_UP);
		price.setText(nf_fr.format(p));
		
		BigDecimal v = deal.getValue().setScale(2, RoundingMode.HALF_UP);
		value.setText(mContext.getString(R.string.from_value) + " " + nf_fr.format(v));
		
		discount.setText("-" + deal.getDiscount().setScale(0, RoundingMode.HALF_UP).toString() + mContext.getString(R.string.percent));
		
		title.setText(deal.getTitle());
		
		mImageFetcher.loadImage(deal.getThumbnail(), imageView);
		
		//TextView lineView = (TextView)convertView.findViewById(android.R.id.text1);
		
		//lineView.setText(String.format("[ %d : %d ] - %s, %s, %s: [%f x %f = %f], (%f, %f, %f)", deal.getId(), deal.getPurchases(), deal.getTitle(), deal.getThumbnail(), deal.getProviderLogo(),
		//		deal.getValue(), deal.getDiscount(), deal.getPrice(), deal.getLon(), deal.getLat(), deal.getMapZoom()));
		
		return convertView;
	}

}
