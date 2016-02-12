package co.odua.nongmo.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import co.odua.nongmo.R;
import co.odua.nongmo.RootActivity;
import co.odua.nongmo.data.ListSet;


public class CategoryList extends ListFragment {
	
	OnCategorySelectedListener mCallback;
	ListSet<String> categoryList;
	
	// The container Activity must implement this interface so the fragment can deliver message
	public interface OnCategorySelectedListener
	{
		public void onCategorySelected(String name);
	}//end interface OnCategorySelectedListener
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		// We need to use a different list item layout for devices older than Honeycomb
        int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;
        
        categoryList = new ListSet<String>(getResources().getStringArray(R.array.categories_names));
        // Create an Array Adapter for the list view
        setListAdapter(new ArrayAdapter<String>(getActivity(), layout,  (String[]) categoryList.toArray()));
	}//end method onCreate
	
	@Override
	public void onStart() 
	{
		super.onStart();
        
		// When in two-pane layout, set the ListView to highlight the selected list item
        // (We do this during onStart because at the point the ListView is available.)
		if(((RootActivity) getActivity()).getPaneCount() >= 2)
		{
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
			mCallback = (OnCategorySelectedListener) activity;
		}//end try
		catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement OnCategorySelectedListener");
		}//end catch
	}//end method onAttach
	
	@Override
	public void onResume() 
	{
		super.onResume();
		//Update Header Text, Only if in One-Pane View
        if(((RootActivity) getActivity()).getPaneCount() == 1)
        {
        	((RootActivity) getActivity()).setHeaderText(getResources().getString(R.string.categories));
        	//Update ActionBar
        	((RootActivity) getActivity()).setActionBar(null, 0, null);
        }
        //make sure loading spinner isn't stuck open
        ((RootActivity) getActivity()).hideLoadingScreen();
	}//end method onResume
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) 
	{
		super.onListItemClick(l, v, position, id);
		//Notify the parent activity of the selected item
		mCallback.onCategorySelected((String) categoryList.toArray()[position]);
		
		//set the item as checked to be highlighted when in two-pane layout
		getListView().setItemChecked(position, true);
	}//end method onListItemClick
}//end class CategoryList
