import java.io.IOException;
import java.net.*;
import java.util.*;
import com.google.gson.*;

/*
 * PeerSearch.java
 * 
 * Implements the simplified version of the P2P protocol laid out at:
 * 
 * https://www.scss.tcd.ie/~ebarrett/Teaching/CS4032/wiki/index.php?n=Main.P2PWebSearchArchitectureSpecification-Official
 * 
 * Adheres to the class interface specified.
 * 
 * Data received for indexing is kept in volatile memory and does not persist beyond the execution of the program.
 * It would be a simple matter to store JSON representations of these records on the local hard drive.
 * 
 * Utilises multithreading to prevent blocking on sockets while the program runs.
 * 
 * Index requests are tracked until they succeed. Nothing is done with them yet if they remain unconfirmed.
 * 
 * The Routing Table is simple at the moment. All routes are retained until communication is lost to the respective node.
 * This is fine for small networks but we may want to consider limiting the number of routes permitted in order to prevent
 * performance hits from large lists of nodes.
 * 
 * Pruning has been tested to work as described. A nodes routing table may be checked using the Print method.
 * 
 * A factor to consider is locality. It may be useful for example to keep another smaller list of nodes 'close' in 
 * the identifier space.
 * 
 * A useful feature to build into the network would be facilitation of direct connections between hosts behind routers,
 * using NAT traversal techniques.
 * 
 * 
 */


/* To Do 
 * 
 * - Coding Tasks -
 * Ensure the Routing Table uses 'Prefix Routing' and generally optimize
 * 
 * - Error Handling -
 * Add better error checking and useful error messages
 * 
 */


//For the simplified routing solution
interface PeerSearchSimplified 
{
    void init(DatagramSocket udp_socket); // Initialize with a UDP socket
    
    long joinNetwork(InetSocketAddress bootstrap_node, String identifier, String target_identifier ); 
    //Returns network_id, a locally
    //generated number to identify peer network
    
    boolean leaveNetwork(long network_id); // parameter is previously returned peer network identifier
    void indexPage(String url, String[] unique_words);
    SearchResult[] search(String[] words);
}

public class PeerSearch extends Thread implements PeerSearchSimplified
{
	DataStore mainStore = new DataStore();
	RoutingTable rt = new RoutingTable();
	
	//Searches initiated by this node
	ArrayList<AggregatedSearchResult> ActiveSearches = new ArrayList<AggregatedSearchResult>(); 
	
	//Watch-lists for possible dead nodes
	HashMap<Long,Long> UnacknowledgedList;
	HashMap<Long,Long> PingedList;
	
	//List of Index requests
	ArrayList<Transaction> PendingTransactions; 
	
	DatagramSocket socket;
	Thread thread;
	Gson gson;
	long node_id;
	boolean IsBootstrap = false;
	String identifier;
	
	public void init(DatagramSocket udp_socket)
	{
		//Initialize with a UDP socket and start listening thread
		socket = udp_socket;
		gson = new Gson(); //Handles JSON parsing
		
		//These Lists keep track of potential dead nodes
		UnacknowledgedList = new HashMap<Long,Long>();
		PingedList = new HashMap<Long,Long>();
		
		//Keeps track of unconfirmed index requests
		PendingTransactions = new ArrayList<Transaction>(); 
		
		//Start new Thread
		start(); 
		
		//Start Pruning network after 25 seconds
		Timer timer = new Timer();
		timer.schedule( new TimerTask(){ public void run() { PruneNetwork(); } }, 25000); 
	}
	
    public long joinNetwork(InetSocketAddress bootstrap_node, String id, String target_identifier )
    {
    	//Returns network_id, a locally generated number to identify peer network
    	
    	node_id = hashCode(id);
    	identifier = id;
    	
    	if(!IsBootstrap)
    	{
    		//If this node is joining an existing network and not bootstrapping a new one
	    	JoinRequest j = new JoinRequest();
	    	j.SetTargetID(hashCode(target_identifier));
	    	j.SetNodeID(node_id);
	    	j.SetIPAddress(GetIPAddress());
	    	
	    	String requestJSON = gson.toJson(j);
	    	
	    	byte packetBytes[] = requestJSON.getBytes();
	    	DatagramPacket requestPacket;
	    	
	    	try 
	    	{
	    		//Send the join request to the bootstrap node
	    		requestPacket = new DatagramPacket(packetBytes, packetBytes.length, bootstrap_node.getAddress(), bootstrap_node.getPort());
	    		socket.send(requestPacket);
	    	}
	    	catch (UnknownHostException e) 
			{
				e.printStackTrace();
			} 
	    	catch (IOException e) 
			{
				e.printStackTrace();
			}
	    	
	    	//If request to join was successful, add the bootstrap node to our RoutingTable
	    	//IMPORTANT: target_identifier must be id of the bootstrap or our routing table will be wrong
	    	rt.UpdateRoute(hashCode(target_identifier), bootstrap_node.getAddress().getHostAddress(), bootstrap_node.getPort());
	    	
    	}
        
    	return node_id;
    }
                                       
