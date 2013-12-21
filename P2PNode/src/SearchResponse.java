
public class SearchResponse extends Message
{

	/*
	 * 
	 {
	    "type": "SEARCH_RESPONSE",
	    "word": "word", // The word to search for
	    "node_id": "45",  // target node id
	    "sender_id": "34", // a non-negative number of order 2'^32^', of this message originator
	    "response":
	    [
	        {
	            url: "www.dsg.cs.tcd.ie/",  //url
	            rank: "32"  //rank
	        },
	        {
	             url: "www.scss.tcd.ie/courses/mscnds/",  //url
	             rank: "1" //rank
	        }
	    ]
	}

	 */
	
	String word;
	long node_id, sender_id;
	Response response[];
	
	SearchResponse(String w, long nID, long sID, Response r[])
	{
		super();
		type = "SEARCH_RESPONSE";
		word = w;
		node_id = nID;
		sender_id = sID;
		response = r;
	}
	
	public String GetWord()
	{
		return word;
	}
	
	public long GetNodeID()
	{
		return node_id;
	}
	
	public long GetSenderID()
	{
		return sender_id;
	}
	
	public Response[] GetResponse()
	{
		return response;
	}
}
