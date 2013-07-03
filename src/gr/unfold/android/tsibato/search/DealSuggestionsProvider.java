package gr.unfold.android.tsibato.search;

import android.content.SearchRecentSuggestionsProvider;

public class DealSuggestionsProvider extends SearchRecentSuggestionsProvider {
	
	public final static String AUTHORITY = "gr.unfold.android.tsibato.search.DealSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;
    
    public DealSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

}
