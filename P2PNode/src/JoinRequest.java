

public class JoinRequest extends Message
{
	/*
	 * {
		    "type": "JOINING_NETWORK_SIMPLIFIED", // a string
		    "node_id": "42", // a non-negative number of order 2'^32^', indicating the id of the joining node).
		    "target_id": "42", // a non-negative number of order 2'^32^', indicating the target node 
		    				   // for this message.
		    "ip_address": "199.1.5.2" // the ip address of the joining node
		}
	 */
	
	long node_id, target_id;
	String ip_address;
	
	JoinRequest()
	{
		super();
		type = "JOINING_NETWORK_SIMPLIFIED";
	}
	
	void SetNodeID(long n)
	{
		node_id = n;
	}
	
	long GetNodeID()
	{
		return node_id;
	}
	
	void SetTargetID(long n)
	{
		target_id = n;
	}
	
	long GetTargetID()
	{
		return target_id;
	}
	
	void SetIPAddress(String i)
	{
		ip_address = i;
	}
	
	String GetIPAddress()
	{
		return ip_address;
	}
	
}