    public boolean leaveNetwork(long network_id)
    {
    	//A LeavingNetwork message is sent to all peers in the routing table
    	
    	LeavingNetwork ln = new LeavingNetwork(network_id);
    	String lnJSON = gson.toJson(ln);
    	
    	//Send message to all the nodes this node is connected to
    	Route otherNodes[] = rt.GetRoutingInfo();
    	
    	for(Route r : otherNodes)
    	{
    		//Send to each node
    		SendByRoute(lnJSON, r);
    	}
    	return true;
    }
    
    public void indexPage(String url, String[] unique_words)
    {
    	/*
    	 * Indexes a URL and a set of words found on the page
    	 * 
    	 */
    	
    	//Format expected is array of urls
    	String urls[] = {url};
    	
    	for(String word : unique_words)
    	{
    		//Create an Index message, convert to JSON and send to target node
    		Index idx = new Index(hashCode(word), node_id, word, urls);
    		Route r = rt.ClosestNodeToID(idx.GetTargetID());
			SendByRoute(gson.toJson(idx), r);
			
			//Keep a record of the transaction
			Transaction trans = new Transaction(idx, GetTime());
			PendingTransactions.add(trans);
			UnacknowledgedList.put(new Long(trans.GetIndex().GetTargetID()), new Long(trans.GetTransactionTime()));
    	}
    	
    }
    
    public SearchResult[] search(String[] words)
    {
    	/*
    	 * Gets search results from the network for the given keywords
    	 * 
    	 * Note: 
    	 * 3 second delay when called before returning to allow receipt of results
    	 * 
    	 */
    	
    	AggregatedSearchResult ASR = new AggregatedSearchResult();
    	
    	for(String word : words)
    	{
    		//Create a new search request and send it over the network
    		Search s = new Search(word, hashCode(word), node_id);
    		Route r = rt.ClosestNodeToID(hashCode(word));
    		UnacknowledgedList.put(new Long(r.GetNodeID()), new Long(GetTime()));
			SendByRoute(gson.toJson(s), r);
			
			//So we know which hosts this query relies on
			ASR.AddDependentNode(hashCode(word));
    	}
    	
    	ActiveSearches.add(ASR);
    	
    	System.out.println("Node:" + identifier);
    	System.out.println("Sent Search Request(s)");
    	
    	//Wait for search results
    	try 
    	{
    		synchronized(ASR.GetLock())
    		{
	    		while(!ASR.IsFinished())
	    		{
	    			ASR.GetLock().wait();
	    		}
    		}
		} 
    	catch (InterruptedException e) 
    	{
			e.printStackTrace();
		}
    	
    	return ASR.GetSearchResults();
    }
    
    public int hashCode(String str) 
    {
    	//Generates NodeIDs, word hashes, etc.
  	  	int hash = 0;
  	  	for (int i = 0; i < str.length(); i++) 
  	  	{
  	  		hash = hash * 31 + str.charAt(i);
  	  	}
  	  	return Math.abs(hash);
  	}
    
    public void run()
    {
    	/* 
    	 * Listens for packets and forwards them to a processing function
    	 * Basically just a receive loop in it's own thread to allow normal
    	 * program function without having to wait for a new message.
    	 * 
    	 */

    	for(;;)
    	{
        	byte[] data = new byte[1024];
        	
            DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
        	try 
        	{
        		//Receive input and handle it appropriately
				socket.receive(receivedPacket);
				HandleMessage(receivedPacket);
			} 
        	catch (IOException e) 
			{
        		//Malformed packet, go on to the next one
        		continue;
				//e.printStackTrace();
			}
    	}
    }
    
