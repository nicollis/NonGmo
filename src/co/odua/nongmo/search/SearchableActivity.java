//SearchableActivity.java
//Gets search request and finds all matching brands and products
//and displays them in list fragments
package co.odua.nongmo.search;


import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.widget.LinearLayout;
import android.widget.Toast;
import co.odua.nongmo.R;
import co.odua.nongmo.RootActivity;
import co.odua.nongmo.data.DataHandler;
import co.odua.nongmo.data.ListSet;
import co.odua.nongmo.fragments.BrandList;
import co.odua.nongmo.fragments.BrandList.OnBrandSelectedListener;
import co.odua.nongmo.fragments.ProductList;
import co.odua.nongmo.search.SearchTask.OnSearchTaskListener;

public class SearchableActivity extends RootActivity 
	implements OnBrandSelectedListener, OnSearchTaskListener
{
	private static final String ARG_RESULTS = "results";
	private static final String ARG_QUERY = "query";
	private static final String IS_SEARCHING_KEY = "is_searching";
	
	private String query = "query";
	private DataHandler results = null;
	
	private static SearchTask mSearchTask;
	private static boolean isSearching = false;
	private static boolean	activityInForeground = false;
	private static SearchableActivity mInstance;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchable);
		this.setLoadingScreenView((LinearLayout) findViewById(R.id.loading_screen_layout));
		
		//See if screen has one or two pane's
		if (findViewById(R.id.brands_fragment) != null)
			this.setPaneCount(2);
		else
			this.setPaneCount(1);
		//skip handleIntent if recovering from a screen rotation
		if (savedInstanceState == null && results == null)
			handleIntent(getIntent());
		else
		{
			results = (DataHandler) savedInstanceState.getSerializable(ARG_RESULTS);
			query = savedInstanceState.getString(ARG_QUERY);
		}
		if (savedInstanceState != null)
		{
			isSearching = savedInstanceState.getBoolean(IS_SEARCHING_KEY);
		}
		
		mInstance = this;
	}//end method onCreate
	
	@Override
	protected void onNewIntent(Intent intent) 
	{
		setIntent(intent);
		//Only call if UI is already inflated
		if (findViewById(R.id.fragment_container) != null)
			handleIntent(intent);
	}//end method onNewIntent
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		activityInForeground = true;
		if (isSearching)//show loading screen if still searching
			this.showLoadingScreen();
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		activityInForeground = false;
	}//end onDestroy
	
	private void handleIntent(Intent intent)
	{
		//show loading screen and start searching for matches
		this.showLoadingScreen();
		//Get the intent and get the request
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			query = intent.getStringExtra(SearchManager.QUERY);
			//save query to SuggestionProvider
			SearchRecentSuggestions sugggestion = new SearchRecentSuggestions(this, 
					SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
			sugggestion.saveRecentQuery(query, null);
			
			results = new DataHandler();
			mSearchTask = new SearchTask(this, this);
			mSearchTask.execute(query);
		}//end if
	}//end method handleIntent
	
	//Handles showing the initial brands list to the user
	private void showBrandsList(DataHandler results)
	{
		//Create Fragment and give it an argument for the selected category
		BrandList newFragment = new BrandList();
		Bundle args = new Bundle();
		args.putString(BrandList.ARG_POSITION, results.getName());
		args.putSerializable(BrandList.CAT_DATA, results);
		newFragment.setArguments(args);
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		
		//Replace whatever is in the fragment_container view with this fragment
		if (this.getPaneCount() == 2)
			transaction.replace(R.id.brands_fragment, newFragment);
		else
			transaction.replace(R.id.fragment_container, newFragment);
		
		//Commit the transaction
		transaction.commit();
	}//end method showBrandsList

	@Override
	public void onBrandSelected(String category, String brand, DataHandler data) {
		//Create Fragment and give it an argument for the selected brand
		ProductList newFragment = new ProductList();
		Bundle args = new Bundle();
		args.putString(ProductList.ARG_CATEGORY, category);
		args.putString(ProductList.ARG_BRAND, brand);
		args.putSerializable(ProductList.ARG_DATA, results);
		newFragment.setArguments(args);
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		
		//Replace the brand fragment currently in view
		transaction.replace(R.id.fragment_container, newFragment);
		if (this.getPaneCount() != 2)//if tablet layout then let back button just return to NonGmoActivity
			transaction.addToBackStack(null);
		
		//Commit the transaction
		transaction.commit();
	}
	
	@Override
	public void setActionBar(ListSet<String> dataArray, int position,
			OnNavigationListener listener) 
	{
		super.setActionBar(dataArray, position, listener);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setSubtitle(query);
		//show back button in action bar
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayUseLogoEnabled(false);
	}
	
	@Override
	public void setHeaderText(String text) 
	{
		if (this.getPaneCount() != 2)
			super.setHeaderText(text);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		super.onSaveInstanceState(outState);
		//save results
		outState.putSerializable(ARG_RESULTS, results);
		outState.putString(ARG_QUERY, query);
		if (mSearchTask != null)
			outState.putBoolean(IS_SEARCHING_KEY, true);
		else
			outState.putBoolean(IS_SEARCHING_KEY, false);
	}
	
	//Fires when SearchTask is finished
	//results are null if nothing was found
	@Override
	public void OnSearchTaskFinished(DataHandler results) 
	{
		mSearchTask = null;//free the memory of the search task
		if (!activityInForeground)
			return; //if activity was closed just return.
		//hide loading screen now that its finished
		this.hideLoadingScreen();
		this.results = results;
		if(results != null)
			mInstance.showBrandsList(results);
		else
		{//if nothing found, show message and close
			String message = getResources().getString(R.string.search_nothing_found);
			Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
			toast.show();
			finish();
		}//end else
	}//end method OnSearchTaskFinished
}//end class SearchableActivity
