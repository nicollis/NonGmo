package co.odua.nongmo.data;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class LoadCategoryTask extends
		AsyncTask<String, Integer, DataHandler> 
{
	private Context context;
	private OnLoadCategoryListener listener;
	
	public interface OnLoadCategoryListener
	{
		void hasFinished(DataHandler category);
	}//end interface LoadCategoriesListener
	
	public LoadCategoryTask(Context context, OnLoadCategoryListener listener)
	{
		this.context = context;
		this.listener = listener;
	}//end constructor

	@Override
	protected DataHandler doInBackground(String... params) {
		
		String categoryString = params[0];
		DataHandler categoryData = DataHandler.loadCategoryInThread(context, categoryString);
		
		//Log Error if a category returned null
		if (categoryData == null)
		{
			Log.e("LOAD_CATEGORIES NULL", categoryString + " retuned null");
			return null; //if a file doesn't load return null
		}//end if
		return categoryData;
	}
	
	@Override
	protected void onPostExecute(DataHandler result) 
	{
		super.onPostExecute(result);
		listener.hasFinished(result);
	}
	
}//end class LoadCategoryFromCacheTask