    public void HandleMessage(DatagramPacket receivedPacket)
    {
    	/*
    	 * This function processes a message which matches any of the specified message types.
    	 * 
    	 */
    	
    	String str;
    	str = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
    	
    	//Convert to generic Message Object to determine Message type
    	Message m = gson.fromJson(str, Message.class);
    	if(m == null)
    	{
    		//If what we received wasn't a message, discard it
    		return;
    	}
    	
    	//Handle Each Message Type Here
    	
    	switch(m.type)
    	{
    		case "JOINING_NETWORK_SIMPLIFIED":
    			
    			//Send routing table info here
    			
    			//Update our own routing info
    			JoinRequest j = gson.fromJson(str, JoinRequest.class);
    			rt.UpdateRoute(j.GetNodeID(), j.GetIPAddress(), receivedPacket.getPort());

    			//Show updated routing table
    			//rt.print();
    			
    			//Send this nodes routing table
    			
    			RoutingInfo ri = new RoutingInfo(node_id, j.GetNodeID(), GetIPAddress());
    			ri.AddRoutes((Route[])rt.GetRoutingInfo());
    			String RoutingInfoJSON = gson.toJson(ri);
    			
    			SendByRoute(RoutingInfoJSON, rt.ClosestNodeToID(j.GetNodeID()));
    			
    		 	break;
    		case "JOINING_NETWORK_RELAY_SIMPLIFIED":	
    			
    			/* Working */
    			
    			//Received a Relay request from joining node or other relay
    			JoiningNetworkRelay jnr = gson.fromJson(str, JoiningNetworkRelay.class);
    			
    			//Create RoutingInfo representation of our RoutingTable
    			ri = new RoutingInfo(jnr.GetGatewayID(), jnr.GetNodeID(), GetIPAddress());
    			ri.AddRoutes((Route[])rt.GetRoutingInfo());
    			
    			//Convert RoutingInfo to JSON
    			RoutingInfoJSON = gson.toJson(ri);
  
    			//Routing Info Going 'backwards' towards gateway / joining node
    			if(jnr.GetGatewayID() == node_id)
    			{
    				//If this node is the gateway, forward routing info directly to joining node
    				SendByRoute(RoutingInfoJSON, rt.ClosestNodeToID(jnr.GetNodeID()));
    			}
    			
    			if(jnr.GetTargetID() != node_id)
    			{
    				//If this is not the destination, relay JNR onto next node
    				Route r = rt.ClosestNodeToID(jnr.GetTargetID());
    				SendByRoute(str, r);
    			}
    			
		 			break;
    		case "ROUTING_INFO":
    			
    			/* Working */
    			
    			//If we receive a RoutingInfo Message
    			RoutingInfo rInfo = gson.fromJson(str, RoutingInfo.class);
    			
    			//Add routing info to our routing table
    			rt.MergeRoutingInfo(rInfo.GetRoutes());

    			//rt.print();
    			
    			if(rInfo.GetNodeID() != node_id)
    			{
    				//If this is not the target node, construct new updated RoutingInfo packet
    				
    				//Create a new RoutingInfo object to contain merged and updated routing tables
        			ri = new RoutingInfo(rInfo.GetGatewayID(), rInfo.GetNodeID(), GetIPAddress());
        			ri.AddRoutes(rt.GetRoutingInfo());
        			
        			//Convert Routing Info to JSON
        			RoutingInfoJSON = gson.toJson(ri);
    				
    				//If this is not the destination, relay onto next node
    				if(rInfo.GetGatewayID() == node_id)
    				{
    					//If this node is the Gateway, relay directly to node_id
    					Route r = rt.ClosestNodeToID(rInfo.GetNodeID());
        				SendByRoute(str, r);
    				}
    				else
    				{
    					//Relay to Gateway
    					Route r = rt.ClosestNodeToID(rInfo.GetGatewayID());
        				SendByRoute(str, r);
    				}
    			}
    			
		 			break;
    		case "LEAVING_NETWORK":	

    			//A host is leaving the network and must be removed from the routing table
    			LeavingNetwork ln = gson.fromJson(str, LeavingNetwork.class);
    			rt.RemoveRoute(ln.GetNodeID());
    			//rt.print();
    			
		 			break;
    		case "INDEX":	

    			//We received an indexing request
    			Index idx = gson.fromJson(str, Index.class);
    			
    			if(node_id == idx.GetTargetID())
    			{
    				//If this is the target node, add links to DataStore
    				
    				String links[] = idx.GetLinks();
    				for(String l : links)
    				{
    					mainStore.Add(idx.GetKeyword(), l);
    				}
    				
    				//Acknowledge here
    				Ack ack = new Ack(idx.GetSenderID(), GetIPAddress());
    				Route r = rt.ClosestNodeToID(idx.GetSenderID());
    				SendByRoute(gson.toJson(ack), r);
    			}
    			else
    			{
    				//Pass on the message
    				Route r = rt.ClosestNodeToID(idx.GetTargetID());
    				SendByRoute(str, r);
    			}
    			
		 			break;
    		case "SEARCH":	
    			
    			/* We've received a search request */
    			
    			Search search = gson.fromJson(str, Search.class);
    			
    			if(search.GetNodeID() == node_id)
    			{
    				//If this node has the data, package it up and send it to the requesting node
    				
    				SearchResponse sr = 
    				new SearchResponse
    				(
    						search.GetWord(), 
    						search.GetSenderID(), 
    						node_id, 
    						mainStore.Retrieve(search.GetWord())
    				);
    				
    				String SearchResponseJSON = gson.toJson(sr);
    				Route r = rt.ClosestNodeToID(search.GetSenderID());
    				SendByRoute(SearchResponseJSON, r);
    			}
    			else 
    			{
    				//Pass the search to the next closest node
    				Route r = rt.ClosestNodeToID(search.GetNodeID());
    				SendByRoute(str, r);
    			}
    			
    			
		 			break;
    		case "SEARCH_RESPONSE":	
    			
    			//If we receive a search response
    			SearchResponse sr = gson.fromJson(str, SearchResponse.class);
    			
    			if(sr.GetNodeID() == node_id)
    			{
    				//This host is definitely alive, remove from watchlists
        			UnacknowledgedList.remove(new Long(sr.GetSenderID()));
        			PingedList.remove(new Long(sr.GetSenderID()));
    				
    				for(AggregatedSearchResult ASR : ActiveSearches)
    				{
    					if(ASR.IsDependentOn(sr.GetSenderID()))
    					{
    						//Update results for the user
    						ASR.AddSearchResponse(sr);
    					}
    				}
    				
    			}
    			else
    			{
    				Route r = rt.ClosestNodeToID(sr.GetNodeID());
    				SendByRoute(str, r);
    			}		

		 			break;
    		case "PING":	
    			
    			Ping ping = gson.fromJson(str, Ping.class);
    			
    			if(!(ping.GetTargetID() == node_id))
    			{
    				//Update IP Address on ping
    				ping.SetIPAddress(GetIPAddress());
    				Route r = rt.ClosestNodeToID(ping.GetTargetID());
    				//Convert back to JSON and send
    				SendByRoute(gson.toJson(ping), r);
    			}
    			
    			//For now it looks up the first node with the ip address from the ping
				//This shouldn't be an issue with standardised port numbers
				Ack pingAck = new Ack(ping.GetTargetID(), GetIPAddress());
				Route pingHost = rt.IPLookup(ping.GetIPAddress());
				SendByRoute(gson.toJson(pingAck), pingHost);

		 			break;
    		case "ACK":	
    			
    			//If we receive an acknowledgement message
    			Ack ack = gson.fromJson(str, Ack.class);
    			
    			//This host is definitely alive, remove from watchlists
    			UnacknowledgedList.remove(new Long(ack.GetNodeID()));
    			PingedList.remove(new Long(ack.GetNodeID()));
    			
    			//Relay ACK
    			if(!(ack.GetNodeID() == node_id))
    			{
    				//Update IP Address on ack
    				ack.SetIPAddress(GetIPAddress());
    				Route r = rt.ClosestNodeToID(ack.GetNodeID());
    				//Convert back to JSON and pass on
    				SendByRoute(gson.toJson(ack), r);
    			}

		 			break;
    		case "ACK_INDEX":	
    			
    			//If we receive an ack index message
    			
    			AckIndex AI = gson.fromJson(str, AckIndex.class);
    			
    			//Host is alive
    			UnacknowledgedList.remove(new Long(AI.GetNodeID()));
    			PingedList.remove(new Long(AI.GetNodeID()));
    			
    			if(AI.GetNodeID() == node_id)
    			{
    				//If this ack index message is destined for this node
    				
    				for(Transaction trans : PendingTransactions)
    				{
    					Index transIndex = trans.GetIndex();
    					if(transIndex.GetKeyword().equals(AI.GetKeyword()))
    					{
    						//Transaction has been confirmed
    						PendingTransactions.remove(trans);
    						break;
    					}
    				}
    			}
    			else
    			{
    				//Pass along the network
    				Route r = rt.ClosestNodeToID(AI.GetNodeID());
    				//Convert back to JSON and pass on
    				SendByRoute(str, r);
    			}
    	}
    }
    
