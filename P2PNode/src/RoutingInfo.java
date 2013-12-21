
public class RoutingInfo extends Message
{
	
	/*
	 * 
	 {
	    "type": "ROUTING_INFO", // a string
	    "gateway_id": "34", // a non-negative number of order 2'^32^', of the gateway node
	    "node_id": "42", // a non-negative number of order 2'^32^', indicating the target node (and also the id of the joining node).
	    "ip_address": "199.1.5.2" // the ip address of the node sending the routing information
	    "route_table":
	    [
	        {
	            "node_id": "3", // a non-negative number of order 2'^32^'.
	            "ip_address": "199.1.5.3" // the ip address of node 3
	        },
	        {
	            "node_id": "22", // a non-negative number of order 2'^32^'.
	            "ip_address": "199.1.5.4" // the ip address of  node 22
	        }
	    ]
	}
	 *
	 */
	
	long gateway_id, node_id;
	String ip_address;
	Route route_table[];
	
	RoutingInfo(long gID, long nID, String IPAddress)
	{
		super();
		type = "ROUTING_INFO";
		gateway_id = gID;
		node_id = nID;
		ip_address = IPAddress;
	}
	
	public void AddRoutes(Route r[])
	{
		route_table = r;
	}
	
	public Route[] GetRoutes()
	{
		return route_table;
	}
	
	public long GetGatewayID()
	{
		return gateway_id;
	}
	
	public long GetNodeID()
	{
		return node_id;
	}
	
	public String GetIPAddress()
	{
		return ip_address;
	}
}
