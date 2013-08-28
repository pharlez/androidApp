package gr.unfold.android.tsibato.data;

import android.os.Parcel;
import android.os.Parcelable;

public class City implements Parcelable {
	
	public int id;
	public String name;
	public double lon;
	public double lat;
	public double mapZoom;
	
	public City(int id, String name, double lon, double lat, double zoom) {
		this.id = id;
		this.name = name;
		this.lon = lon;
		this.lat = lat;
		this.mapZoom = zoom;
	}
	
	public City(Parcel in) {
		readFromParcel(in);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.id);
		dest.writeString(this.name);
		dest.writeDouble(this.lon);
		dest.writeDouble(this.lat);
		dest.writeDouble(this.mapZoom);
	}
	
	private void readFromParcel(Parcel in) {
		this.id = in.readInt();
		this.name = in.readString();
		this.lon = in.readDouble();
		this.lat = in.readDouble();
		this.mapZoom = in.readDouble();
	}
	
	public static final Parcelable.Creator<City> CREATOR =
	    	new Parcelable.Creator<City>() {
	            public City createFromParcel(Parcel in) {
	                return new City(in);
	            }
	 
	            public City[] newArray(int size) {
	                return new City[size];
	            }
	        };

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
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
	
	public String toString() {
		return name;
	}
}
