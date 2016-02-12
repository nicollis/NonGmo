//RootActivity.java
//Base class for activities in NonGmo to extend from
package co.odua.nongmo;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.odua.nongmo.data.DataHandler;
import co.odua.nongmo.data.ListSet;
import co.odua.nongmo.search.SuggestionProvider;
import co.odua.nongmo.utils.IabHelper;
import co.odua.nongmo.utils.IabResult;
import co.odua.nongmo.utils.Inventory;
import co.odua.nongmo.utils.Purchase;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdView;
import com.google.ads.InterstitialAd;

public class RootActivity extends ActionBarActivity implements AdListener
{
	public static final String BRAND_KEY = "brand";
	public static final String CATEGORY_KEY = "category";
	private static final String UPGRADED_KEY = "upgraded";
	private static final String HAS_CHECKED_UPGRADE_KEY = "has_checked_upgrade";

	protected TextView  fragmentContainerHeader;
	protected LinearLayout mLoadingScreenView;
	protected AdView adView;
	private InterstitialAd interstitial;
	private boolean adHasLoaded = false;
	private boolean adFailed = false;
	private boolean showAdWhenLoaded = false;
	static private boolean isUpgraded = true;
	private boolean hasCheckedUpgraded = false;
	
	private IabHelper mIabHelper;
	private String mUpgradeSku = "remove_ads_nongmo";
	private int mRequestCode = 10001;
	
	static String key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAltg5W3D0zwDJ0l3M0t8xRuwNy+PGR/1piy3JxGzCNCj3iHPiBBQ2P5t3uiLPEQqQ/VenFr1IYsQxDloyE/ODwYyDwb4iqjNuyf4BZjD5JkmZd0XYxoOWvT+sh+dbEmUCiRmpHB3gGXatlIbgJxdaK4uBViopZ1zzDW4qzvvujxrcbw1mNbJHiSXbMiq6A9vLGVTHjS1xl4EDcHd98+UGne69+PauC1IvXz6DE2D6Z2KgKS3+9EnaqEBHpQyJoKq8kqxHbIMKyVqpBfbszaLbTF0j8iOgUO8u1ZqRPfGLbbzBXfWha+O0XiIx5/Y9Dm5FIC6YRaJMtj7ukY8cvSgaEwIDAQAB";
	
