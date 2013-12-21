
public class Index extends Message
{
	
	/*
	 {
	    "type": "INDEX", //string
	    "target_id": "34", //the target id
	    "sender_id": "34", // a non-negative number of order 2'^32^', of the message originator
	    "keyword": "XXX", //the word being indexed
	    "link": [
	               "http://www.newindex.com", // the url the word is found in
	               "http://www.xyz.com"
	              ]
	}
	 */
	
	long target_id, sender_id;
	String keyword;
	String link[];
	
	Index(long target, long sender, String key, String links[] )
	{
		super();
		type = "INDEX";
		target_id = target;
		sender_id = sender;
		keyword = key;
		link = links;
	}
	
	String GetKeyword()
	{
		return keyword;
	}

	String[] GetLinks()
	{
		return link;
	}
	
	long GetTargetID()
	{
		return target_id;
	}
	
	long GetSenderID()
	{
		return sender_id;
	}
}
