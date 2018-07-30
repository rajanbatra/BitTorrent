package cnt5106c;

import cnt5106c.peerProcess;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

class Log {
	
	private File logFile;
	private String logFilePath;
	private int peerID;
	
	private DateFormat timeFormat = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" );
    private BufferedWriter writer;
    
	Log( int peerid ) throws IOException {
		
		this.peerID = peerid;
		this.logFilePath = peerProcess.baseDirectory + "log_peer_" + this.peerID + ".log";
		
		// Files.deleteIfExists( Paths.get(this.logFilePath) );
		logFile = new File( this.logFilePath );
		if ( logFile.exists() )
			logFile.createNewFile();
        
        writer = new BufferedWriter( new FileWriter(logFile.getAbsolutePath(),true) );
        writer.flush();
	}
	
	private StringBuffer createStringBuffer() {
		
        return new StringBuffer( timeFormat.format(new Date()) + ": Peer " + peerID );
	}
	
	private synchronized void writeLog( StringBuffer log ) {
		
		try{
            writer.write( log.toString() );
            writer.newLine();
            writer.flush();
        }
        catch(Exception e){
        	e.printStackTrace();
        }
	}
	
	public void connectionTo( int otherPeerID ) {
        
        StringBuffer log = createStringBuffer();
        log.append(" makes a connection to Peer " + otherPeerID + ".");
        writeLog( log );
    }

    public void connectionFrom( int otherPeerID ) {

    	StringBuffer log = createStringBuffer();
        log.append(" is connected from Peer " + otherPeerID + ".");
        writeLog( log );
    }

    public void changePreferredNeighbors( int[] ids ) {

    	StringBuffer log = createStringBuffer();
        log.append(" has the preferred neighbors ");
        for ( int id : ids )
            log.append( id + "," );
        
        log.deleteCharAt( log.length()-1 );
        log.append(".");
        writeLog( log );
    }

    public void changeOptimisticallyUnchokedNeighbor( int otherPeerID ) {

    	StringBuffer log = createStringBuffer();
        log.append(" has the optimistically unchoked neighbor " + otherPeerID + ".");
        writeLog( log );
    }

    public void unchokedBy( int otherPeerID ) {

    	StringBuffer log = createStringBuffer();
        log.append(" is unchoked by " + otherPeerID + ".");
        writeLog( log );
    }

    public void chokedBy( int otherPeerID ) {

    	StringBuffer log = createStringBuffer();
        log.append(" is choked by " + otherPeerID + ".");
        writeLog( log );
    }

    public void receivedHaveFrom( int otherPeerID, int index ) {

    	StringBuffer log = createStringBuffer();
        log.append(" received the 'have' message from " + otherPeerID + " for the piece " + index + ".");
        writeLog( log );
    }

    public void receivedInterestedFrom( int otherPeerID ) {
    	
    	StringBuffer log = createStringBuffer();
        log.append(" received the 'interested' message from " + otherPeerID + ".");
        writeLog( log );
    }

    public void receivedNotInterestedFrom( int otherPeerID ) {

    	StringBuffer log = createStringBuffer();
        log.append(" received the 'not interested' message from " + otherPeerID + ".");
        writeLog( log );
    }

    public void downloadedPiece( int otherPeerID, int index, int numOfPieces ) {

    	StringBuffer log = createStringBuffer();
        log.append(" has downloaded the piece " + index + " from " + otherPeerID + "."); // log.append( System.lineSeparator() );
        log.append(" Now the number of pieces it has is " + numOfPieces + ".");
        writeLog( log );
    }

    public void downloadCompleted() {

    	StringBuffer log = createStringBuffer();
        log.append(" has downloaded the complete file.");
        writeLog( log );
    }
}
