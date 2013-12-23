import java.util.*;

public class DataStore 
{
	/*
	 * DataStore:
	 * Keeps track of keyword - url - frequency triplets.
	 * 
	 */
	
	ArrayList<Record> data = new ArrayList<Record>();
	
	public void Add(String key, String URL)
	{
		//If the word / url combo exists, increase it's 'hit' counter
		//Otherwise add the pair
		
		boolean pairExists = false;
		for(Record i : data)
		{
			if(i.GetURL().equals(URL) && i.GetWord().equals(key))
			{
				i.Hit();
				pairExists = true;
				break;
			}
		}
		
		if(!pairExists)
		{
			Record i = new Record(key, URL);
			data.add(i);
		}
	}
	
	public Response[] Retrieve(String key)
	{
		//Retrieve a list of URLS and hits for a keyword
		ArrayList<Response> results = new ArrayList<Response>();
		
		for(Record i : data)
		{
			if(i.GetWord().equals(key))
			{
				results.add(i.ToResponse()); 
			}
		}
		
		return (Response[]) results.toArray(new Response[results.size()]);
	}
	
	
	
}
