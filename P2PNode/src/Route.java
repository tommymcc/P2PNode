import java.net.InetAddress;
import java.net.UnknownHostException;


public class Route 
{
	//Class to store a path to a particular node, used by the routing table
	
	long node_id;
	String ip_address;
	int port;
	
	Route(long nID, String IP, int prt)
	{
		node_id = nID;
		ip_address = IP;
		port = prt;
	}
	
	long GetNodeID()
	{
		return node_id;
	}
	
	String GetNodeIP()
	{
		return ip_address;
	}
	
	int GetNodePort()
	{
		return port;
	}
	
	public InetAddress GetInetAddress()
	{
		InetAddress a = null;
		try 
		{
			a = InetAddress.getByName(ip_address);
		} 
		catch (UnknownHostException e) 
		{
			e.printStackTrace();
		}
		return a;
	}
}
