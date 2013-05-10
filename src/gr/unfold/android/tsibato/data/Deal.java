package gr.unfold.android.tsibato.data;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

public class Deal implements Parcelable {
	
	public int id;
	public String title;
	public String thumbnail;
	public String providerLogo;
	public int purchases;
	public BigDecimal price;
	public BigDecimal value;
	public BigDecimal discount;
	public double lon;
	public double lat;
	public double mapZoom;
	
	public Deal(int id, String title, String thumb, String logo, int purchases, BigDecimal price, BigDecimal value, BigDecimal discount,
			double lon, double lat, double zoom) {
		this.id = id;
		this.title = title;
		this.thumbnail = thumb;
		this.providerLogo = logo;
		this.purchases = purchases;
		this.price = price;
		this.value = value;
		this.discount = discount;
		this.lon = lon;
		this.lat = lat;
		this.mapZoom = zoom;
	}
	
	public Deal(Parcel in) {
		readFromParcel(in);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.id);
		dest.writeString(this.title);
		dest.writeString(this.thumbnail);
		dest.writeString(this.providerLogo);
		dest.writeInt(this.purchases);
		dest.writeString(this.price.toString());
		dest.writeString(this.value.toString());
		dest.writeString(this.discount.toString());
		dest.writeDouble(this.lon);
		dest.writeDouble(this.lat);
		dest.writeDouble(this.mapZoom);
	}
	
	private void readFromParcel(Parcel in) {
		this.id = in.readInt();
		this.title = in.readString();
		this.thumbnail = in.readString();
		this.providerLogo = in.readString();
		this.purchases = in.readInt();
		this.price = new BigDecimal(in.readString());
		this.value = new BigDecimal(in.readString());
		this.discount = new BigDecimal(in.readString());
		this.lon = in.readDouble();
		this.lat = in.readDouble();
		this.mapZoom = in.readDouble();
	}
	
	public static final Parcelable.Creator<Deal> CREATOR =
	    	new Parcelable.Creator<Deal>() {
	            public Deal createFromParcel(Parcel in) {
	                return new Deal(in);
	            }
	 
	            public Deal[] newArray(int size) {
	                return new Deal[size];
	            }
	        };

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getThumbnail() {
		return thumbnail.replace("_78x58", "");
	}

	public String getProviderLogo() {
		return providerLogo;
	}

	public int getPurchases() {
		return purchases;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BigDecimal getValue() {
		return value;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public double getLon() {
		return lon;
	}

	public double getLat() {
		return lat;
	}

	public double getMapZoom() {
		return mapZoom;
	}
}
