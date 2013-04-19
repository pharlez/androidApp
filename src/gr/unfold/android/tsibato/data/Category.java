package gr.unfold.android.tsibato.data;

public class Category {
	
	private final int id;
	private final String name;
	private final boolean isSelected;
	
	public Category(int id, String name, boolean isSelected) {
		this.id = id;
		this.name = name;
		this.isSelected = isSelected;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean isSelected() {
		return this.isSelected;
	}
	
}