    public static String GetIPAddress()
    {
    	//Code from http://stackoverflow.com/a/14364233
    	//Basically gets us a more reliable IP address for use on the local network
	    String ip = null;
	    try 
	    {
	        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        while (interfaces.hasMoreElements()) 
	        {
	            NetworkInterface iface = interfaces.nextElement();
	            // filters out 127.0.0.1 and inactive interfaces
	            if (iface.isLoopback() || !iface.isUp())
	                continue;
	
	            Enumeration<InetAddress> addresses = iface.getInetAddresses();
	            while(addresses.hasMoreElements()) 
	            {
	                InetAddress addr = addresses.nextElement();
	                ip = addr.getHostAddress();
	            }
	        }
	    } 
	    catch (SocketException e) 
	    {
	        throw new RuntimeException(e);
	    }
	    
	    return ip;
    }
    
    public void BootstrapNetwork(long nID)
    {
    	//When this is the first node in the network, we don't need join relay requests
    	IsBootstrap = true;
    	node_id = nID;
    }
    
    public void SendByRoute(String text, Route route)
    {
    	if(route == null || text == null)
    	{
    		//If the route or text is invalid, discard
    		return;
    	}
    	
    	//Sends a JSON formatted message to the IP Address and Port specified by route.
    	DatagramPacket p = new DatagramPacket(text.getBytes(), text.length(), route.GetInetAddress(), route.GetNodePort());
		try 
		{
			socket.send(p);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			System.out.println(identifier);
		}
    }
    
