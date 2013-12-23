import java.util.HashMap;
import java.util.Map;


public class RoutingTable 
{
	/*
	 * No limit on number of Routes as of yet, could refine system to purge 'old' records, etc
	 * 
	 * Maybe limited number of Routes with LRU for replacement algorithm
	 * 
	 * Different Maps for different localities?
	 * 
	 */
	
	//Maps each nodeID to an IP address and Port
	HashMap<Long , Route > Routes = new HashMap<Long, Route >();
	
	public void UpdateRoute(long NodeID, String IP, int port)
	{
		if(!RouteExists(NodeID))
		{
			//If this node doesn't have a Route associated with it's ID
			//Create a new Route and add it to the RoutingTable
			Route r = new Route(NodeID, IP, port);
			Routes.put(new Long(NodeID), r); //Add to the table
			
		}
		else
		{
			//Should we update the listing or is there a possibility the message is malicious?
			//Or even which is more up to date?
			//Could have a 'last seen' field in routing info
		}
	}
	
	public boolean RouteExists(long NodeID)
	{
		if(Routes.containsKey(new Long(NodeID)))
		{
			return true;
		}
		
		return false;
	}
	
	public Route GetRoute(long NodeID)
	{
		return Routes.get(new Long(NodeID));
	}
	
	public void RemoveRoute(long NodeID)
	{
		Routes.remove(new Long(NodeID));
	}

	public void print()
	{
		System.out.println("/*Routing Table*/");
		
		for (Map.Entry entry : Routes.entrySet()) 
		{
			Route r = (Route)entry.getValue();
		    System.out.println("\nNode " + r.GetNodeID() + " with IP " + r.GetNodeIP());
		    System.out.println("and port " + r.GetNodePort() + "\n-");
		}
	}
	
	public Route ClosestNodeToID(long id)
	{
		Route closest = null;
		
		System.out.println("Calculating Closest Node to " + id);
		
		closest = Routes.get(id);
		if(closest != null)
		{
			//If this node is in the routing table, return it
			System.out.println("Found Target Node In RT");
			return closest;
		}
		
		//Find the closest node
		
		for (Map.Entry entry : Routes.entrySet()) 
		{
			if(closest == null)
			{
				//Default value
				closest = (Route)entry.getValue();
				continue;
			}
			
			Route r = (Route)entry.getValue();
			
			if(Math.abs(r.GetNodeID()-id) < Math.abs(closest.GetNodeID()-id))
			{
				//If this node is closer to the requested id
				closest = r;
			}
		}
		
		if(closest != null)
		{
			//If rt wasn't empty
			//System.out.println("Closest Found:" + closest.GetNodeID());
		}
		else
		{
			//System.out.println("No Entries in RT");
		}
		
		return closest;
	}
	
	public Route[] GetRoutingInfo()
	{
		Route result[] = new Route[Routes.size()];
		result = (Route[]) Routes.values().toArray(new Route[Routes.size()]);
		return result;
	}
	
	public void MergeRoutingInfo(Route[] NewRoutingInfo)
	{
		// Add new listings to our routing table
		for(int i = 0; i < NewRoutingInfo.length; i++)
		{
			Route r = NewRoutingInfo[i];
			UpdateRoute(r.GetNodeID(), r.GetNodeIP(), r.GetNodePort());
		}
	}
	
	public Route IPLookup(String IP)
	{
		//Lookup a route for a certain IP address.
		for (Map.Entry entry : Routes.entrySet()) 
		{
			Route r = (Route)entry.getValue();
			if(r.GetNodeIP().equals(IP))
			{
				return r;
			}
		}
		return null;
	}
}
