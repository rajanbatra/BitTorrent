package cnt5106c;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import cnt5106c.peerProcess.BitFieldMessage;
import cnt5106c.peerProcess.HandShakeMessage;
import cnt5106c.peerProcess.HaveMessage;
import cnt5106c.peerProcess.MessageType;
import cnt5106c.peerProcess.MessageWithNoPayLoad;
import cnt5106c.peerProcess.PieceMessage;
import cnt5106c.peerProcess.RequestMessage;

/**
* A handler thread class. Handlers are spawned from the listening
* loop by a peer and are responsible for dealing with another peer's requests.
*/
public class ConnectionSocketHandler extends Thread {

	private Socket connectionSocket;
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;	//stream write to the socket
	private Peer peer;
	private int otherPeerID = -1;

    public ConnectionSocketHandler( Socket connectionSocket, Peer peer ) {
    	
    	this.connectionSocket = connectionSocket;
	    this.peer = peer;
	    if ( peer.nrConnectedNeighbours < peer.config().nrPreferredNeighbors )
	    	peer.nrConnectedNeighbours++;
	    
	    // final InetAddress clientIA = connectionSocket.getInetAddress();
	    // System.out.println( "HOSTNAME: " + clientIA.getHostName() );
	    // this.peer.log().connectionFrom( peer.neighbours().getPeerID(clientIA.getHostName()) ); // TODO: Check if address has to be passed.
	    this.start();
    }

    private boolean isNeighbourUnchoked()
 	{
    	if ( this.otherPeerID == -1 )
    		return false;
    	
 		return peer.neighbours().getPeerInfo(this.otherPeerID).isUnchoked || peer.neighbours().getPeerInfo(this.otherPeerID).isOptimisticallyUnchoked;
 	}
    
    public void run()
    {    	
       	try{
       		//initialize Input and Output streams
       		out = new ObjectOutputStream(connectionSocket.getOutputStream());
       		out.flush();
       		in = new ObjectInputStream(connectionSocket.getInputStream());
       		try{
       			while ( true )
       			{
       				Object object = in.readObject();
       				// if ( !isNeighbourUnchoked() )
       				//	continue;
       				
    				if ( object instanceof HandShakeMessage )
    				{
    					// Receive HandShakeMessage from client peer
    					HandShakeMessage receivedmsg = (HandShakeMessage)object;
    					this.otherPeerID = receivedmsg.peerID;
    					this.peer.log().connectionFrom( this.otherPeerID );
    					System.out.println("Peer " + peer.peerID() + " received connection from peer: " + this.otherPeerID );
    				
    					// Send HandShakeMessage to client peer
    					sendMessage( new HandShakeMessage(peer.peerID()) );
    				}
    				else if ( object instanceof BitFieldMessage )
    				{
    					sendMessage( new BitFieldMessage(peer.bitField()) );
    					
    					BitFieldMessage otherBitField = (BitFieldMessage)object;
    					if ( isNeighbourUnchoked() && peer.isInteresting(otherBitField.bitField() ) )
    						sendMessage( new MessageWithNoPayLoad(MessageType.interested) );
    					else
    						sendMessage( new MessageWithNoPayLoad(MessageType.notinterested) );
    				}
    				else if ( object instanceof MessageWithNoPayLoad )
    				{
    					MessageWithNoPayLoad msg = (MessageWithNoPayLoad)object;
    					if ( msg.type() == MessageType.choke )
    						peer.log().chokedBy( this.otherPeerID );
    					else if ( msg.type() == MessageType.unchoke )
    						peer.log().unchokedBy( this.otherPeerID );
    					else if ( msg.type() == MessageType.interested )
    						peer.log().receivedInterestedFrom( this.otherPeerID );
    					else if ( msg.type() == MessageType.notinterested )
    						peer.log().receivedNotInterestedFrom( this.otherPeerID );
    				}
    				else if ( object instanceof HaveMessage )
    				{
    					HaveMessage msg = (HaveMessage)object;
    					
    					peer.log().receivedHaveFrom( this.otherPeerID, msg.pieceIndex() );
    				}
    				else if ( object instanceof RequestMessage )
    				{
    					RequestMessage msg = (RequestMessage)object;
    					if ( isNeighbourUnchoked() )
    						sendMessage( new PieceMessage(msg.pieceIndex(),peer.getFilePiece(msg.pieceIndex())) );
    				}
    				else if ( object instanceof PieceMessage )
    				{
    					PieceMessage msg = (PieceMessage)object;
    					peer.setFilePiece( msg.pieceIndex(), msg.payLoad() );
    					peer.log().downloadedPiece( this.otherPeerID, msg.pieceIndex(), peer.nrPieces() );
    					if ( peer.nrPieces() == peer.config().nrPieces )
    						peer.log().downloadCompleted();
    				}
       			}
       		}
       		catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
       	}
       	catch(IOException ioException){
       		System.out.println("Disconnect with Peer " + peer.peerID());
       	}
       	finally{
       		//Close connections
       		try{
       			in.close();
       			out.close();
       			connectionSocket.close();
       		}
       		catch(IOException ioException){
       			System.out.println("Disconnect with Peer " + peer.peerID());
       		}
       	}
    }
    
    //send a message to the output stream
    public void sendMessage( Object msg )
    {
      	try{
       		out.writeObject( msg );
       		out.flush();
       	}
       	catch(IOException ioException) {
       		ioException.printStackTrace();
       	}
    }
}
