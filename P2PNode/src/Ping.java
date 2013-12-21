
public class Ping extends Message
{
	/*
	{
	    "type": "PING", // a string
	    "target_id": "23", // a non-negative number of order 2'^32^', identifying the suspected dead node.
	    "sender_id": "56", // a non-negative number of order 2'^32^', identifying the originator
	                               //    of the ping (does not change)
	   "ip_address": "199.1.5.4" // the ip address of  node sending the message (changes each hop)
	}
	*/
	
	long target_id, sender_id;
	String ip_address;
	
	Ping(long tID, long sID, String IP)
	{
		super();
		type = "PING";
		target_id = tID;
		sender_id = sID;
		ip_address = IP;
	}
	
	public long GetTargetID()
	{
		return target_id;
	}
	
	public long GetSenderID()
	{
		return sender_id;
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
