import java.io.IOException;
import java.net.*;
import java.util.*;

import com.google.gson.*;


/* To Do 
 * 
 * 
 * - Coding Tasks -
 * Ensure the Routing Table uses 'Prefix Routing' and generally optimise
 * 
 * Local Storage For Records - Low Priority
 * 
 * Methods returning search results as an array - High Priority
 * 
 * Pruning Routing Table - High Priority
 * 
 * Aggregating Search Results:
 * In response to a request to search for a set of words, a set of SEARCH messages should be sent on the network, 
 * one for each word passed, each message including as the target node_id the hash code of the word and other details 
 * as indicated in the SEARCH message description below. The node should then wait for a set of SEARCH_RESPONSE messages to be received. 
 * After a timeout (3 seconds), the responses that have been received should be aggregated and a response returned. 
 * After 30 seconds, any as yet non-received responses should cause PING messages to be sent as described below in the section 
 * "Message Acknowledgement". 
 * 
 * For an INDEX message sent by a node, the receipt by the target should be acknowledged by an ACK message sent via the overlay network. 
 * These messages should be received within 30 seconds by the original node. If an ACK message, or alternative communication is not received 
 * in that timeframe, the original node should attempt to send a PING message to the same target and wait for an ACK message. 
 * The PING message proceeds in the normal way through the overlay network. At each stage, on receipt of a PING message, 
 * a node should send an ACK message directly (i.e. using the ip address) to the immediate sender of the PING 
 * message (i.e. not the originator of the PING message), and should send a PING message on to the next node as appropriate. 
 * If the node is the final target of the PING, it should cease processing. 
 * 
 * If an expected ACK message is not received within 10 seconds the node waiting should remove the node to which 
 * the PING message was sent from the routing table.
 * 
 * 
 * - Error Handling -
 * 
 * - Final Changes -
 * All communication will be via UDP to port 8767.
 * 
 * To start a network, a first node must be initialised as a bootstrap node. 
 * Your implementation should accept a command line parameter "--boot [Integer Identifier 232]" and if passed must become the first node. 
 * The first node will open a UDP port 8767 and wait for connections. 
 * The bootstrap node may leave the network once at least one node is connected. 
 * 
 * A node wishing to join the network must have the ip address of a node presently known to be connected to the 
 * network (the bootstrap node may be available but is not guaranteed). 
 * The initial node ip address and the identifier for this node should be sent to the system 
 * via a command line parameter "--bootstrap [IP Address] --id [Integer Identifier 232]"
 * 
 */


// for the simplified routing solution
interface PeerSearchSimplified 
{
    void init(DatagramSocket udp_socket); // initialise with a udp socket
    
    long joinNetwork(InetSocketAddress bootstrap_node, String identifier, String target_identifier ); 
    //returns network_id, a locally
    // generated number to identify peer network
    
    boolean leaveNetwork(long network_id); // parameter is previously returned peer network identifier
    void indexPage(String url, String[] unique_words);
    SearchResult[] search(String[] words);
}

public class PeerSearch extends Thread implements PeerSearchSimplified
{
	DataStore mainStore = new DataStore();
	RoutingTable rt = new RoutingTable();
	ArrayList<AggregatedSearchResult> ActiveSearches = new ArrayList<AggregatedSearchResult>(); //Searches initiated by this node
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
		UnacknowledgedList = new HashMap<Long,Long>();
		PingedList = new HashMap<Long,Long>();
		PendingTransactions = new ArrayList<Transaction>(); //Just keeps track of unconfirmed index requests
		start(); //Start new Thread
		
