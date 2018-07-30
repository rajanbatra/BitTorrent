package cnt5106c;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import cnt5106c.peerProcess.HandShakeMessage;
import cnt5106c.peerProcess.BitFieldMessage;
import cnt5106c.peerProcess.HaveMessage;
import cnt5106c.peerProcess.MessageType;
import cnt5106c.peerProcess.MessageWithNoPayLoad;
import cnt5106c.peerProcess.RequestMessage;
import cnt5106c.peerProcess.PieceMessage;

public class RequestSocketHandler extends Thread {
	
	private Socket requestSocket;
	private ObjectOutputStream out;
 	private ObjectInputStream in;
 	
 	private Peer peer;
 	private RemotePeerInfo otherPeerInfo;
 	
 	private boolean isNeighbourUnchoked()
 	{
 		return peer.neighbours().getPeerInfo(this.otherPeerInfo.peerID).isUnchoked || peer.neighbours().getPeerInfo(this.otherPeerInfo.peerID).isOptimisticallyUnchoked;
 	}

 	public RequestSocketHandler( Socket requestSocket, Peer peer, RemotePeerInfo otherPeerInfo ) {
 		
 		// System.out.println( peer.peerID() + ": " + peer.portNr() + " connected to " + otherPeerInfo.peerID + " in port " + requestSocket.getPort() );
 		this.requestSocket = requestSocket;
 		this.peer = peer;
	    this.otherPeerInfo = otherPeerInfo;
	    if ( peer.nrConnectedNeighbours < peer.config().nrPreferredNeighbors )
	    	peer.nrConnectedNeighbours++;
	    
 		this.start();
 	}
 	
 	public void run()
 	{
 		try{
			//initialize inputStream and outputStream
			out = new ObjectOutputStream( requestSocket.getOutputStream() );
			out.flush();
			in = new ObjectInputStream( requestSocket.getInputStream() );
			
			HandShakeMessage handshakemsg = new HandShakeMessage( peer.peerID() );
			peer.log().connectionTo( otherPeerInfo.peerID );
			sendMessage( handshakemsg );
			
			while( true )
			{
				Object object = in.readObject();
				// if ( !isNeighbourUnchoked() )
				//	continue;
				
				if ( object instanceof HandShakeMessage )
				{
					HandShakeMessage msg = (HandShakeMessage)object;
					if ( msg.peerID != otherPeerInfo.peerID )
						break;
					
					// System.out.println("Peer " + peer.peerID() + " received back handshake msg with header: " + receivedmsg.handShakeHeader );
					sendMessage( new BitFieldMessage(peer.bitField()) );
				}
				else if ( object instanceof BitFieldMessage )
				{
					final BitFieldMessage msg = (BitFieldMessage)object;
					int[] otherBitField = msg.bitField();
					if ( isNeighbourUnchoked() && peer.isInteresting(otherBitField ) )
					{
						sendMessage( new MessageWithNoPayLoad(MessageType.interested) );
						int[] bitfield = peer.bitField();
						Random rand = new Random();
						while ( true )
						{
							final int idx = rand.nextInt( bitfield.length );
							if ( bitfield[idx]==0 && otherBitField[idx]==1 )
								{ sendMessage( new RequestMessage(idx) ); break; }
						}
					}
					else
						sendMessage( new MessageWithNoPayLoad(MessageType.notinterested) );
				}
				else if ( object instanceof MessageWithNoPayLoad )
				{
					MessageWithNoPayLoad msg = (MessageWithNoPayLoad)object;
					if ( msg.type() == MessageType.choke )
						peer.log().chokedBy( this.otherPeerInfo.peerID );
					else if ( msg.type() == MessageType.unchoke )
					{
						peer.log().unchokedBy( this.otherPeerInfo.peerID );
						if ( !peer.hasFile && isNeighbourUnchoked() )
							sendMessage( new BitFieldMessage(peer.bitField()) );
					}
					else if ( msg.type() == MessageType.interested )
						peer.log().receivedInterestedFrom( this.otherPeerInfo.peerID );
					else if ( msg.type() == MessageType.notinterested )
						peer.log().receivedNotInterestedFrom( this.otherPeerInfo.peerID );
				}
				else if ( object instanceof HaveMessage )
				{
					HaveMessage msg = (HaveMessage)object;
					peer.log().receivedHaveFrom( this.otherPeerInfo.peerID, msg.pieceIndex() );
					if ( !peer.hasFile && isNeighbourUnchoked() )
						sendMessage( new RequestMessage(msg.pieceIndex()) );
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
					peer.log().downloadedPiece( this.otherPeerInfo.peerID, msg.pieceIndex(), peer.nrPieces() );
					if ( peer.nrPieces() == peer.config().nrPieces )
						peer.log().downloadCompleted();
					else
						sendMessage( new BitFieldMessage(peer.bitField()) );
				}
			}
		}
		catch (ConnectException e) {
    		System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        } 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException) {
			ioException.printStackTrace();
		}
		finally {
			//Close connections
			try {
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException) {
				ioException.printStackTrace();
			}
		}
 	}
 	
 	//send a message to the output stream
 	void sendMessage( Object msg )
 	{
 		try{
 			out.writeObject( msg );
 			out.flush();
 		}
 		catch(IOException ioException){
 			ioException.printStackTrace();
 		}
 	}
}