package cnt5106c;

import java.net.*;
import java.io.*;
import java.util.*;
import cnt5106c.peerProcess;
import cnt5106c.peerProcess.CommonConfiguration;
import cnt5106c.peerProcess.NeighbourPeerInfo;

// Class for a Peer. Each Peer acts both as a server and a client. 
public class Peer extends Thread {
	
	private int peerID;
 	private String hostName;
 	private int listeningPort;
 	public boolean hasFile;
 	
 	private String fileDirectoryPath;
 	private String filePath;
 	private byte[] fileBytes;
 	private byte[][] filePieces;
 	private int[] bitField;
 	private int nrPieces = 0;
 	public int nrConnectedNeighbours = 0;
 	
 	private CommonConfiguration config = new CommonConfiguration();
 	private Log log;
 	private NeighbourPeerInfo neighbours;
 	
 	public int peerID()						{ return peerID; }
 	public int portNr()						{ return listeningPort; }
 	public boolean hasFile()				{ return hasFile; }
 	
 	public CommonConfiguration config()		{ return config; }
 	public Log log()						{ return log; }
 	public NeighbourPeerInfo neighbours()	{ return neighbours; }
 	public int nrPieces()					{ return nrPieces; }
 	public int[] bitField()					{ return bitField; }
 	public byte[] getFilePiece( int idx )	{ return filePieces[idx]; }
 	public void setFilePiece( int idx, byte[] piece ) throws IOException
 	{
 		if ( bitField[idx] == 1 )
 			return;
 		
 		filePieces[idx] = piece;
 		setBitField( idx );
 	}
 	
 	public boolean isInteresting( int[] otherbitField )
 	{
 		if ( this.hasFile )
 			return false;
 		
 		for ( int idx=0; idx<otherbitField.length; idx++ )
 			if ( this.bitField[idx]==0 && otherbitField[idx]==1 )
 				return true;
 		
 		return false;
 	}
 	
 	private void setBitField( int bitidx ) throws IOException
 	{
 		if ( this.hasFile || bitidx>=config.nrPieces || this.bitField[bitidx]==1 )
 			return;
 		
 		this.bitField[bitidx] = 1;
 		if ( ++(this.nrPieces) != config.nrPieces )
 			return;
 		
        for ( int idx=0; idx<filePieces.length; idx++ )
        {
        	final int destPos = idx*config.pieceSize;
        	if ( destPos+config.pieceSize > config.fileSize )
        		System.arraycopy( filePieces[idx], 0, this.fileBytes, destPos, config.fileSize-destPos );
        	else
        		System.arraycopy( filePieces[idx], 0, this.fileBytes, destPos, config.pieceSize );
        }
        
        createAndWriteToFile( this.fileBytes );
        System.out.println( peerID + " has downloaded the file." );
 		this.hasFile = true;
 	}
 	
 	public void createAndWriteToFile( byte[] bytes ) throws IOException
 	{
 		File file = new File( this.filePath );
 		if ( file.exists() )
 			return;
 		
	    file.createNewFile();
	    BufferedOutputStream output = new BufferedOutputStream( new FileOutputStream(file) );
	    output.write( bytes );
	    output.flush();
	    output.close();
 	}
 	
	// Peer starts listening on a portNr
	Peer( int peerid, String hostname, int portid, boolean hasfile ) throws Exception
	{
		this.peerID = peerid;
		this.hostName = hostname;
		this.listeningPort = portid;
		this.hasFile = hasfile;
		this.log = new Log( this.peerID );
		this.neighbours = new NeighbourPeerInfo( this.peerID );
		
		this.fileDirectoryPath = peerProcess.baseDirectory + "peer_" + this.peerID;
		final File fileDirectory = new File( this.fileDirectoryPath );
		if ( !fileDirectory.exists() )
			fileDirectory.mkdir();
		
		this.filePath = this.fileDirectoryPath + "/" + config.fileName;		
		this.fileBytes = new byte[config.fileSize];
		this.filePieces = new byte[config.nrPieces][];
		this.bitField = new int[config.nrPieces];
		// if ( !this.hasFile )
		//	 Files.deleteIfExists( Paths.get(this.filePath) );
		if ( this.hasFile )
		{
			File file = new File( this.filePath );
			if ( !file.exists() )
			{
				// new RandomAccessFile(file,"rw").setLength( config.fileSize );
		        file.createNewFile();
		        BufferedOutputStream output = new BufferedOutputStream( new FileOutputStream(file) );
		        output.write( config.fileBytes );
		        output.flush();
		        output.close();
		        
		        this.fileBytes = config.fileBytes.clone();
			}
			else
			{
				BufferedInputStream input = new BufferedInputStream( new FileInputStream(this.filePath) );
				input.read( fileBytes );
				input.close();
			}
            
            for ( int idx=0; idx<config.nrPieces; idx++ )
            {
            	final int pieceStart = idx*config.pieceSize;
                filePieces[idx] = Arrays.copyOfRange( fileBytes, pieceStart, pieceStart+config.pieceSize );
            }
            
            Arrays.fill( bitField, 1 );
			this.nrPieces = bitField.length;
		}
		
		new WelcomeSocketHandler( this );
		new UnchokeNeighbours( this );
		this.start();
	}

	// Peer starts receiving and sending messages to another peer which is listening on some other port through a request socket.
	public void run()
	{
		for( int otherPeerID : neighbours.peerInfos().keySet() )
		{
			if ( peerID == otherPeerID )
				break;
			
			//create a socket to connect to the server (other peer)
			final RemotePeerInfo neighbour = neighbours().getPeerInfo( otherPeerID );
			// log().connectionTo( neighbour.peerID );
			try {
				final Socket requestSocket = new Socket( neighbour.peerAddress, neighbour.peerPort );
				new RequestSocketHandler( requestSocket, this, neighbour );
			}
			catch (ConnectException e) {
	    		System.err.println("Connection refused. You need to initiate a server first. " + peerID + " Other: " + otherPeerID);
			} 
			catch(UnknownHostException unknownHost){
				System.err.println("You are trying to connect to an unknown host!");
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
}
