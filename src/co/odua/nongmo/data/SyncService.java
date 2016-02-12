//SyncService.java
//Download and parses Data in the background
package co.odua.nongmo.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import co.odua.nongmo.R;
import co.odua.nongmo.data.ParseTask.ParseListener;

public class SyncService extends IntentService implements ParseListener
{
	public static final int DOWNLOAD_PROGRESS = 8344;
	public static final int PARSE_PROGRESS = 8345;
	public static final int SERVICE_COMPLETED = 8346;
	public static final int SERVICE_FAILED = 8347;
	
	public static final String URL_KEY	= "url";
	public static final String URL_BACKUP_KEY = "url_backup";
	public static final String RECEIVER_KEY = "receiver";
	public static final String PROGRESS_KEY = "progress";
	public static final String DATA_KEY = "data";
	public static final String MESSAGE_KEY = "message";
	
	public static ResultReceiver receiver;
	
	private int categoriesSize;
	private int parsedComplete = 0;
	private boolean isComplete = false;

	public SyncService() {
		super("SyncService");
	}//end constructor SyncService
	
	@Override
	public void onHandleIntent(Intent intent) {
		Log.i("SyncService", "Started");
		String urlString = intent.getStringExtra(URL_KEY);
		String urlBackupString = intent.getStringExtra(URL_BACKUP_KEY);
		if (urlBackupString == null)
			urlBackupString = urlString;
	    receiver = (ResultReceiver) intent.getParcelableExtra(RECEIVER_KEY);
		
		String xml = null;
		xml = getXml(urlString);
		//if XML failed the try backup
		if (xml == null || xml.length() <= 0)
			xml = getXml(urlBackupString);
		//fire's if both download attempts fail
		if (xml == null || xml.length() <= 0)
		{
			Bundle bundle = new Bundle();
			bundle.putInt(PROGRESS_KEY, 0);
			bundle.putString(MESSAGE_KEY, getApplicationContext().getResources().getString(R.string.download_failed_toast));
			if(receiver != null)
				receiver.send(SERVICE_FAILED, bundle);
			stopSelf();
			return;
		}//end if
		//Download complete start parse and save
		Document main_xml = Jsoup.parse(xml);
		//break main_html into each section
		Elements categoriesDiv = main_xml.select("category");
		categoriesSize = categoriesDiv.size();
		//loop though and parse each category
		for (int i = 0; i < categoriesDiv.size(); i++)
		{
			Element categoryData = categoriesDiv.get(i);
			parseComplete(ParseTask.ParseInThread(categoryData));
		}//end for
		while(!isComplete){try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}}//end try/catch
		//send a message saying service has completed
		Bundle bundle = new Bundle();
		bundle.putInt(PROGRESS_KEY, 100);
		if (receiver != null)
			receiver.send(SERVICE_COMPLETED, bundle);
		stopSelf();
	}//end method onHandleIntent
	
	//Fires when a ParseTask has finished
	@Override
	public void parseComplete(DataHandler data) 
	{
		parsedComplete++; 
		//save the data to cache
		if (data != null)
			DataHandler.saveCategory(this, data);
		//publish the progress
		Bundle bundle = new Bundle();
		bundle.putInt(PROGRESS_KEY, (int) (parsedComplete * 100 / categoriesSize));
		bundle.putSerializable(DATA_KEY, data);
		if (receiver != null)
			receiver.send(PARSE_PROGRESS, bundle);
		if(parsedComplete >= categoriesSize)
			isComplete = true;
	}//end listener parseComplete
	
	private String getXml(String url)
	{
		String xml = null;
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			InputStream in = response.getEntity().getContent();
			
			long fileLength = response.getEntity().getContentLength();
			int total = 0;
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				total += line.length();
				//build response
				Bundle bundle = new Bundle();
				bundle.putInt(PROGRESS_KEY, ((int) (total * 100 / fileLength)));
				if (receiver != null)
					receiver.send(DOWNLOAD_PROGRESS, bundle);
				str.append(line);
			}//end while
			//set progress to 100 because download is done
			Bundle bundle = new Bundle();
			bundle.putInt(PROGRESS_KEY, 100);
			if(receiver != null)
				receiver.send(DOWNLOAD_PROGRESS, bundle);
			in.close();
			xml = str.toString();
		}//end try
		catch (UnknownHostException e)
		{
			return null;
		}
		catch (IOException e)
		{
			return null;
		}//end catch
		return xml;
	}//end function getXml
}//End class SyncService
