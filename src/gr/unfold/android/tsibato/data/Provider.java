package gr.unfold.android.tsibato.data;

public class Provider {

	private final int id;
	private final String name;
	private final boolean isSelected;
	
	public Provider(int id, String name, boolean isSelected) {
		this.id = id;
		this.name = name;
		this.isSelected = isSelected;
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
}
