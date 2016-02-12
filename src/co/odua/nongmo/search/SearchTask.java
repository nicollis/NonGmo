//SearchTask.java
//Background task to search for a given keyword in a list of products
package co.odua.nongmo.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.AsyncTask;
import co.odua.nongmo.R;
import co.odua.nongmo.data.DataHandler;

public class SearchTask extends AsyncTask<String, Integer, DataHandler> implements Serializable{
	
	private static final long serialVersionUID = -447069558157098475L;
	private OnSearchTaskListener mCallBack;
	private DataHandler results;
	private Context context;
	public interface OnSearchTaskListener
	{
		void OnSearchTaskFinished(DataHandler results);
	}//end interface
	
	public SearchTask(Context context, OnSearchTaskListener mCallBack) 
	{
		this.mCallBack = mCallBack;
		this.context = context;
	}

	@Override
	protected DataHandler doInBackground(String... arg0) {
		String keyword = arg0[0];
		results = new DataHandler();
		//set some base values
		results.setName(keyword);
		results.setComplete(true);
		String[] categories = context.getResources().getStringArray(R.array.categories_names);
		//loop though each category to look at brands and products
		for (String categoryName : categories)
		{
			//load the brand data
			DataHandler category = DataHandler.loadCategoryInThread(context, categoryName);
			if (category == null)
				continue;
			//Loop though brand names
			for(String brand : category.getBrands())
			{
				//check if brand name is a match
				if (match(keyword, brand))
				{//if brand match add all products to the list
					results.addBrand(brand);
					results.addProductMap(brand, category.getProductsList(brand));
				}//end if
				else //check all brands products to see if there is a match
				{
					List<String> list = new ArrayList<String>();
					//loop though products looking for a match
					for (String product : category.getProductsList(brand))
					{//if product name contains the keyword add it to the list
						if (match(keyword, product))
							list.add(product);
					}//end for
					//if the list isn't empty then add it the the results
					if(!list.isEmpty())
					{
						results.addBrand(brand);
						results.addProductMap(brand, list);
					}//end if
				}//end else
			}//end for
		}//end for
		return results;
	}//end doInBackground
	
	@Override
	protected void onPostExecute(DataHandler result) 
	{
		super.onPostExecute(result);
		//set to null if brands list is empty
		if (result.getBrands().isEmpty())
			result = null;
		if (this.mCallBack != null && !this.isCancelled())
			this.mCallBack.OnSearchTaskFinished(result);
	}//end method onPostExecute
	
	/**
	 * Checks to see if keyword is found in the given string
	 * @param keyword: Text you are looking for
	 * @param string to be tested against the keyword
	 * @return returns true if the keyword is found in the string
	 */
	private boolean match(String keyword, String string)
	{
		Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(string);
		return matcher.find();
	}//end method match
}//end class SearchTask
