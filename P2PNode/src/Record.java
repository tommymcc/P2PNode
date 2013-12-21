
public class Record 
{
	/* Local storage class for results */
	
	String word;
	String url;
	int hits;
	
	Record(String w, String u)
	{
		word = w;
		url = u;
		hits = 1;
	}
	
	void Hit()
	{
		hits++;
	}
	
	String GetURL()
	{
		return url;
	}
	
	String GetWord()
	{
		return word;
	}
	
	int GetRank()
	{
		return hits;
	}
	
	public void SetRank(int r)
	{
		hits = r;
	}

	public Response ToResponse()
	{
		Response r = new Response(url, hits);
		return r;
	}
}
