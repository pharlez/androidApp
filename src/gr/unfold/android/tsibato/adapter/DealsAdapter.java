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
	
	private LayoutInflater inflater;
	private Context mContext;
	private ImageFetcher mImageFetcher;
	
	private List<Deal> mDeals;
	
	public DealsAdapter(Context context, List<Deal> objects, ImageFetcher imageFetcher) {
		super(context, android.R.layout.simple_list_item_1, objects);
		
		mContext = context;
		mImageFetcher = imageFetcher;
		mDeals = objects;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public List<Deal> getDeals() {
		return mDeals;
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
		
		if (position % 2 == 0) {
			convertView.setBackgroundResource(R.drawable.item_selector_odd);
		} else {
			convertView.setBackgroundResource(R.drawable.item_selector_even);
		}
        
		
		ImageView imageView = (ImageView)convertView.findViewById(R.id.dealimage);
		if (imageView == null) {
			imageView = new RecyclingImageView(mContext);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		}

		
		Deal deal = getItem(position);
		
		mImageFetcher.loadImage(deal.getThumbnail(), imageView);
		
		DecimalFormat nf_el = (DecimalFormat) NumberFormat.getInstance(new Locale("el"));
		nf_el.setMinimumFractionDigits(2);
		nf_el.setMaximumFractionDigits(2);
		
		viewHolder.priceView.setText(nf_el.format(deal.price) + mContext.getString(R.string.euro_symbol));
		
		viewHolder.valueView.setText(mContext.getString(R.string.from_value) + " " + nf_el.format(deal.value) + mContext.getString(R.string.euro_symbol));
		
		viewHolder.discountView.setText("-" + deal.discount + mContext.getString(R.string.percent));
		
		viewHolder.titleView.setText(deal.getSmallTitle(300));
		
		return convertView;
	}
	
	static class ViewHolder {
		TextView priceView;
		TextView valueView;
		TextView discountView;
		TextView titleView;
	}
	
}
