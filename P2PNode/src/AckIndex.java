
public class AckIndex extends Message
{
	
	/*
	 {
	    "type": "ACK_INDEX", // a string
	    "node_id": "23", // a non-negative number of order 2'^32^', identifying the target node.
	    "keyword": "fish" // the keyword from the original INDEX message
	 }
	 */
	
	long node_id;
	String keyword;
	
	public AckIndex(long nID, String key)
	{
		super();
		type = "ACK_INDEX";
		node_id = nID;
		keyword = key;
	}
	
	public long GetNodeID()
	{
		return node_id;
	}
	
	public String GetKeyword()
	{
		return keyword;
	}
}
