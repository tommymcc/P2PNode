import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import sun.misc.Lock;


public class AggregatedSearchResult 
{

	/* 
	 * Keeps track of a search request which may include multiple keywords.
	 * 
	 * At the moment, we track nodes instead of keywords to determine which result messages apply to each 
	 * AggregatedSearchResult. This could be changed easily and should be if the network ever uses more
	 * than one keyword per node id.
	 * 
	 * A timer is used to mark the result as finished 3 seconds after the request is created. Control is then
	 * returned to the calling program along with the results.
	 * 
	 * 
	 */
	
	ArrayList<Record> results;
	ArrayList<Long> dependentNodes;
	boolean hasDisplayed = false;
	long startTime;
	boolean isFinished = false;
	Object lock;
	
	public AggregatedSearchResult()
	{
		results = new ArrayList<Record>();
		dependentNodes = new ArrayList<Long>();
		lock = new Lock();
		
		//Search is deemed complete after 3 seconds
		Timer timer = new Timer();
		timer.schedule( 
			new TimerTask()
			{ 
				public void run() 
				{ 
					synchronized(lock)
					{
						isFinished=true; 
						lock.notifyAll();
					}
				} 
			}, 3000);
		
		//Keep a record of the task's initialization time
		startTime = (System.currentTimeMillis() / 1000);
	}
	
	public void AddRecord(Record r)
	{
		//Add on-the-fly sorting of records
		results.add(r);
	}
	
	public void AddSearchResponse(SearchResponse sr)
	{
		//Add a response to the result set
		Response response[] = sr.GetResponse();
		dependentNodes.remove((Long)sr.GetSenderID());
		
		for(Response r : response)
		{
			//Add to results
			Record rec = new Record(sr.GetWord(), r.GetURL());
			rec.SetRank(r.GetRank());
			AddRecord(rec);
		}
		
	}
	
	public boolean IsComplete()
	{
		if(dependentNodes.isEmpty())
		{
			return true;
		}
		return false;
	}
	
	public void AddDependentNode(long nID)
	{
		dependentNodes.add(new Long(nID));
	}
	
	public boolean IsDependentOn(long nID)
	{
		if(dependentNodes.contains(new Long(nID)))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public Long[] GetDependentNodes()
	{
		return dependentNodes.toArray(new Long[dependentNodes.size()]);
	}
	
	public void printResults()
	{
		System.out.println("The following results were received:");
		if(IsComplete()){System.out.println("(Complete Set Received)");}
		for(Record r : results)
		{
			System.out.println("Word:'" + r.GetWord() + "' URL: '" + r.GetURL() + "' Rank: " + r.GetRank());
		}
		hasDisplayed = true;
	}
	
	public boolean HasDisplayed()
	{
		return hasDisplayed;
	}
	
	public long GetStartTime()
	{
		return startTime;
	}
	
	public boolean IsFinished()
	{
		return isFinished;
	}
	
	public SearchResult[] GetSearchResults()
	{
		//Return results as an array of SearchResult Objects
		SearchResult sr[] = new SearchResult[results.size()];
		int i = 0;
		
		for(Record r : results)
		{
			String url[] = new String[1];
			url[0] = r.GetURL();
			sr[i] = new SearchResult(r.GetWord(), url , r.GetRank());
			i++;
		}
		return sr;
	}
	
	public Object GetLock()
	{
		return lock;
	}
}
