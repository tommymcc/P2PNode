
public class Search extends Message
{
	
	/*
	 {
	    "type": "SEARCH", // string
	    "word": "apple", // The word to search for
	    "node_id": "34",  // target node id
	    "sender_id": "34", // a non-negative number of order 2'^32^', of this message originator
	 }
	 */
	
	String word;
	long node_id, sender_id;
	
	
	Search(String w, long nID, long sID)
	{
		super();
		type = "SEARCH";
		word = w;
		node_id = nID;
		sender_id = sID;
	}
	
	String GetWord()
	{
		return word;
	}
	
	long GetNodeID()
	{
		return node_id;
	}
	
	long GetSenderID()
	{
		return sender_id;
	}
	
	

}