	private AtomicInteger PaneCount;
	
	
	public RootActivity()
	{
		PaneCount = new AtomicInteger();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		//check to see if upgraded status has already been searched
		if (savedInstanceState != null)
		{
			isUpgraded = savedInstanceState.getBoolean(UPGRADED_KEY);
			hasCheckedUpgraded = savedInstanceState.getBoolean(HAS_CHECKED_UPGRADE_KEY);
		}
		
		mIabHelper = new IabHelper(this, key);
	}//end method onCreate
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		//Get reference to ad View
		adView = (AdView) this.findViewById(R.id.adView);
		adView.setVisibility(View.GONE);
		//start connection to play store
		mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			   public void onIabSetupFinished(IabResult result) {
			      if (!result.isSuccess()) {
			         // Oh noes, there was a problem.
			         Log.d("ROOTACTIVITY", "Problem setting up In-app Billing: " + result);
			      }            
			         // Hooray, IAB is fully set up!
			      	//check for upgrade
				    isUpgraded();
			   }
		});
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		if (mIabHelper != null) mIabHelper.dispose();
		mIabHelper = null;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		super.onSaveInstanceState(outState);
		//Save upgrade status
		outState.putBoolean(UPGRADED_KEY, isUpgraded);
		outState.putBoolean(HAS_CHECKED_UPGRADE_KEY, hasCheckedUpgraded);
	}
	
	protected void setLoadingScreenView(LinearLayout LoadingScreen)
	{
		this.mLoadingScreenView = LoadingScreen;
	}
	protected LinearLayout getLoadingScreenView(){return this.mLoadingScreenView;}
	
	/**
	 * Shows Loading Screen when loading brands list or search
	 */
	public void showLoadingScreen()
	{
		try
		{
			mLoadingScreenView.setVisibility(View.VISIBLE);
			//lock the current screen orientation
		}catch (NullPointerException e)
		{
			Log.e("ROOTACTIVITY", "Must define Loading Screen in Activity");
		}//end try/catch
	}//end method showLoadingScreen
	/**
	 * Hides Loading Screen when loading brands list or search
	 */
	public void hideLoadingScreen()
	{
		try
		{
			mLoadingScreenView.setVisibility(View.GONE);
			//unlock the screen orientation
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}catch (NullPointerException e)
		{
			Log.e("ROOTACTIVITY", "Must define Loading Screen in Activity");
		}//end try/catch
	}//end method hideLoadingScreen
	
	//inflates the options menu for the application.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options_menu, menu);
		
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		return true;
	}//end onCreateOptionsMenu method
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId()){
		//NonGmo Application Icon
		case android.R.id.home:
			FragmentManager fm= getSupportFragmentManager();
			 if(fm.getBackStackEntryCount()>0){
			   fm.popBackStack(); 
			 }else
			 {
				 finish();
			 }
			return true;
		//Search Button
		case R.id.action_search:
			return true;
		//Upgrade NonGmo
		case R.id.action_store:
			try
			{//wrap in try/catch to make sure aSync task doesn't crash app if its not closed properly
				mIabHelper.launchPurchaseFlow(this, mUpgradeSku, mRequestCode,   
					   mPurchaseFinishedListener, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");//TODO auto generate pay-load string
			}
			catch (IllegalStateException e)
			{
				Log.e("ROOTACIVITY", "PurchaseFlow not closed: "+e.getMessage());
			}
			return true;
		//Review NonGmo
		case R.id.action_review:
			UriIntent(R.string.play_store_link);
			return true;
		//Clear History
		case R.id.action_clear_history:
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
			        SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
			suggestions.clearHistory();
			return true;
		//Buying Fruits/Vegetables
		case R.id.action_buy_veggies:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(this.getLayoutInflater().inflate(R.layout.buying_veggies, null));
			builder.create().show();
			return true;
		//More about NonGmo's
		case R.id.action_more_about_gmos:
			UriIntent(R.string.about_gmo_link);
			return true;
		default:
		 return super.onOptionsItemSelected(item);
		}//end switch
	}//end method onOptionsItemSelected
	
	private void UriIntent(int uri)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(getResources().getString(uri)));
		startActivity(intent);
	}//end method UriIntent
	
	public int getPaneCount(){ return PaneCount.get();}
	public void setPaneCount(int i) {PaneCount.set(i);}
	
	/**
	 * Changes the Header Text for the fragment_container
	 * Viable in Single and Two-Pane Views
	 * @param text Text to be set
	 */
	public void setHeaderText(String text)
	{
		fragmentContainerHeader = (TextView) findViewById(R.id.fragment_container_header);
		if (fragmentContainerHeader != null)
			fragmentContainerHeader.setText(text);
	}//end method setHeaderText
	
	/**
	 * Updates the ActionBar View
	 * @param dataArray String[] the be shown in spinner, if null resets to default
	 * @param position Selects an item in the spinner to be focused on
	 * @param listener OnNavigationListener that responds to clicks
	 */

	public void setActionBar(ListSet<String> dataArray, int position, OnNavigationListener listener)
	{
		ActionBar actionBar = getSupportActionBar();
		//if dataArray is null, then reset back to default NonGmo state
		if (dataArray == null)
		{
			//remove the back button on the action bar
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setDisplayUseLogoEnabled(true);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			return;
		}//end if
		//set the back button on the action bar
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayUseLogoEnabled(false);
		//set drop down menu
		ArrayAdapter<String> spinnerAdapter =
	            new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
	                    (String[]) dataArray.toArray());
		//set adapter and listener to actionBar
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(spinnerAdapter, listener);
		actionBar.setSelectedNavigationItem(position);
	}//end method setActionBar
	
	//********************************
	//			Store Functions
	//*******************************
	// Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
        	mIabHelper.flagEndAsync();
        	if (result.isFailure()) {
   	         Log.d("ROOTACTIVITY", "Error purchasing: " + result);
   	         if (result.getResponse() == 7)
   	         {
   	        	 alert(getResources().getString(R.string.already_upgraded));
   	         }
   	         return;
   	      }      
   	      else if (purchase.getSku().equals(mUpgradeSku)) {
   	         setUpgraded(true);
   	      }
        }
    };
    
    //Fires if store is just closed out by back button, or clicking outside the window
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	if (requestCode == mRequestCode)
    	{
    		mIabHelper.flagEndAsync();
    		//have it double check for a purchase
    		isUpgraded = false;
    		isUpgraded();
    	}
    };
	
	protected void alert(String message)
	{
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}
	
	protected void setUpgraded(boolean value)
	{
		//upgrade UI accordingly
		if (value)//if user has upgraded
		{//remove ads if able
			if (adView != null)
			{
				adView.destroy();
				adView.setVisibility(View.GONE);
				adView = null;
			}
			if (interstitial != null)
			{
				interstitial = null;
			}
		}//end if
		else
		{//show ads
			loadInterstitialAd();
			adView = (AdView) this.findViewById(R.id.adView);
			adView.setVisibility(View.VISIBLE);
			try
			{
				adView.loadAd(this.getAdRequest());
			}
			catch (NullPointerException e)
			{
				Log.e("NonGmo", "Activity that extends RootActivity doesn't impliment adView");
			}
		}//end else
		hasCheckedUpgraded = true;
		isUpgraded = value;
	}
	
	//Checks with google to see if app has updated 
	//if hasen't already
	protected boolean isUpgraded()
	{
		if (hasCheckedUpgraded && isUpgraded)
			return isUpgraded;
		else
		{
			try
			{
				hasCheckedUpgraded = true;
				mIabHelper.queryInventoryAsync(mGotInventoryListener);
			}catch (IllegalStateException e)
			{
				Log.e("NonGmo", e.getMessage());
			}catch (NullPointerException e)
			{
				Log.e("NonGmo", e.getMessage());
				isUpgraded = true;
			}
		}
		
		return isUpgraded;
	}
	
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener 
	   = new IabHelper.QueryInventoryFinishedListener() {
	   public void onQueryInventoryFinished(IabResult result,
	      Inventory inventory) {

	      if (result.isFailure()) {
	        // handle error here
	      }
	      else {
	        // does the user have the premium upgrade?
	        setUpgraded(inventory.hasPurchase(mUpgradeSku));        
	      }
	   }
	};
	
	//********************************
	//			Ad Functions
	//*******************************
	private final static String CLICK_COUNTER = "click_counter";
	
	/**
	 * Counts clicks for showing full screen ads
	 * @return current clicks counting the one that was just recorded
	 */
	public int countClick()
	{
		if (isUpgraded())//don't run if upgraded
			return 1;
		
		int clicks = getClicks() + 1;
		//if this is click 5 show full ad, and reset counter
		if (clicks >= 5)
		{
			showInterstitialAd();
			clicks = -1;
		}
		Editor editor = this.getSharedPreferences(DataHandler.NONGMO_SHARED_PREFRENCES, Context.MODE_PRIVATE).edit();
		editor.putInt(CLICK_COUNTER, clicks);
		editor.commit();
		return clicks;
	}
	
	public int getClicks()
	{
		SharedPreferences sp = this.getSharedPreferences(DataHandler.NONGMO_SHARED_PREFRENCES, Context.MODE_PRIVATE);
		return sp.getInt(CLICK_COUNTER, 3);
	}
	
	protected AdRequest getAdRequest()
	{
		AdRequest request = new AdRequest();
		
		request.addTestDevice(AdRequest.TEST_EMULATOR);
		request.addTestDevice("F4C23BDD372EAFA6F391227C8B395BE2");	//My T-Mobile Nexus 4
		
		return request;
	}
	
	protected void loadInterstitialAd()
	{
		// Create the interstitial
	    interstitial = new InterstitialAd(this, "ca-app-pub-4169925702361116/8550713389");

	    // Create ad request
	    AdRequest adRequest = new AdRequest();

	    // Begin loading your interstitial
	    interstitial.loadAd(adRequest);

	    // Set Ad Listener to use the callbacks below
	    interstitial.setAdListener(this);
	}
	
	protected void showInterstitialAd()
	{
		if (isUpgraded())
			return; 
		
		if (adFailed)
			return;
		if (adHasLoaded)
			interstitial.show();
		else
			showAdWhenLoaded = true;
	}
	
	@Override
	  public void onReceiveAd(Ad ad) {
		if (ad == interstitial)
			adHasLoaded = true;
		if (showAdWhenLoaded)
		{
			interstitial.show();
			showAdWhenLoaded = false;
		}
	}

	@Override
	public void onDismissScreen(Ad arg0) {
		adHasLoaded = false;
		showAdWhenLoaded = false;
		loadInterstitialAd();
	}

	@Override
	public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
		adFailed = true;
	}

	@Override
	public void onLeaveApplication(Ad arg0) {
	}

	@Override
	public void onPresentScreen(Ad arg0) {
	}
}//end class RootActivity