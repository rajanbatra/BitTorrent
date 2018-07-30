package cnt5106c;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Vector;

import cnt5106c.Peer;

public class peerProcess {
	
	static String localHost = "localhost";
	static String baseDirectory = System.getProperty("user.dir") + "/cnt5106c/";
	static String peerConfigurationFile = baseDirectory + "PeerInfo.cfg";
	static String commonConfigurationFile = baseDirectory + "Common.cfg";
	
	static class HandShakeMessage implements Serializable {
	
		private static final long serialVersionUID = 1L;
		
		final String handShakeHeader = "P2PFILESHARINGPROJ";
		final byte tenByteZeroBits[] = new byte[10]; 
		final int peerID;
		
		HandShakeMessage( int peerID )	{ this.peerID = peerID; }
	}
	
	static enum MessageType {
		choke( 0 ), unchoke( 1 ), interested( 2 ), notinterested( 3 ), have( 4 ), bitfield( 5 ), request( 6 ), piece( 7 );
		
		MessageType( int value )		{ this.value = (byte)value; }
		public int getValue()			{ return value; }
		private final byte value;
	}
	
	static class MessageWithNoPayLoad implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		MessageWithNoPayLoad( MessageType type )					{ this.type = type; this.length = 1; }
		
		public int length()							{ return length; }
		public MessageType type()					{ return type; }
		
		private int			length;
		private MessageType	type;
	}
	
	static class HaveMessage implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		HaveMessage( int pieceIndex )				{ this.length = 5; this.pieceIndex = pieceIndex; }
		
		public int length()							{ return length; }
		public MessageType type()					{ return type; }
		public int pieceIndex()						{ return pieceIndex; }
		
		private int			length;
		private MessageType	type = MessageType.have;
		private int			pieceIndex;
	}
	
	static class BitFieldMessage implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		BitFieldMessage( int[] bitfield )			{ this.length = bitfield.length+1; this.bitField = bitfield.clone(); }
		
		public int length()							{ return length; }
		public MessageType type()					{ return type; }
		public int[] bitField()						{ return bitField; }
		
		private int			length;
		private MessageType	type = MessageType.piece;
		private int[]		bitField;
	}
	
	static class RequestMessage implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		RequestMessage( int pieceIndex )			{ this.length = 5; this.pieceIndex = pieceIndex; }
		
		public int length()							{ return length; }
		public MessageType type()					{ return type; }
		public int pieceIndex()						{ return pieceIndex; }
		
		private int			length;
		private MessageType	type = MessageType.request;
		private int			pieceIndex;
	}
	
	static class PieceMessage implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		PieceMessage( int pieceIndex, byte[] payload )			{ this.length = payload.length+5; this.pieceIndex = pieceIndex; this.payLoad = payload.clone(); }
		
		public int length()							{ return length; }
		public MessageType type()					{ return type; }
		public int pieceIndex()						{ return pieceIndex; }
		public byte[] payLoad()						{ return payLoad; }
		
		private int			length;
		private MessageType	type = MessageType.piece;
		private int			pieceIndex;
		private byte[]		payLoad;
	}
	
	static class CommonConfiguration {
		
		int 	nrPreferredNeighbors;
		int 	unchokingInterval; //!< In seconds.
		int 	optimisticUnchokingInterval;
		String	fileName;
		int 	fileSize;
		int 	pieceSize;
		int		nrPieces;
		byte[]	fileBytes;
		
		CommonConfiguration() throws Exception {
			
			BufferedReader reader = new BufferedReader( new FileReader(commonConfigurationFile) );
	    String info;
	    info = reader.readLine();
            nrPreferredNeighbors = Integer.parseInt( info.split(" ")[1] );
	    info = reader.readLine();
            unchokingInterval = Integer.parseInt( info.split(" ")[1] );
	    info = reader.readLine();
            optimisticUnchokingInterval = Integer.parseInt( info.split(" ")[1] );
	    info = reader.readLine();
            fileName = info.split(" ")[1];
	    info = reader.readLine();
            fileSize = Integer.parseInt( info.split(" ")[1] );
	    info = reader.readLine();
            pieceSize = Integer.parseInt( info.split(" ")[1] );
	    info = reader.readLine();
            nrPieces = (int)Math.ceil(((float)fileSize)/pieceSize);
            reader.close();
            
            fileBytes = new byte[fileSize];
            new Random().nextBytes( fileBytes ); // TODO: Check, should be created only once.
		}
	}
	
	static class NeighbourPeerInfo {
		
		private LinkedHashMap<Integer,RemotePeerInfo> peerInfos;
		private HashMap<String,Integer> peerIDs;
		
		public RemotePeerInfo getPeerInfo( int peerID )				{ return peerInfos.get( peerID ); }
		public int getPeerID( String hostName )						{ return peerIDs.get( hostName ); }
		public LinkedHashMap<Integer,RemotePeerInfo> peerInfos()	{ return this.peerInfos; }
		
		public RemotePeerInfo randomNeighbour()
		{
			ArrayList<Integer> keysAsArray = new ArrayList<Integer>( peerInfos.keySet() );
			Random generator = new Random();
			return peerInfos.get( keysAsArray.get(generator.nextInt(keysAsArray.size())) );
		}
		
		NeighbourPeerInfo( int peerID ) throws Exception {
			
			this.peerInfos = new LinkedHashMap<Integer,RemotePeerInfo>();
			this.peerIDs = new HashMap<String,Integer>(); //!< (hostName,peerID)
			
			BufferedReader reader = new BufferedReader( new FileReader(peerProcess.peerConfigurationFile) );
			String peerConfig;
			while( (peerConfig = reader.readLine()) != null )
			{
				final String[] tokens = peerConfig.split( "\\s+" );
				final RemotePeerInfo peerInfo = new RemotePeerInfo( tokens[0], tokens[1], tokens[2] );
				// if ( peerInfo.peerID == peerID )
				//	continue;
				
				peerInfos.put( peerInfo.peerID, peerInfo );
				peerIDs.put( peerInfo.peerAddress, peerInfo.peerID );
			}
			
			reader.close();
		}
	}
		
	public static void main( String args[] ) throws Exception
	{
		final int peerID = Integer.parseInt( args[0] );
		
		BufferedReader reader = new BufferedReader( new FileReader(peerProcess.peerConfigurationFile) );
		String peerConfig;
		while( (peerConfig = reader.readLine()) != null )
		{
			final String[] tokens = peerConfig.split( "\\s+" );
			final RemotePeerInfo peerInfo = new RemotePeerInfo( tokens[0], tokens[1], tokens[2] );
			final boolean hasfile = (Integer.parseInt(tokens[3]) == 1);
			if ( peerInfo.peerID == peerID )
			    new Peer( peerInfo.peerID, peerInfo.peerAddress, peerInfo.peerPort, hasfile );
		}
		
        reader.close();
	}
}
