
public class JoiningNetworkRelay extends Message
{
	
	/*
	{
	    "type": "JOINING_NETWORK_RELAY_SIMPLIFIED", // a string
	    "node_id": "42", // a non-negative number of order 2'^32^', indicating the id of the joining node).
	    "target_id": "42", // a non-negative number of order 2'^32^', indicating the target node for this message.
	    "gateway_id": "34", // a non-negative number of order 2'^32^', of the gateway node
	}
	*/
	
	//
	
	long node_id, target_id, gateway_id;
	
	JoiningNetworkRelay(long nID, long tID, long gID)
	{
		super();
		type = "JOINING_NETWORK_RELAY_SIMPLIFIED";
		node_id = nID;
		target_id = tID;
		gateway_id = gID;
	}
	
	void SetNodeID(long n)
	{
		node_id = n;
	}
	
	long GetNodeID()
	{
		return node_id;
	}
	
	void SetTargetID(long t)
	{
		target_id = t;
	}
	
	long GetTargetID()
	{
		return target_id;
	}
	
	void SetGatewayID(long g)
	{
		gateway_id = g;
	}
	
	long GetGatewayID()
	{
		return gateway_id;
	}
}