    public static long GetTime()
    {
    	//Get time in seconds
    	return (System.currentTimeMillis() / 1000);
    }
    
    public void stopExecution()
    {
    	//Test method here to simulate node failure
    	thread.interrupt();
    }
    
    public void PruneNetwork()
    {
    	//Deal with pruning, etc. here

    	//Remove Completed Searches From ActiveSearches and identify possible dead nodes
    	
    	java.util.Iterator<AggregatedSearchResult> it = ActiveSearches.iterator();
    	while(it.hasNext())
    	{
    		AggregatedSearchResult ASR = it.next();
    		
    		if(ASR.IsFinished() && ASR.HasDisplayed())
    		{
    			//If search has been completed and results displayed, remove the search
    			System.out.println("Removing Completed Search Request");
    			//Remove the search 
        		it.remove();
        		continue;
    		}
    		
    	}

    	
    	//Update the routing table and PossibleDeadList to prune dead nodes
    	
    	if(!UnacknowledgedList.isEmpty())
    	{
        	for (Map.Entry entry : UnacknowledgedList.entrySet()) 
    		{
    			Long node = (Long)entry.getKey();
    			Long lastSeen = (Long)entry.getValue();
    		    
    			if((GetTime() - lastSeen) > 30)
    			{
    				//Haven't seen node for more than 30 seconds - Ping it and add it to the PingedList
    				
    				//Send the Ping
    				Ping ping = new Ping(node, node_id, GetIPAddress());
    				Route r = rt.ClosestNodeToID(node);
    				SendByRoute(gson.toJson(ping), r);
    				
    				//Add node to PingedList
    				PingedList.put(node, lastSeen);
    				UnacknowledgedList.remove(node);
    			}
    			
    		}
    	
    	}
    	
    	if(!PendingTransactions.isEmpty())
    	{
    		/*
    		 * Not doing anything with unconfirmed transactions yet
    		 * 
    		 */
    	}
    	
    	if(!PingedList.isEmpty())
    	{
        	for (Map.Entry entry : PingedList.entrySet()) 
    		{
    			Long node = (Long)entry.getKey();
    			Long lastSeen = (Long)entry.getValue();
    		    
    			if((GetTime() - lastSeen) > 40)
    			{
    				//Haven't seen node for more than 40 seconds - remove from RT
    				rt.RemoveRoute(node);
    				PingedList.remove(node);
    				//System.out.println("Removed node " + node + " for failing to respond.");
    			}
    		}
    	}
    	
    	//Self perpetuating
    	Timer timer = new Timer();
		timer.schedule( new TimerTask(){ public void run() { PruneNetwork(); } }, 5000); //Run again in 5 seconds
    }

}


