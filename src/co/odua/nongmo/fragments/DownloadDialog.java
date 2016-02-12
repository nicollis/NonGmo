//DownloadDialog.java
//Dialog Fragment that appears if a category is corrupt and needs to be download'ed again.
package co.odua.nongmo.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import co.odua.nongmo.R;
import co.odua.nongmo.data.SyncService;

public class DownloadDialog extends DialogFragment 
{
	TextView mDialogMessage;
	TextView mProgressText;
	ProgressBar mProgressBar;
	
	String url, backupUrl, mCategoryName;
	DownloadDialogListener mCallBack;
	Context context;
	
	/**
	 * The activity that creates an instance of this dialog fragment
	 * must implement this interface in order to receive event callback.
	 */
	public interface DownloadDialogListener 
	{
		public void onDialogHasFinished(boolean wasSuccessful, String mCategoryName);
	}//end method DownloadDialogListener
	
	//Constructors
	public DownloadDialog(){}
	
	//Getters and Setters
	public void setUrl(String url, String backupUrl){this.url = url; this.backupUrl = backupUrl;}
	public String getUrl(){return this.url;}
	public void setContext(Context context) {this.context = context;}
	public void setName(String name){this.mCategoryName = name;}
	
	//Overridden methods
	//Build and inflate the dialog
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		//Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		//Inflate and set the layout for the dialog
		// pass null as the parent view because its going in the dialog layout
		View view = inflater.inflate(R.layout.download_dialog, null);
		builder.setView(view);
		
		//attach the UI
		mDialogMessage = (TextView) view.findViewById(R.id.download_text_message);
		mProgressText = (TextView) view.findViewById(R.id.download_percent_text);
		mProgressBar = (ProgressBar) view.findViewById(R.id.download_progress_bar);
		//Get the custom ProgressBar Style
		Drawable pbDrawable = getResources().getDrawable(R.drawable.loading_progressbar);
		mProgressBar.setProgressDrawable(pbDrawable);
		return builder.create();
	}
	
	//insure there is a callback method
	@Override
	public void onAttach(Activity activity) 
	{
		super.onAttach(activity);
		try
		{
			mCallBack = (DownloadDialogListener) activity;
		}//end try
		catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() 
					+ "must implement DownloadDialogListener");
		}//end catch
	}//end method onAttach
	
	//on Show dialog, start the download/parsing process
	@Override
	public void show(FragmentManager manager, String tag) 
	{
		super.show(manager, tag);
		
		//Start Sync Service
		Intent serviceIntent = new Intent(context, SyncService.class);
		if (url == null || backupUrl == null) {throw new NullPointerException("Must define a URL with setUrl(Stiring url)");}
		serviceIntent.putExtra(SyncService.URL_KEY, url);
		serviceIntent.putExtra(SyncService.URL_BACKUP_KEY, backupUrl);
		serviceIntent.putExtra(SyncService.RECEIVER_KEY, new DownloadReceiver(new Handler()));
		context.startService(serviceIntent);
	}
	
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
				mProgressBar.setMax(100);
				mProgressBar.setProgress(progress);
				mProgressText.setText(progress+"%");
				//Test to see if its a Download or Parse Update
				if (resultCode == SyncService.DOWNLOAD_PROGRESS)
				{
					mDialogMessage.setText(getResources().getString(R.string.downloading_text_message));
					//change to parsing message when complete
					if (progress >= 100)
					{
						mProgressBar.setProgress(0);
						mProgressText.setText("0%");
						mDialogMessage.setText(getResources().getString(R.string.completed_text_message));
					}//end if
				}//end if
				else if (resultCode == SyncService.PARSE_PROGRESS)//is an parse update
				{
					mDialogMessage.setText(getResources().getString(R.string.completed_text_message));
				}//end else
				else if (resultCode == SyncService.SERVICE_COMPLETED)
				{//all the data is completed
					mCallBack.onDialogHasFinished(true, mCategoryName);
				}
				else if (resultCode == SyncService.SERVICE_FAILED)
				{//if downloading failed
					mCallBack.onDialogHasFinished(false, mCategoryName);
				}//end if
			}//end method onReceiveResult
		}//end anonymous inner class
}//end class DownloadDialog
