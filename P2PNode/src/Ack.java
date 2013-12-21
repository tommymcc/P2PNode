
public class Ack extends Message 
{
	/*
	 {
	    "type": "ACK", // a string
	    "node_id": "23", // a non-negative number of order 2'^32^', identifying the suspected dead node.
	    "ip_address": "199.1.5.4" // the ip address of  sending node, this changes on each hop (or used to hold the keyword in an ACK message returned following an INDEX message)
	 }
	 */
	
	long node_id;
	String ip_address;

	public Ack(long nID, String IP)
	{
		super();
		type = "ACK";
		node_id = nID;
		ip_address = IP;
	}
	
	public long GetNodeID()
	{
		return node_id;
	}
	
	public String GetIPAddress()
	{
		return ip_address;
	}
	
	public void SetIPAddress(String IP)
	{
		ip_address = IP;
	}
}
