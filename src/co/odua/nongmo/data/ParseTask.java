package co.odua.nongmo.data;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.AsyncTask;

public class ParseTask extends AsyncTask<Element, Void, DataHandler> {
	
	private ParseListener listener;
	
	public interface ParseListener
	{
		void parseComplete(DataHandler data);
	}
	
	public ParseTask(ParseListener listener)
	{
		this.listener = listener;
	}
	@Override
	protected DataHandler doInBackground(Element... params) {
		return ParseInThread(params[0]);
	}//end doInBackground
	
	@Override
	protected void onPostExecute(DataHandler result) {
		super.onPostExecute(result);
		listener.parseComplete(result);
	}//end onPostExecute
	
	public static DataHandler ParseInThread(Element xml)
	{
		DataHandler results = new DataHandler();
		try
		{
			//Category name and id
			results.setId(Integer.parseInt(xml.select("id").first().text()));
			results.setName(xml.select("name").first().text());
	
			//Get all brand elements from brands tag
			Element brandsRoot = xml.select("brands").first();
			Elements brandsTag = brandsRoot.select("brand");
			if (brandsTag == null) { results.setComplete(false); return results;}
	    	for (Element brandBlock : brandsTag)
	    	{
	    		//Get Brand name from "a" tag
	    		String _name = brandBlock.select("name").first().text();
	    		//Get Brand Logo URL from image tag
	    		String _url = brandBlock.select("url").first().text();
	    		//add data to category list
	    		results.addBrand(_name);
	    		results.addBrandImage(_name, _url);
	    		
	    		//Get Product info while in brand
	    		Element productBlock = brandBlock.select("products").first();
	    		//get all "span" tags 
	    		Elements productsTags = productBlock.select("product");
	    		List<String> _products = new ArrayList<String>();
	    		//loop though span to collect all products
	    		for (Element product : productsTags)
	    		{
	    			String _productName = product.text();
	    			//collect in list collection
	    			_products.add(_productName);
	    		}//end for
	    		//add products to hash map
	    		results.addProductMap(_name, _products);
	    	}//end for
		}//end try
    	catch (NullPointerException e)
    	{//fires if the data was corrupted
    		if (results.getName() != null)
    		{
    			results.setComplete(false);
    			return results;
    		}//end if
    		else
    			return null;
    	}//end catch
		return results;
	}
}//end class ParseTask