/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE 
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */
package cnt5106c;

public class RemotePeerInfo {
	
	public int peerID;
	public String peerAddress;
	public int peerPort;
	public boolean isUnchoked;
	public boolean isOptimisticallyUnchoked;
	
	public RemotePeerInfo(String pID, String pAddress, String pPort) {
		
		peerID = Integer.parseInt( pID );
		peerAddress = pAddress;
		peerPort = Integer.parseInt( pPort );
		isUnchoked = true;
		isOptimisticallyUnchoked = false;
	}
}