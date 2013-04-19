package gr.unfold.android.tsibato.data;

public class City {
	
	private final int id;
	private final String name;
	private final boolean isSelected;
	private final double lon;
	private final double lat;
	private final double mapZoom;
	
	public City(int id, String name, boolean isSelected, double lon, double lat, double zoom) {
		this.id = id;
		this.name = name;
		this.isSelected = isSelected;
		this.lon = lon;
		this.lat = lat;
		this.mapZoom = zoom;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isSelected() {
		return isSelected;
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
