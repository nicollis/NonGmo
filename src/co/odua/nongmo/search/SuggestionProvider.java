//SuggestionProvider.java
//Mechanics behind showing search history
package co.odua.nongmo.search;

import android.content.SearchRecentSuggestionsProvider;

public class SuggestionProvider extends SearchRecentSuggestionsProvider 
{
	public final static String AUTHORITY = "co.odua.nongmo.search.SuggestionProvider";
	public final static int MODE = DATABASE_MODE_QUERIES;
	
	public SuggestionProvider()
	{
		setupSuggestions(AUTHORITY, MODE);
	}//end constructor 
}//end class SuggestionProvider
