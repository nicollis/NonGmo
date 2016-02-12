//NonGmoActivity.java
//Main Activity for NonGmo, controls fragments and maintains data
package co.odua.nongmo;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.odua.nongmo.data.DataHandler;
import co.odua.nongmo.fragments.BrandList;
import co.odua.nongmo.fragments.BrandList.OnBrandSelectedListener;
import co.odua.nongmo.fragments.CategoryList;
import co.odua.nongmo.fragments.CategoryList.OnCategorySelectedListener;
import co.odua.nongmo.fragments.DownloadDialog.DownloadDialogListener;
import co.odua.nongmo.fragments.ProductList;

public class NonGmoActivity extends RootActivity
	implements OnCategorySelectedListener, OnBrandSelectedListener, DownloadDialogListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.setLoadingScreenView((LinearLayout) findViewById(R.id.loading_screen_layout));
		
		//Detect if the category_fragment was inflated, if not initialize the fragment as nothing is in view.
		if (findViewById(R.id.category_fragment) == null)
		{
			this.setPaneCount(1);
			// However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }
            
            //Create an instance of CategoryFragment
            CategoryList firstFragment = new CategoryList();
            
            //add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, firstFragment).commit();
		}//end if
		else //in two or three-pane view
		{//we need to set the fragment_container with a brand/product/null fragment based on
			//what was currently being used in the three-pain view
			//if two-pane
			if (findViewById(R.id.brands_fragment) == null)
			{
				this.setPaneCount(2);
			}//end if
			else //if three-pane
			{
				this.setPaneCount(3);
			}//end else
		}//end else
		
		//get the fragment_container_header if not in three-pane view
		if(this.getPaneCount() < 3)
			fragmentContainerHeader = (TextView) findViewById(R.id.fragment_container_header);
	}//end onCreate method
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		//Show Banner Ad
		//TODO this.hasUpgraded();
	}
	
	//When the Application is Paused, i.e. home button is pressed; phone call comes in
	@Override
	protected void onPause() 
	{
		super.onPause();
	}//end method onPause
	
	//When the application comes back from a Paused state.
	@Override
	protected void onResume() 
	{
		super.onResume();
	}//end method onResume
	
	//Saves the current state for when the application is paused or rotated; saves current screen and values.
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		super.onSaveInstanceState(outState);
	}//end method onSaveInstanceState
	//Restores applications current data from the saved state
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) 
	{
		super.onRestoreInstanceState(savedInstanceState);
	}//end method onRestoreInstanceState

	@Override
	public void onCategorySelected(String name) {
		//The user has selected a category.
		
		//Test to see if brands fragment is already open
		String currentClassName = null;
		
		try
		{
			currentClassName = getSupportFragmentManager()
				.findFragmentById(R.id.fragment_container).getClass().getName();
		}//end try
		catch (NullPointerException ignore){}//end catch
			
		//check to see if we are in three-pane view
		if(this.getPaneCount() == 3)
		{
			BrandList brandFragment = (BrandList)
					getSupportFragmentManager()
					.findFragmentById(R.id.brands_fragment);
			brandFragment.updateBrandsView(name);
			//remove highlight selected brand field
			brandFragment.uncheckItem();
			//clear the product list as its no longer valid
			((ProductList) getSupportFragmentManager()
					.findFragmentById(R.id.products_fragment))
					.updateProductView(null, null, false);
		}//end if
		//if BrandList is already inflated, then update
		else if (currentClassName == BrandList.class.getName())
		{
			BrandList brandFragment = (BrandList)
					getSupportFragmentManager()
					.findFragmentById(R.id.fragment_container);
			brandFragment.updateBrandsView(name);
		}//end if
		//in one-plane view and need to transition to brands view
		else
		{
			//Create Fragment and give it an argument for the selected category
			BrandList newFragment = new BrandList();
			Bundle args = new Bundle();
			args.putString(BrandList.ARG_POSITION, name);
			newFragment.setArguments(args);
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			
			//Replace whatever is in the fragment_container view with this fragment
			transaction.replace(R.id.fragment_container, newFragment);
			transaction.addToBackStack(null);
			
			//Commit the transaction
			transaction.commit();
		}//end else
	}//end method onCategorySelect

	@Override
	public void onBrandSelected(String category, String brand, DataHandler data) 
	{
		if (this.getPaneCount() == 3)
		{//check to see if we are in three-pane view
			//if so just update the fragment that is already inflated
			ProductList productFragment = (ProductList)
					getSupportFragmentManager().findFragmentById(R.id.products_fragment);
			productFragment.setData(data);
			productFragment.updateProductView(category , brand, true);
		}//end if
		else
		{
			//Create Fragment and give it an argument for the selected brand
			ProductList newFragment = new ProductList();
			Bundle args = new Bundle();
			args.putString(ProductList.ARG_CATEGORY, category);
			args.putString(ProductList.ARG_BRAND, brand);
			args.putSerializable(ProductList.ARG_DATA, data);
			newFragment.setArguments(args);
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			
			//Replace the brand fragment currently in view
			transaction.replace(R.id.fragment_container, newFragment);
			transaction.addToBackStack(null);
			
			//Commit the transaction
			transaction.commit();
		}//end else
	}//end method onBrandSelected
	
	//Listener for checkBrandHealth if fails then show message
    //if successful then try to update UI again
    @Override
	public void onDialogHasFinished(boolean wasSuccessful, String mCategoryName) 
    {
    	try
    	{
	    	BrandList.dialog.dismiss();
			BrandList.dialog = null;
	    	
			if(wasSuccessful)
				onCategorySelected(mCategoryName);
	    	else
	    	{
	    		Toast toast = new Toast(this);
	    		toast.setText("Trouble showing data for "+mCategoryName+" check your connection and try again.");
	    		toast.setGravity(Gravity.CENTER, 0, 0);
	    		toast.setDuration(Toast.LENGTH_LONG);
	    	}//end else
    	}catch(IllegalStateException ignore){}//User might have clicked back before response has fired
	}//end method onDialogHasFinished
}//end class NonGmoActivity
