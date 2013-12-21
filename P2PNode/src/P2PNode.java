import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

import com.google.gson.Gson;


public class P2PNode 
{

	public static void main(String[] args) throws SocketException 
	{
		
		PeerSearch node1 = new PeerSearch();
		PeerSearch node2 = new PeerSearch();
		PeerSearch node3 = new PeerSearch();
		
		DatagramSocket sock1 = new DatagramSocket(7777);
		DatagramSocket sock2 = new DatagramSocket(7778);
		DatagramSocket sock3 = new DatagramSocket(7779);
		
		
		InetSocketAddress a = new InetSocketAddress("127.0.0.1", 7777);
		
		node1.init(sock1);
		node1.BootstrapNetwork(node1.hashCode("cats"));
		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(">");
		try{ br.readLine(); } catch (IOException e) {e.printStackTrace();}
		
		node2.init(sock2);
		node2.joinNetwork(a, "hendrix","cats"); 
		
		System.out.print(">");
		try{ br.readLine(); } catch (IOException e) {e.printStackTrace();}

		node3.init(sock3);
		node3.joinNetwork(a, "free","cats"); 
		
		System.out.print(">");
		try{ br.readLine(); } catch (IOException e) {e.printStackTrace();}

		// ... lots more initialisation of nodes
		
		String testIndexTerms[] = {"hendrix","free"};
		node1.indexPage("http://freehendrix.com", testIndexTerms ); // this message would route to hendrix and free nodes
		
		String testIndexTerms2[] = {"cats"};
		node1.indexPage("http://catstuff.com", testIndexTerms2 );
		node1.indexPage("http://othercoolcatstuff.com", testIndexTerms2 );
		node2.indexPage("http://catdiddley.com", testIndexTerms2 );
		node3.indexPage("http://siamesecats.com", testIndexTerms2 );
		node3.indexPage("http://catdiddley.com", testIndexTerms2 );
		
		String testSearchTerms[] = {"hendrix", "free", "cats"};
		SearchResult results[] = node1.search(testSearchTerms); // this message would route to hendrix, free, and cats nodes
		
		//Kill node 3
		node3.stopExecution();
		
		String testSearchTerms2[] = {"free"};
		node1.search(testSearchTerms2); // this message should fail since node 3 is dead
		
		
		
		
		
	}
	

}