		Timer timer = new Timer();
		timer.schedule( new TimerTask(){ public void run() { PruneNetwork(); } }, 2500); //Start Pruning network after 25 seconds
	}
	
    public long joinNetwork(InetSocketAddress bootstrap_node, String id, String target_identifier )
    {
		
    	//returns network_id, a locally
    	// generated number to identify peer network
    	
    	node_id = hashCode(id);
    	identifier = id;
    	
    	if(!IsBootstrap)
    	{
    		System.out.println("Node:" + identifier);
    		System.out.println("Attempting to join network");
    		
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
	    		requestPacket = new DatagramPacket(packetBytes, packetBytes.length, bootstrap_node.getAddress(), bootstrap_node.getPort());
	    		socket.send(requestPacket);
	    	
	    	} catch (UnknownHostException e) 
			{
				e.printStackTrace();
				//Add error handling at some stage - messages, etc
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
	    	
	    	//If request to join was successful, add the bootstrap node to our RoutingTable
	    	rt.UpdateRoute(hashCode(target_identifier), bootstrap_node.getAddress().getHostAddress(), bootstrap_node.getPort());
	    	
    	}
    	else
    	{
    		//If this node is the bootstrap node
    		System.out.println("Node:" + identifier);
    		System.out.println("Bootstrapping Network");
    	}
        
    	return node_id;
    }
                                       
    public boolean leaveNetwork(long network_id)
    {
    	// parameter is previously returned peer network identifier
    	System.out.println("Node:" + identifier);
    	System.out.println("Leaving Network...");
    	
    	LeavingNetwork ln = new LeavingNetwork(network_id);
    	String lnJSON = gson.toJson(ln);
    	
    	//Send message to all the nodes this node is connected to
    	Route otherNodes[] = rt.GetRoutingInfo();
    	
    	for(Route r : otherNodes)
    	{
    		//Send to each node
    		SendByRoute(lnJSON, r);
    	}
    	System.out.println("Peers Notified");
    	return true;
    }
    
    public void indexPage(String url, String[] unique_words)
    {
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
			
			System.out.println("Node:" + identifier);
			System.out.println("Sending Index Request");
    	}
    	
    }
    
    public SearchResult[] search(String[] words)
    {
    	AggregatedSearchResult ASR = new AggregatedSearchResult();
    	
    	
    	for(String word : words)
    	{
    		//Create a new search request and send it over the network
    		Search s = new Search(word, hashCode(word), node_id);
    		Route r = rt.ClosestNodeToID(hashCode(word));
    		UnacknowledgedList.put(new Long(r.GetNodeID()), new Long(GetTime()));
			SendByRoute(gson.toJson(s), r);
			
			//Should keep a record of queries
			ASR.AddDependentNode(hashCode(word));
    	}
    	
    	ActiveSearches.add(ASR);
    	
    	System.out.println("Node:" + identifier);
    	System.out.println("Sent Search Request(s)");
    	
    	
    	SearchResult[] a = new SearchResult[1];
    	a[0]= new SearchResult();
    	return a;
    	
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
    	 * Listens for packets and decodes them into messages
    	 * which are handled by their respective methods.
    	 */
    	System.out.println("Node:" + identifier);
    	System.out.println("Started Listening");
    	
    	for(;;)
    	{
        	byte[] data = new byte[1024];
        	String str;
            DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
        	try 
        	{
				socket.receive(receivedPacket);
			} 
        	catch (IOException e) 
			{
        		//Malformed packet, go on to the next one
        		continue;
				//e.printStackTrace();
			}
        	
        	str = new String(data, 0, receivedPacket.getLength());
        	
        	//Convert to generic Message Object to determine Message type
        	Message m = gson.fromJson(str, Message.class);
        	if(m == null)
        	{
        		//If what we received wasn't a message, discard it
        		continue;
        	}
        	
        	//System.out.println("Received Msg of type:");
        	//System.out.println(m.type);
        	
        	//Handle Each Message Type Here
        	
        	switch(m.type)
        	{
        		case "JOINING_NETWORK_SIMPLIFIED":
        			
        			//Send routing table info here
        			
        			//Update our own routing info
        			JoinRequest j = gson.fromJson(str, JoinRequest.class);
        			rt.UpdateRoute(j.GetNodeID(), j.GetIPAddress(), receivedPacket.getPort());
        			//System.out.println("Node " + j.GetNodeID() + " joined.");
        			//System.out.println(receivedPacket.getAddress().getHostAddress());
        			
        			System.out.println("Node:" + identifier);
        			System.out.println("Join Request Received.\nRouting Table Updated.");
        			
        			//Show updated routing table
        			rt.print();
        			
        			//Send this nodes routing table
        			
        			RoutingInfo ri = new RoutingInfo(node_id, j.GetNodeID(), GetIPAddress());
        			ri.AddRoutes((Route[])rt.GetRoutingInfo());
        			String RoutingInfoJSON = gson.toJson(ri);
        			
        			SendByRoute(RoutingInfoJSON, rt.ClosestNodeToID(j.GetNodeID()));
        			System.out.println("Node:" + identifier);
    				System.out.println("Sending Routing Info To Joining Node:");
        			
        		 	break;
        		case "JOINING_NETWORK_RELAY_SIMPLIFIED":	
        			
        			/* Working */
        			
        			//Received a Relay request from joining node or other relay
        			JoiningNetworkRelay jnr = gson.fromJson(str, JoiningNetworkRelay.class);
        			
        			//Send Routing info here
        			System.out.println("Node:" + identifier);
        			System.out.println("Received Network Relay");
        			
        			//Create RoutingInfo representation of our RoutingTable
        			ri = new RoutingInfo(jnr.GetGatewayID(), jnr.GetNodeID(), GetIPAddress());
        			ri.AddRoutes((Route[])rt.GetRoutingInfo());
        			
        			//Convert RoutingInfo to JSON
        			RoutingInfoJSON = gson.toJson(ri);
        			
        			System.out.println("Node:" + identifier);
        			System.out.println("Routing Info Message Assembled:\n");
        			System.out.println(RoutingInfoJSON + "\n");
        			
        			//Routing Info Going 'backwards' towards gateway / joining node
        			if(jnr.GetGatewayID() == node_id)
        			{
        				//If this node is the gateway, forward routing info directly to joining node
        				SendByRoute(RoutingInfoJSON, rt.ClosestNodeToID(jnr.GetNodeID()));
        				System.out.println("Node:" + identifier);
        				System.out.println("Sending Routing Info To Final Dest:");
        				
        				Route r = rt.ClosestNodeToID(jnr.GetNodeID());
        				System.out.println("Node:" + identifier);
        				System.out.println(r.GetNodeIP() + ":" + r.GetNodePort());
        				
        			}
        			
        			if(jnr.GetTargetID() != node_id)
        			{
        				//If this is not the destination, relay JNR onto next node
        				Route r = rt.ClosestNodeToID(jnr.GetTargetID());
        				SendByRoute(str, r);
        				System.out.println("Node:" + identifier);
        				System.out.println("Sent Routing Info to Gateway");
        			}
        			
   		 			break;
        		case "ROUTING_INFO":
        			
        			/* Working */
        			
        			//If we receive a RoutingInfo Message
        			RoutingInfo rInfo = gson.fromJson(str, RoutingInfo.class);
        			
        			//Add routing info to our routing table
        			rt.MergeRoutingInfo(rInfo.GetRoutes());
        			
        			System.out.println("Node:" + identifier);
        			System.out.println("Added Routing Info.");
        			rt.print();
        			
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
            				System.out.println("Node:" + identifier);
            				System.out.println("Sent Routing Info Closer To Target:");
            				System.out.println(r.GetNodeID() + "@" + r.GetNodeIP() + ":" + r.GetNodePort());
        				}
        				else
        				{
        					//Relay to Gateway
        					Route r = rt.ClosestNodeToID(rInfo.GetGatewayID());
            				SendByRoute(str, r);
            				System.out.println("Node:" + identifier);
            				System.out.println("Sent Routing Info to Gateway");
        				}
        			}
        			
   		 			break;
        		case "LEAVING_NETWORK":	
        			
        			/* Working */
        			
        			//A host is leaving the network and must be removed from the routing table
        			LeavingNetwork ln = gson.fromJson(str, LeavingNetwork.class);
        			rt.RemoveRoute(ln.GetNodeID());
        			System.out.println("Node:" + identifier);
        			System.out.println("Host " + ln.GetNodeID() + " Left Network\nRouting Table Updated");
        			rt.print();

        			//What about propagation of leaving messages? ... potential for infinite loops, etc
        			
   		 			break;
        		case "INDEX":	
        			
        			/* Working */
        			
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
        				System.out.println("Node:" + identifier);
        				System.out.println("Indexed links for keyword '" + idx.GetKeyword() + "'");
        			}
        			else
        			{
        				//Pass on the message
        				Route r = rt.ClosestNodeToID(idx.GetTargetID());
        				SendByRoute(str, r);
        				System.out.println("Node:" + identifier);
        				System.out.println("Relayed Index Request");
        			}

        			//System.out.println();.”

        			
   		 			break;
        		case "SEARCH":	
        			
        			/* Working */
        			
        			Search search = gson.fromJson(str, Search.class);
        			System.out.println("Node:" + identifier);
        			System.out.println("Received Search Request");
        			
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
        				System.out.println("Node:" + identifier);
        				System.out.println("Sent search response");
        			}
        			else 
        			{
        				//Pass the search to the next closest node
        				Route r = rt.ClosestNodeToID(search.GetNodeID());
        				SendByRoute(str, r);
        				System.out.println("Node:" + identifier);
        				System.out.println("Relayed Search Request");
        			}
        			
        			
   		 			break;
        		case "SEARCH_RESPONSE":	
        			
        			/* Relay Working but need to display results*/
        			
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
        						ASR.AddSearchResponse(sr);
        					}
        				}
        		    	
        				
        				//If this is the response to a query made by this node
        				System.out.println("Node:" + identifier);
        				System.out.println("Received Search Response");
        				
        				//Display Search Results
        			}
        			else
        			{
        				Route r = rt.ClosestNodeToID(sr.GetNodeID());
        				SendByRoute(str, r);
        				System.out.println("Node:" + identifier);
        				System.out.println("Relayed Search Response");
        			}		

   		 			break;
        		case "PING":	
        			
        			Ping ping = gson.fromJson(str, Ping.class);
        			
        			if(ping.GetTargetID() == node_id)
        			{
        				//If ping destined for this node
        				System.out.println("Node:" + identifier);
        				System.out.println("Received a Ping. Replying...");
        			}
        			else
        			{
        				//Update IP Address on ping
        				ping.SetIPAddress(GetIPAddress());
        				Route r = rt.ClosestNodeToID(ping.GetTargetID());
        				//Convert back to JSON and send
        				SendByRoute(gson.toJson(ping), r);
        				System.out.println("Node:" + identifier);
        				System.out.println("Relayed Ping");
        			}
        			
        			//For now it looks up the first node with the ip address from the ping
    				//This shouldn't be an issue with standardised port numbers
    				Ack pingAck = new Ack(ping.GetTargetID(), GetIPAddress());
    				Route pingHost = rt.IPLookup(ping.GetIPAddress());
    				SendByRoute(gson.toJson(pingAck), pingHost);
        			
        			//System.out.println();
   		 			break;
        		case "ACK":	
        			
        			//If we receive an acknowledgement message
        			Ack ack = gson.fromJson(str, Ack.class);
        			
        			//This host is definitely alive, remove from watchlists
        			UnacknowledgedList.remove(new Long(ack.GetNodeID()));
        			PingedList.remove(new Long(ack.GetNodeID()));
        			
        			if(ack.GetNodeID() == node_id)
        			{
        				//If this acknowledgement is for this node
        				System.out.println("Node:" + identifier);
        				System.out.println("Receieved acknowledgement");
        			}
        			else
        			{
        				//Update IP Address on ping
        				ack.SetIPAddress(GetIPAddress());
        				Route r = rt.ClosestNodeToID(ack.GetNodeID());
        				//Convert back to JSON and pass on
        				SendByRoute(gson.toJson(ack), r);
        				System.out.println("Node:" + identifier);
        				System.out.println("Relayed Ack");
        			}
        			
        			
        			//System.out.println();
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
        				
        				System.out.println("Node:" + identifier);
        				System.out.println("Received Ack Index");
        			}
        			else
        			{
        				//Pass along the network
        				Route r = rt.ClosestNodeToID(AI.GetNodeID());
        				//Convert back to JSON and pass on
        				SendByRoute(str, r);
        				System.out.println("Node:" + identifier);
        				System.out.println("Relayed Ack Index");
        			}
        	}
    	}
    }
    
    public static String GetIPAddress()
    {
    	//Code from http://stackoverflow.com/a/14364233
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
	    } catch (SocketException e) 
	    {
	        throw new RuntimeException(e);
	    }
	    
	    return ip;
    }
    
    public void BootstrapNetwork(long nID)
    {
    	IsBootstrap = true;
    	node_id = nID;
    	System.out.println("Bootstrap Node ID:" + nID);
    }
    
    public void SendByRoute(String text, Route route)
    {
    	DatagramPacket p = new DatagramPacket(text.getBytes(), text.length(), route.GetInetAddress(), route.GetNodePort());
		try 
		{
			socket.send(p);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
    }
    
    public static long GetTime()
    {
    	//Get time in seconds
    	return (System.currentTimeMillis() / 1000);
    }
    
    public void stopExecution()
    {
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
    		
    		if(ASR.IsComplete() && ASR.HasDisplayed())
    		{
    			//If search has been completed and results displayed, remove the search
    			System.out.println("Removing Completed Search Request");
    			//Remove the search 
        		it.remove();
        		continue;
    		}
    		else if(!ASR.IsComplete() && ASR.HasDisplayed())
    		{
    			//If results have been displayed but some nodes did not reply in a timely fashion
    			Long pdn[] = ASR.GetDependentNodes();
    			
    			for(Long node : pdn)
    			{
    				if(!UnacknowledgedList.containsKey(node))
    				{
    					System.out.println("Adding node " + node + " to Unack. List.");
    					//Get nodes which haven't replied and the last time they were contacted
    					UnacknowledgedList.put(node, new Long(ASR.GetStartTime()));
    				}
    			}
    			
    			//Remove the search 
        		it.remove();
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
    				
    				System.out.println("Adding " + node + " to Pinged List.");
    				
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
    		 * 
        	for (Transaction trans : PendingTransactions) 
    		{
    			Index idx = trans.GetIndex();
    		    
    			if((GetTime() - trans.GetTransactionTime()) > 3)
    			{
    				//Haven't received confirmation in more than 30 seconds - Ping it and add it to the PingedList
    				
    				//Send the Ping
    				Ping ping = new Ping(node, node_id, GetIPAddress());
    				Route r = rt.ClosestNodeToID(node);
    				SendByRoute(gson.toJson(ping), r);
    				
    				PingedList.put(node, lastSeen);
    				UnacknowledgedList.remove(node);
    			}
    			
    		}
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
    				System.out.println("Removed node " + node + " for failing to respond.");
    			}
    		}
    	}
    	
    	//Self perpetuating
    	Timer timer = new Timer();
		timer.schedule( new TimerTask(){ public void run() { PruneNetwork(); } }, 5000); //Run again in 5 seconds
    	
    }

}


