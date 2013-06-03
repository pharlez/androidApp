package gr.unfold.android.tsibato.adapter;

import java.text.DecimalFormat;
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
	
	private final int[] bgColors = new int[] { R.color.list_white, R.color.list_gray };
	
	private LayoutInflater inflater;
	private Context mContext;
	private ImageFetcher mImageFetcher;
	
//	private static List<Deal> mDeals;
	
	public DealsAdapter(Context context, List<Deal> objects, ImageFetcher imageFetcher) {
		super(context, android.R.layout.simple_list_item_1, objects);
		
		mContext = context;
		mImageFetcher = imageFetcher;
//		mDeals = objects;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.list_deal, null);
			
			viewHolder = new ViewHolder();
			viewHolder.priceView = (TextView)convertView.findViewById(R.id.dealprice);
			viewHolder.valueView = (TextView)convertView.findViewById(R.id.dealvalue);
			viewHolder.discountView = (TextView)convertView.findViewById(R.id.dealdiscount);
			viewHolder.titleView = (TextView)convertView.findViewById(R.id.dealtitle);
			
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
//		int colorPosition = position % bgColors.length;
//        convertView.setBackgroundResource(bgColors[colorPosition]);
		if (position % 2 == 0) {
			convertView.setBackgroundResource(R.drawable.item_selector_odd);
			//convertView.setBackgroundColor(mContext.getResources().getColor(R.color.item_selector_odd));
		} else {
			convertView.setBackgroundResource(R.drawable.item_selector_even);
			//convertView.setBackgroundColor(mContext.getResources().getColor(R.color.item_selector_even));
		}
        
		
		ImageView imageView = (ImageView)convertView.findViewById(R.id.dealimage);
		if (imageView == null) {
			imageView = new RecyclingImageView(mContext);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		}
		
//		TextView price = (TextView)convertView.findViewById(R.id.dealprice);
//		TextView value = (TextView)convertView.findViewById(R.id.dealvalue);
//		TextView discount = (TextView)convertView.findViewById(R.id.dealdiscount);
//		TextView title = (TextView)convertView.findViewById(R.id.dealtitle);
		
		Deal deal = getItem(position);
//		Deal deal = mDeals.get(position);
//		String[] data = deal.data;
		
		mImageFetcher.loadImage(deal.thumbnail, imageView);
		
		DecimalFormat nf_el = (DecimalFormat) NumberFormat.getInstance(new Locale("el"));
		nf_el.setMinimumFractionDigits(2);
		nf_el.setMaximumFractionDigits(2);
		
		//BigDecimal p = deal.price.setScale(2, RoundingMode.HALF_UP);
		viewHolder.priceView.setText(nf_el.format(deal.price) + mContext.getString(R.string.euro_symbol));
//		viewHolder.priceView.setText("50,00");
		
		//BigDecimal v = deal.value.setScale(2, RoundingMode.HALF_UP);
		viewHolder.valueView.setText(mContext.getString(R.string.from_value) + " " + nf_el.format(deal.value) + mContext.getString(R.string.euro_symbol));
//		viewHolder.valueView.setText(mContext.getString(R.string.from_value) + " " + "500,00");
		
		viewHolder.discountView.setText("-" + deal.discount + mContext.getString(R.string.percent));
//		viewHolder.discountView.setText("-" + "90" + mContext.getString(R.string.percent));
		
		viewHolder.titleView.setText(deal.getSmallTitle(300));
//		viewHolder.titleView.setText("Fucking hell is this working smoothly now");		
		
		//TextView lineView = (TextView)convertView.findViewById(android.R.id.text1);
		
		//lineView.setText(String.format("[ %d : %d ] - %s, %s, %s: [%f x %f = %f], (%f, %f, %f)", deal.getId(), deal.getPurchases(), deal.getTitle(), deal.getThumbnail(), deal.getProviderLogo(),
		//		deal.getValue(), deal.getDiscount(), deal.getPrice(), deal.getLon(), deal.getLat(), deal.getMapZoom()));
		
		return convertView;
	}
	
	static class ViewHolder {
		TextView priceView;
		TextView valueView;
		TextView discountView;
		TextView titleView;
	}
	
}
