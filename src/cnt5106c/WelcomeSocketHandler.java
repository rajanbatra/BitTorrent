package cnt5106c;

import java.io.IOException;
import java.net.ServerSocket;

// Need this make welcome socket listen to requests in a separate thread. Else, while the Peer is waiting for
// incoming messages, it may not allow the main thread to proceed.
public class WelcomeSocketHandler extends Thread {
 	
 	private Peer peer;
 		
 	WelcomeSocketHandler( Peer peer )	{ this.peer = peer; this.start(); }
 		
 	public void run() {
 			
 		ServerSocket listener = null;
		try {
			listener = new ServerSocket( peer.portNr() );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
 	    try {
 	    	while( true )
 	          	new ConnectionSocketHandler( listener.accept(), peer );
 	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 	    finally {
 	       	try {
 	       		listener.close();
			} catch (IOException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
			}
 	    }
 	}
}