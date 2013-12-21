import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class AggregatedSearchResult 
{

	/* Needs improvement 
	 * -Keyword tracking, etci
	 */
	
	ArrayList<Record> results;
	ArrayList<Long> dependentNodes;
	boolean hasDisplayed = false;
	long startTime;
	
	public AggregatedSearchResult()
	{
		results = new ArrayList<Record>();
		dependentNodes = new ArrayList<Long>();
		
		//Display Results after 3 seconds
		Timer timer = new Timer();
		timer.schedule( new TimerTask(){ public void run() { printResults(); } }, 3000);
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
}
