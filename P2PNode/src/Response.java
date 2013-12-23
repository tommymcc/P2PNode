
public class Response 
{
	/*
	 * For search responses in messages.
	 * 
	 */
	
	String url;
	int rank;
	
	Response(String u, int r)
	{
		url = u;
		rank = r;
	}
	
	String GetURL()
	{
		return url;
	}
	
	int GetRank()
	{
		return rank;
	}
	
	public void increaseRank()
	{
		rank++;
	}
}
