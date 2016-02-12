//ListSet.java
//Extends TreeSet as to make a more reliable "toArray" method
//that was throwing a "ClassCastExecption" when casting to a String[]
package co.odua.nongmo.data;

import java.util.Iterator;
import java.util.TreeSet;

public class ListSet<E> extends TreeSet<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8956L;
	
	public ListSet(){}
	
	@SuppressWarnings("unchecked")
	public ListSet(Object[] data) 
	{
		for (Object obj : data)
		{
			this.add((E) obj);
		}//end for
	}//end ListSet
	
	/**
	 * Overrides the built in toArray last to return 
	 * contents list in an array format
	 */
	@Override
	public String[] toArray() 
	{
		//if empty then return null
		if(this.isEmpty())
			return null;
		//Initialize array with object size
		String[] array = new String[this.size()];
		int count = 0;
		Iterator<?> iterator = this.iterator();
		//do while to insure last object is added
		while(iterator.hasNext())
		{
			array[count] = iterator.next().toString();
			count++;
		}
		//return new array
		return array;
	}//end method toArray

}
