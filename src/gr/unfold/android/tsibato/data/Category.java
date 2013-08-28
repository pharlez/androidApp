package gr.unfold.android.tsibato.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Category implements Parcelable {
	
	public int id;
	public String name;
	
	public Category(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public Category(Parcel in) {
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
	}
	
	private void readFromParcel(Parcel in) {
		this.id = in.readInt();
		this.name = in.readString();
	}
	
	public static final Parcelable.Creator<Category> CREATOR =
	    	new Parcelable.Creator<Category>() {
	            public Category createFromParcel(Parcel in) {
	                return new Category(in);
	            }
	 
	            public Category[] newArray(int size) {
	                return new Category[size];
	            }
	        };
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String toString() {
		return name;
	}
	
}
