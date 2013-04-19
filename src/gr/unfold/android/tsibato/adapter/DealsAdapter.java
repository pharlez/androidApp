package gr.unfold.android.tsibato.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import gr.unfold.android.tsibato.R;
import gr.unfold.android.tsibato.data.Deal;

public class DealsAdapter extends ArrayAdapter<Deal> {
	
	private LayoutInflater inflater;
	
	public DealsAdapter(Context context, List<Deal> objects) {
		super(context, android.R.layout.simple_list_item_1, objects);
		
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.list_deal, null);
		}
		
		Deal deal = getItem(position);
		
		TextView price = (TextView)convertView.findViewById(R.id.dealprice);
		TextView value = (TextView)convertView.findViewById(R.id.dealvalue);
		TextView discount = (TextView)convertView.findViewById(R.id.dealdiscount);
		TextView title = (TextView)convertView.findViewById(R.id.dealtitle);
		
		price.setText(deal.getPrice().toString());
		value.setText(deal.getValue().toString());
		discount.setText(deal.getDiscount().toString());
		title.setText(deal.getTitle());		
		
		//TextView lineView = (TextView)convertView.findViewById(android.R.id.text1);
		
		//lineView.setText(String.format("[ %d : %d ] - %s, %s, %s: [%f x %f = %f], (%f, %f, %f)", deal.getId(), deal.getPurchases(), deal.getTitle(), deal.getThumbnail(), deal.getProviderLogo(),
		//		deal.getValue(), deal.getDiscount(), deal.getPrice(), deal.getLon(), deal.getLat(), deal.getMapZoom()));
		
		return convertView;
	}

}
