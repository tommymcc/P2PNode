import java.net.*;


public class P2PNode 
{
	/*
	 * Skeleton code to drive the execution of 3 instances of PeerSearch 
	 * 
	 * Demo - Running Without Arguments:
	 * Creates 3 nodes with unique ports on the local machine
	 * First node bootstraps the network and the others join.
	 * Various index and search requests are run.
	 * Failure of node3 is simulated which triggers a pruning of the routing tables.
	 * 
	 * Running With Arguments:
	 * Will either bootstrap a network by specifying "--boot" and a keyword or join an 
	 * existing p2p network by specifying "-bootstrap", the address of a bootstrap node and
	 * a network id to use, specified using "--id".
	 * 
	 */

	public static void main(String[] args) throws SocketException 
	{
		String id = null;
		String bootstrapIP = null;
		boolean bootstrap = false;
		
		if(args.length > 0)
		{
			//If the program has command line arguments and is not just demoing 
			
			//Retrieve inputs
			for(int i = 0; i < args.length; i+=2)
			{
				if(args[i].equals("--boot"))
				{
					//Takes keyword instead of Integer Identifier
					bootstrap = true;
					id = args[i+1];
				}
				else if(args[i].equals("--id"))
				{
					id = args[i+1];
				}
				else if(args[i].equals("--bootstrap"))
				{
					//The IP of the bootstrap node needed to join the network
					bootstrapIP = args[i+1];
				}
			}
			
			//Create a new peersearch instance
			PeerSearch node1 = new PeerSearch();
			DatagramSocket sock1 = new DatagramSocket(8767);
			node1.init(sock1);
			
			if(bootstrap)
			{
				//Set up bootstrap node with key id
				node1.BootstrapNetwork(node1.hashCode(id));
			}
			else
			{
				//Join a network with the address of the bootstrap node
				InetSocketAddress a = new InetSocketAddress(bootstrapIP, 8767);
				node1.joinNetwork(a, id, "bootstrap"); 
			}
			
			
			//Perform transactions here..
			//Index requests, Searches, etc
			
		}
		else
		{
			
			//Create the PeerSearch Node instances
			PeerSearch node1 = new PeerSearch();
			PeerSearch node2 = new PeerSearch();
			PeerSearch node3 = new PeerSearch();
			
			//A UDP Socket for each node
			DatagramSocket sock1 = new DatagramSocket(8767);
			DatagramSocket sock2 = new DatagramSocket(7778);
			DatagramSocket sock3 = new DatagramSocket(7779);
			
			//The address of the bootstrap node
			InetSocketAddress a = new InetSocketAddress("127.0.0.1", 8767);
			
			//Set up bootstrap node with key 'cats'
			node1.init(sock1);
			node1.BootstrapNetwork(node1.hashCode("cats"));
			
			//Set up joining node with key "hendrix"
			node2.init(sock2);
			node2.joinNetwork(a, "hendrix","cats"); 
	
			//Third node
			node3.init(sock3);
			node3.joinNetwork(a, "free","cats"); 
	
			//Index a URL with the keywords "hendrix" and "free"
			String testIndexTerms[] = {"hendrix","free"};
			node1.indexPage("http://freehendrix.com", testIndexTerms ); // this message would route to hendrix and free nodes
			
			//Index some URLs with the keyword "cats"
			String testIndexTerms2[] = {"cats"};
			node1.indexPage("http://catstuff.com", testIndexTerms2 );
			node1.indexPage("http://othercoolcatstuff.com", testIndexTerms2 );
			node2.indexPage("http://catvideos.com", testIndexTerms2 );
			node3.indexPage("http://siamesecats.com", testIndexTerms2 );
			node3.indexPage("http://catvideos.com", testIndexTerms2 );
			
			//Search for URLs matching "hendrix", "free" or "cats"
			String testSearchTerms[] = {"hendrix", "free", "cats"};
			SearchResult results[] = node1.search(testSearchTerms); // this message would route to hendrix, free, and cats nodes
			
			//Print Results as a Test
			System.out.println("Printing Results:");
			for(SearchResult sr : results)
			{
				System.out.println(sr.GetWord() + " : " + sr.GetURLs()[0] + " " + sr.GetFrequency());
			}
			
			//Kill node 3 to simulate communication failure with a node
			try
			{
				node3.stopExecution();
			}
			catch(Exception e){}
			
			//Node1 attempts to search node3 which is offline, leading to network pruning
			String testSearchTerms2[] = {"free"};
			node1.search(testSearchTerms2); // this message should fail since node 3 is dead
		
		}
	}
}
