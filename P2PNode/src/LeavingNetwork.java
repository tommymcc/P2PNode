
public class LeavingNetwork extends Message
{
	/*
     {
	    "type": "LEAVING_NETWORK", // a string
	    "node_id": "42", // a non-negative number of order 2'^32^' identifying the leaving node.
	 }
	 */
	
	long node_id;
	
	LeavingNetwork(long n)
	{
		super();
		node_id = n;
	}
	
	long GetNodeID()
	{
		return node_id;
	}
}
