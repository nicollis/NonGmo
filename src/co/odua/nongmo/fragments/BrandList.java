package co.odua.nongmo.fragments;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import co.odua.nongmo.NonGmoActivity;
import co.odua.nongmo.R;
import co.odua.nongmo.RootActivity;
import co.odua.nongmo.data.DataHandler;
import co.odua.nongmo.data.ListSet;
import co.odua.nongmo.data.LoadCategoryTask;
import co.odua.nongmo.data.LoadCategoryTask.OnLoadCategoryListener;
import co.odua.nongmo.search.SearchableActivity;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BrandList extends ListFragment implements OnLoadCategoryListener
{	
	OnBrandSelectedListener mCallback;
	
	public final static String ARG_POSITION = "position";
	public final static String CAT_DATA = "data";
	public static DownloadDialog dialog;
	
    private String mCurrentCategory = null;
    private ArrayAdapter<String> adapter;
    private int layout;
    private int position_selected;
    private DataHandler brand;
    
    public interface OnBrandSelectedListener
    {
    	void onBrandSelected(String category, String brand, DataHandler data);
    }//end interface OnBrandSelectedListener
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	
    	// We need to use a different list item layout for devices older than Honeycomb
        layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;
        
        //Set up adapter with no data
        adapter = new ArrayAdapter<String>(getActivity(), layout, new ArrayList<String>());
        setListAdapter(adapter);
    }//end onCreate
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	// If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
        	mCurrentCategory = savedInstanceState.getString(ARG_POSITION);
        	brand = (DataHandler) savedInstanceState.getSerializable(CAT_DATA);
        }
    }
    
    @Override
    public void onStart() 
    {
    	super.onStart();
    	
    	// During startup, check if there are arguments passed to the fragment.
    	Bundle args = getArguments();
    	String category = null;
        if (args != null && mCurrentCategory == null) {
            // Set article based on argument passed in
        	brand = (DataHandler) args.getSerializable(CAT_DATA);
        	
            category = args.getString(ARG_POSITION);
        } else {
            // Set article based on saved instance state defined during onCreate
        	category = mCurrentCategory;
        }//end else if
        if (category != null)
        {
        	updateBrandsView(category);
        	//Update Header Text
        	((RootActivity) getActivity()).setHeaderText(getResources().getString(R.string.brands));
        }
        //check to see if we are in three-pain view
        if(getFragmentManager().findFragmentById(R.id.brands_fragment) != null)
		{//brands_fragment is only defined in three-pane view
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		}
    }//end method onStart
    
    @Override
    public void onAttach(Activity activity) 
    {
    	super.onAttach(activity);
    	
    	// This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
		try
		{
			mCallback = (OnBrandSelectedListener) activity;
		}//end try
		catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement OnCategorySelectedListener");
		}//end catch
    }//end method onAttach
    
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onResume() 
    {
    	super.onResume();
    }//end method onResume
    
    //Updates the data shown in the ActionBar
    public void updateActionBar(String categoryName)
    {
    	//if only One-Pane and is an instance of NonGmoActivity, put category list in spinner
    	RootActivity activity = ((RootActivity) getActivity());
    	if (activity == null)
    	{
    		return;
    	}
        if (activity.getPaneCount() == 1 && getActivity() instanceof NonGmoActivity)
        {
        	//gets dataArray
        	final ListSet<String> categoryList = new ListSet<String>(getResources().getStringArray(R.array.categories_names));
        	//pulls the position to be selected
        	final int listenerPosition = categoryList.headSet(categoryName).size();
        	//set up onClickListener
        	ActionBar.OnNavigationListener listener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					//make sure the user hasn't selected the same item
					if (listenerPosition != itemPosition)
					{
						mCurrentCategory = (String) categoryList.toArray()[itemPosition];
						updateBrandsView(mCurrentCategory);
						return true;
					}//end if
					return false;
				}//end method onNavigationItemSelected
			};//end anonymous inner class
			
			//Send data to main RootActivity class
			((RootActivity) getActivity()).setActionBar(categoryList, listenerPosition, listener);
        }//end if
        else if (((RootActivity) getActivity()).getPaneCount() == 2 || getActivity() instanceof SearchableActivity)
        {//if in two pane mode, reset the action bar because products spinner might be active
        	((RootActivity) getActivity()).setActionBar(null, 0, null);
        }//end else if
    }//end method updateActionBar
    
    public void updateBrandsView(String name)
    {   
    	RootActivity activity = ((RootActivity) getActivity());
    	if (activity == null)
    	{
    		return;
    	}
    	mCurrentCategory = name;
    	//if data if from array update brand object
    	if(brand == null || !brand.getName().equals(name))
    	{
    		//load brand from cache
    		activity.showLoadingScreen();
    		new LoadCategoryTask(getActivity(), this).execute(name);
    		return;
    	}
    	//Close spinner
    	activity.hideLoadingScreen();
        adapter.clear();
        for (String brand : this.brand.getBrands())
        	adapter.add(brand);
    	adapter.notifyDataSetChanged();
    	updateActionBar(mCurrentCategory);
    	//count click
    	activity.countClick();
    }//end method updateBrandsView
    
    //Fires when LoadCategoryTask has finished
    @Override
	public void hasFinished(DataHandler category) {
    	brand = category; //set category to brand data
    	//check its health
    	if (!brandIsHealthy())
    		//if not healthy downloadDialog will take over
    		return;
    	updateBrandsView(brand.getName());
	}//end listener hasFinished
    
    public boolean brandIsHealthy()
    {
    	//only run if in an instance of NonGmoActivity
    	if(!(getActivity() instanceof NonGmoActivity))
    		return true;
    	//check if brand is null, or is corrupted
    	if(brand == null || !brand.isComplete())
    	{
    		int id = 0;
    		String[] categoryNames = getResources().getStringArray(R.array.categories_names);
			String[] categoryIds = getResources().getStringArray(R.array.categories_id);
			for (int i = 0; i < categoryNames.length; i++)
			{
				if (categoryNames[i].contentEquals(mCurrentCategory))
				{
					id = Integer.parseInt(categoryIds[i]);
					break;
				}//end if
			}//end for
    		String url = getResources().getString(R.string.categories_base_url)+id;
    		String backupUrl = getResources().getString(R.string.categories_base_url_backup)+id;
    		//build the dialog
    		BrandList.dialog = new DownloadDialog();
    		dialog.setUrl(url, backupUrl);
    		dialog.setContext(getActivity());
    		dialog.setName(mCurrentCategory);
    		dialog.setCancelable(false);
    		dialog.show(getFragmentManager(), mCurrentCategory+" DownloadDialog");
    		return false;
    	}//end if
    	else 
    		return true;
    }//end method checkBrandHealth
    
    //Sends callback when brand item is clicked
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) 
    {
    	super.onListItemClick(l, v, position, id);
    	
    	mCallback.onBrandSelected(mCurrentCategory, adapter.getItem(position), brand);
    	
    	//set the item as checked to be highlighted when in three-pane layout
    	getListView().setItemChecked(position, true);
    	position_selected = position;
    }//end method onListItemClick
    
    public void uncheckItem()
    {
    	getListView().setItemChecked(position_selected, false);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) 
    {
    	super.onSaveInstanceState(outState);
    	
    	//Save the current band in selection
    	outState.putString(ARG_POSITION, mCurrentCategory);
    	outState.putSerializable(CAT_DATA, brand);
    }//end method onSaveInstanceState
}//end method BrandList
