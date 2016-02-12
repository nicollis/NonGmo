//DataHandler.java
//Handles Pulling and Saving the data from the Internet and local cached storage
package co.odua.nongmo.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class DataHandler implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	public static final String DATA_HANDLER_KEY = "data_handler_key";
	public static final String NONGMO_SHARED_PREFRENCES = "nongmo_shared_prefrences";
	private static final String DATE_LAST_SAVED_KEY = "date_last_saved_key";
	
	private static final long TIME_TO_EXPIRE = 604800000;//7 days; expires every Saturday

	public static final String CATEGORY_POSTFIX_FILE_NAME = ".gmo";
	private static final String OLD_CATEGORY_FILE_NAME = "data.gmo";//TODO REMOVE ON UPDATE
	
	private int Id; //Category Id
	private String Name;//Category Name
	private String ImageUrl;//Category image URL
	private long DateCached;//time stamp for when data was last pulled
	private ListSet<String> Brands;//List of Brand Names in Category
	private Map<String, String> BrandImages;//List of Bran Image URL's by Brand Name
	private Map<String, List<String>> ProductsMap;//key = brand; List of products in that brand
	private boolean isComplete = true;//flagged false, if something failed to parse
	
	/*
	 * TODO remove me on version 7 after everyone has upgraded to version 5/6
	 * removes old data.gmo database to limit space taken by the application
	 */
	public void removeOldDb(Context context)
	{
		//open the file, if it exists, then delete it
		File file = new File(context.getCacheDir(), OLD_CATEGORY_FILE_NAME);
		if (file.exists())
			file.delete();
	}//end method removeOldDb
	
	public DataHandler()
	{
		Brands = new ListSet<String>();
		BrandImages = new HashMap<String, String>();
		ProductsMap = new TreeMap<String, List<String>>();
		DateCached = getLastSaturday();
	}
	//get previous Saturday date in milliseconds
	private static long getLastSaturday()
	{
		Calendar calendar = Calendar.getInstance();
		//set to upcoming Saturday
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		//converts that date to milliseconds
		long dateInMilliseconds = calendar.getTimeInMillis();
		//roll back to the previous Saturday and return the value
		return (dateInMilliseconds - TIME_TO_EXPIRE);
	}
	
	public DataHandler(String name, int id, String imageUrl)
	{
		this.Name = name; this.Id = id; 
		this.ImageUrl = imageUrl;;
	}
	
	public String toString()
	{
		return Name;
	}

	public void addBrand(String name)
	{
		if (!Brands.contains(name))
				Brands.add(name);
	}//end method addBrand
	public ListSet<String> getBrands()
	{
		return Brands;
	}
	
	public void addProductMap(String brand, List<String> products)
	{
		if (!ProductsMap.containsKey(brand))
			ProductsMap.put(brand, products);
		else
		{//brand already exist
			List<String> temp = ProductsMap.get(brand);
			//add products from list if they don't already exist
			for (String product : products)
			{
				if(!temp.contains(product))
					temp.add(product);
			}//end for
			//put new list into ProductMap
			ProductsMap.put(brand, temp);
		}//end else
	}//end method addProductMap
	public List<String> getProductsList(String brand)
	{
		return ProductsMap.get(brand);
	}
	
	public void addBrandImage(String name, String url)
	{
		BrandImages.put(name, url);
	}
	
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public int getId() {
		return Id;
	}
	public void setId(int id) {
		Id = id;
	}
	public String getImageUrl() {
		return ImageUrl;
	}
	public void setImageUrl(String imageUrl) {
		ImageUrl = imageUrl;
	}
	public long getDateCached() {
		return DateCached;
	}

	public void setDateCached(long dateCached) {
		DateCached = dateCached;
	}
	
	public boolean isComplete() {
		return isComplete;
	}

	public void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}

	public boolean hasExpired()
	{	//if cached has expired return true;
		if ((DateCached + TIME_TO_EXPIRE) <= System.currentTimeMillis())
			return true;
		return false;
	}
	
	public static boolean hasExpired(long whenLastPulled)
	{
		if ((whenLastPulled + TIME_TO_EXPIRE) <= System.currentTimeMillis())
			return true;
		return false;
	}
	
	//Gets when Loading screen/background updater pulled data last
	public static long getMasterDateCached(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(NONGMO_SHARED_PREFRENCES, Context.MODE_PRIVATE);
		return sp.getLong(DATE_LAST_SAVED_KEY, 0);
	}//end method getMasterDateCached
	
	//Saves current time as when Loading screen/background updater pulled data last
	public static void saveMasterDateCached(Context context)
	{
		long timeToSave = getLastSaturday();
		Editor editor = context.getSharedPreferences(NONGMO_SHARED_PREFRENCES, Context.MODE_PRIVATE).edit();
		editor.putLong(DATE_LAST_SAVED_KEY, timeToSave);
		editor.commit();
	}//end method savemasterDateCached
	
	public static String getFileName(String categoryName)
	{
		String fileName = categoryName.replaceAll("[^A-Za-z]+", "") + CATEGORY_POSTFIX_FILE_NAME;
		return fileName;
	}//end method getFileName

	//Loops though list of categories and saves each on of them, returns false if one false to save
	public static boolean saveCategories(Context context, Map<String, DataHandler> categoryMap)
    {	
    	boolean results = true;
    	
    	for (DataHandler category : categoryMap.values())
    	{
    		boolean _results = saveCategory(context, category);
    		if (!_results)
    			results = _results;
    	}//end for
    	return results;
    }//end save categories
	
	//saves individual categories
	public static boolean saveCategory(Context context, DataHandler category)
	{
		//create a file in the cache directory
    	File f = new File(context.getCacheDir(), getFileName(category.Name));
    	try
    	{
    		//open a file stream to the created file
    		FileOutputStream fos = new FileOutputStream(f);
    		//link an object stream to file stream to save categories to it.
    		ObjectOutputStream oos = new ObjectOutputStream(fos);
    		oos.writeObject(category);//saves category to the object
    		//close the stream when done
    		oos.close();
    		fos.close();
    		
    	}//end try
    	catch(FileNotFoundException e)
    	{
    		Log.e("SAVE_CATEGORIES FNFE", e.getLocalizedMessage());
    		return false;
    	}//end catch
    	catch (IOException e) {
    		Log.e("SAVE_CATEGORIES IOE", e.getLocalizedMessage());
    		return false;
		}//end catch
    	return true;
	}
	
	/**
	 * Loads category data from cache in the CURRENT THREAD
	 * Make sure not to be doing this on the UI thread
	 * @param context
	 * @param CatName
	 * @return
	 */
	public static DataHandler loadCategoryInThread(Context context, String CatName)
	{
		DataHandler categoryData = null;
		
		//Create/Open a file in the cache directory
		File _file = new File(context.getCacheDir(), DataHandler.getFileName(CatName));
		//try to pull data, cache any exceptions
		try
		{
			//open a fileStream to the opened file
			FileInputStream fileInputStream  = new FileInputStream(_file);
			//create a ObjectInputStream to pull data types  from the FileInputStream
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
			//read objects and save to "category" object
			categoryData = (DataHandler) objectInputStream.readObject();
			//close steams to free memory
			objectInputStream.close();
			fileInputStream.close();
		}//end try
		catch(FileNotFoundException e)
    	{
    		Log.d("LOAD_CATEGORIES FNFE", e.getLocalizedMessage());
    		return null;
    	}//end catch
    	catch (NotSerializableException e) {
    		Log.e("LOAD_CATEGORIES NSE", e.getLocalizedMessage());
    		return null;
		}//end catch
    	catch (ClassNotFoundException e) {
    		Log.e("LOAD_CATEGORIES CNFE", e.getLocalizedMessage());
    		return null;
		} catch (StreamCorruptedException e) {
			Log.e("LOAD_CATEGORIES SCE", e.getLocalizedMessage());
			return null;
		} catch (IOException e) {
			Log.e("LOAD_CATEGORIES IOE", e.getLocalizedMessage());
			e.printStackTrace();
    		return null;
		}
		return categoryData;
	}//end static method loadCategoryInThread
	
	//method for parsing the HTML into a DataHandler
	public static DataHandler parseXml(Element xml)
	{
		DataHandler results = new DataHandler();
		
		//Category name and id
		results.setId(Integer.parseInt(xml.select("id").first().text()));
		results.setName(xml.select("name").first().text());
		
		//Get all brand elements from brands tag
		Element test = xml.select("brands").first();
		Elements brandsTag = test.select("brand");
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
		return results;
	}//end method parseHtml
}
