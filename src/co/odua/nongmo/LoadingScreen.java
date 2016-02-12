//LoadingScreen.java
//Initial Screen to NonGmo, Loads in data, or downloads and saves if needed
package co.odua.nongmo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import co.odua.nongmo.data.DataHandler;
import co.odua.nongmo.data.SyncService;

public class LoadingScreen extends Activity
{
	boolean finishedLoading = false;
	
	//GUI DataTypes
	TextView loadingStatusTextView;
	ProgressBar loadingProgressBar;
	TextView downloadingMesasgeTextView;
	ProgressBar circleLoadingProgressBar;
	TextView percentTextView;
	
	//Downloading progress tracker
	int numberOfCategories;
	int numberOfCategoriesDone = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_screen);
		
		loadingStatusTextView = (TextView) findViewById(R.id.loading_text_view);
		loadingProgressBar = (ProgressBar) findViewById(R.id.loading_progression_bar);
		percentTextView = (TextView) findViewById(R.id.percent_text_view);
		downloadingMesasgeTextView = (TextView) findViewById(R.id.downloading_message);
		circleLoadingProgressBar = (ProgressBar) findViewById(R.id.circle_loading_bar);
		circleLoadingProgressBar.setProgress(0);
		
		//lock the screen orientation
		int currentOrientation = getResources().getConfiguration().orientation;
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
		   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		else {
		   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}//end method onCreate
	
	@Override
	protected void onResume() {
		super.onResume();
		downloadCategories();
	}//end method onResume
	
	//fires when the category data has been loaded from cache
	public void downloadCategories() 
	{
		long dwnTime = DataHandler.getMasterDateCached(this);
		if (dwnTime <= 0)
		{//if failed start download
			//update UI to show download is needed
			loadingProgressBar.setProgress(numberOfCategoriesDone);
			//Get the custom ProgressBar Style
			Drawable pbDrawable = getResources().getDrawable(R.drawable.loading_progressbar);
			loadingProgressBar.setProgressDrawable(pbDrawable);
			circleLoadingProgressBar.setVisibility(View.GONE);
			loadingProgressBar.setVisibility(View.VISIBLE);
			downloadingMesasgeTextView.setVisibility(View.VISIBLE);
			loadingStatusTextView.setText(getResources().getString(R.string.downloading_text_message));
			percentTextView.setVisibility(View.VISIBLE);
			percentTextView.setText("0%");
			//start downloading info and update UI as needed
			syncCategories();
			return;
		}//end if
		else if(DataHandler.hasExpired(dwnTime))//if data has been download'ed, but expired start background sync
		{
			//Start Sync Service
			Intent backgroundSync = new Intent(this, SyncService.class);
			backgroundSync.putExtra(SyncService.URL_KEY, getResources().getString(R.string.categories_sync_url));
			backgroundSync.putExtra(SyncService.URL_BACKUP_KEY, getResources().getString(R.string.categories_sync_backup_url));
			startService(backgroundSync);
		}
		loadingHasFinsihed();
	}//end listener method haveLoaded
	
	//Runs SyncCategoryTask to pull down all category data in one html file
	private void syncCategories()
	{
		//Test to see if the background service is running
		//if so, attaches a new receiver to it, and updates the ui 
		//without starting a new service
		if (isSyncServiceRunning())
		{
			SyncService.receiver = new DownloadReceiver(new Handler());
			return;
		}
		//Save current time as when Master Sync was ran
		DataHandler.saveMasterDateCached(this);
		
		//set number of categories in the progress bar and text
		numberOfCategories = getResources().getStringArray(R.array.categories_names).length;
		loadingProgressBar.setMax(numberOfCategories);
		//Start Sync Service
		Intent serviceIntent = new Intent(this, SyncService.class);
		serviceIntent.putExtra(SyncService.URL_KEY, getResources().getString(R.string.categories_sync_url));
		serviceIntent.putExtra(SyncService.URL_BACKUP_KEY, getResources().getString(R.string.categories_sync_backup_url));
		serviceIntent.putExtra(SyncService.RECEIVER_KEY, new DownloadReceiver(new Handler()));
		startService(serviceIntent);
	}//end method syncCategories
	
	//Inner class to receive data from SyncService
	private class DownloadReceiver extends ResultReceiver
	{
		public DownloadReceiver(Handler handler)
		{
			super(handler);
		}//end constructor
		
		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) 
		{
			super.onReceiveResult(resultCode, resultData);
			//get current progress
			int progress = resultData.getInt(SyncService.PROGRESS_KEY);
			//values to be set regardless of what update it is
			loadingProgressBar.setMax(100);
			loadingProgressBar.setProgress(progress);
			percentTextView.setText(progress+"%");
			//Test to see if its a Download or Parse Update
			if (resultCode == SyncService.DOWNLOAD_PROGRESS)
			{
				loadingStatusTextView.setText(getResources().getString(R.string.downloading_text_message));
				//change to parsing message when complete
				if (progress >= 100)
				{
					loadingProgressBar.setProgress(0);
					percentTextView.setText("0%");
					loadingStatusTextView.setText(getResources().getString(R.string.completed_text_message));
				}//end if
			}//end if
			else if (resultCode == SyncService.PARSE_PROGRESS)//is an parse update
			{
				loadingStatusTextView.setText(getResources().getString(R.string.completed_text_message));
			}//end else
			else if (resultCode == SyncService.SERVICE_COMPLETED)
			{//all the data is completed
				loadingHasFinsihed();
			}
			else if (resultCode == SyncService.SERVICE_FAILED)
			{//if downloading failed
				String message = resultData.getString(SyncService.MESSAGE_KEY);
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
				finish();
			}//end if
		}//end method onReceiveResult
	}//end anonymous inner class
	
	private void loadingHasFinsihed()
	{
		finishedLoading = true;
		Intent intent = new Intent(LoadingScreen.this, NonGmoActivity.class);
		startActivity(intent);
		LoadingScreen.this.finish();//prevents back button from pulling this screen
	}//end method loadingHasFinsihed
	
	private boolean isSyncServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (SyncService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}//end class LoadingScreen
