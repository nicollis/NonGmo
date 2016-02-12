package co.odua.nongmo.fragments;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.widget.ArrayAdapter;
import co.odua.nongmo.R;
import co.odua.nongmo.RootActivity;
import co.odua.nongmo.data.DataHandler;
import co.odua.nongmo.data.ListSet;
import co.odua.nongmo.search.SearchableActivity;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ProductList extends ListFragment 
{	
	public final static String ARG_BRAND = "brand";
	public final static String ARG_CATEGORY = "category";
	public final static String ARG_DATA = "data";
	
    private String mCurrentBrand = null;
    private String mCurrentCategory = null;
    private DataHandler mCurrentData = null;
    private ArrayAdapter<String> adapter;
    private int layout;
    
    public void setData(DataHandler data) { mCurrentData = data; }
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	
    	// We need to use a different list item layout for devices older than Honeycomb
        layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;
    	
    	// If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
        	mCurrentBrand = savedInstanceState.getString(ARG_BRAND);
        	mCurrentCategory = savedInstanceState.getString(ARG_CATEGORY);
        	mCurrentData = (DataHandler) savedInstanceState.getSerializable(ARG_DATA);
        }
        
      //Set up adapter with no data
        adapter = new ArrayAdapter<String>(getActivity(), layout, new ArrayList<String>());
        setListAdapter(adapter);
    }//end onCreate
    
    @Override
    public void onStart() 
    {
    	super.onStart();
    	
    	// During startup, check if there are arguments passed to the fragment.
    	Bundle args = getArguments();
        if (args != null && mCurrentBrand == null) {
        	mCurrentData = (DataHandler) args.getSerializable(ARG_DATA);
            // Set article based on argument passed in
            updateProductView(args.getString(ARG_CATEGORY), args.getString(ARG_BRAND), false);
        } else if (mCurrentBrand != null) {
            // Set article based on saved instance state defined during onCreate
        	updateProductView(mCurrentCategory, mCurrentBrand, false);
        }
    }//end method onStart
    
    @Override
    public void onResume() 
    {
    	super.onResume();
    	//Update Header Text
        ((RootActivity) getActivity()).setHeaderText(getResources().getString(R.string.products));
    }
    
    //Updates the data shown in the ActionBar
    public void updateActionBar(String brandName)
    {
    	//if only One-Pane or Two-Pane, put category list in spinner
    	// or if its an instance of SearchableActivity only show spinner in One-Pane
        if (((RootActivity) getActivity()).getPaneCount() <= 2 
        		|| ((getActivity() instanceof SearchableActivity) && ((RootActivity) getActivity()).getPaneCount() <= 1))
        {
        	//gets dataArray
        	final ListSet<String> brandsList = mCurrentData.getBrands();
        	//pulls the position to be selected
        	final int listenerPosition = brandsList.headSet(brandName).size();
        	//set up onClickListener
        	ActionBar.OnNavigationListener listener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					//make sure the user hasn't selected the same item
					if (listenerPosition != itemPosition)
					{
						mCurrentBrand = (String) (brandsList.toArray())[itemPosition];
						updateProductView(mCurrentCategory, mCurrentBrand, false);
						return true;
					}//end if
					return false;
				}//end method onNavigationItemSelected
			};//end anonymous inner class
			
			//Send data to main RootActivity class
			((RootActivity) getActivity()).setActionBar(brandsList, listenerPosition, listener);
        }//end if
    }//end method updateActionBar
    
    public void updateProductView(String category, String brand, boolean triPane)
    {
    	mCurrentCategory = category;
    	mCurrentBrand = brand;
        adapter.clear();
        //check if a value is null; resets to blank if in three pain view
        if (category == null || brand == null)
        {
        	adapter.notifyDataSetChanged();
        	return;
        }//end if
        for(String product : mCurrentData.getProductsList(mCurrentBrand))
        	adapter.add(product);
    	adapter.notifyDataSetChanged();
    	updateActionBar(mCurrentBrand);
    }//end method updateBrandsView
    
    @Override
    public void onSaveInstanceState(Bundle outState) 
    {
    	super.onSaveInstanceState(outState);
    	
    	//Save the current band/category in selection
    	outState.putString(ARG_CATEGORY, mCurrentCategory);
    	outState.putString(ARG_BRAND, mCurrentBrand);
    	outState.putSerializable(ARG_DATA, mCurrentData);
    }//end method onSaveInstanceState
}//end method BrandList
