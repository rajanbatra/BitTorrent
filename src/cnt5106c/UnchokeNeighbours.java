package cnt5106c;

public class UnchokeNeighbours extends Thread {

	private Peer peer;
	
	UnchokeNeighbours( Peer peer ) {
		
		this.peer = peer;
		this.start();
	}
	
	public void run() {
		
		int totalnrseconds = 0;
		while ( true )
		{
			final int nrsleepseconds = peer.config().unchokingInterval;
			try {
				Thread.sleep( nrsleepseconds * 1000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			unchokeRandomNeighbour();
			totalnrseconds += nrsleepseconds;
			if ( (totalnrseconds%peer.config().optimisticUnchokingInterval) == 0 )
				optimisticallyUnchokeRandomNeighbour();
		}
	}
	
	private void unchokeRandomNeighbour() {
		
		while ( peer.nrConnectedNeighbours > peer.config().nrPreferredNeighbors )
		{
			RemotePeerInfo randomNeighbour = peer.neighbours().randomNeighbour();
			if ( peer.peerID()<randomNeighbour.peerID || randomNeighbour.isUnchoked )
				{ randomNeighbour.isUnchoked = false; peer.nrConnectedNeighbours--; }
		}
		
		RemotePeerInfo randomNeighbour = peer.neighbours().randomNeighbour();
		while ( true )
		{
			if ( peer.peerID()>randomNeighbour.peerID && randomNeighbour.isUnchoked )
				{ randomNeighbour.isUnchoked = false; break; }
			else
				randomNeighbour = peer.neighbours().randomNeighbour();
		}
		
		randomNeighbour = peer.neighbours().randomNeighbour();
		while ( true )
		{
			if ( peer.peerID()>randomNeighbour.peerID && !randomNeighbour.isUnchoked )
			{
				randomNeighbour.isUnchoked = true;
				final int nrpreferredneighbours = peer.config().nrPreferredNeighbors;
				int ids[] = new int[nrpreferredneighbours];
				int count = 0;
				for ( Integer key : peer.neighbours().peerInfos().keySet() )
					if ( peer.neighbours().getPeerInfo(key).isUnchoked && count<nrpreferredneighbours )
						ids[count++] = key;
				
				peer.log().changePreferredNeighbors( ids );
				break;
			}
			else
				randomNeighbour = peer.neighbours().randomNeighbour();
		}
	}
	
	private void optimisticallyUnchokeRandomNeighbour() {
		
		RemotePeerInfo randomNeighbour = peer.neighbours().randomNeighbour();
		while ( true )
		{
			if ( peer.peerID()>randomNeighbour.peerID && randomNeighbour.isOptimisticallyUnchoked )
				{ randomNeighbour.isOptimisticallyUnchoked = false; break; }
			else
				randomNeighbour = peer.neighbours().randomNeighbour();
		}
		
		randomNeighbour = peer.neighbours().randomNeighbour();
		while ( true )
		{
			if ( peer.peerID()>randomNeighbour.peerID && !randomNeighbour.isOptimisticallyUnchoked )
			{
				randomNeighbour.isOptimisticallyUnchoked = true;
				peer.log().changeOptimisticallyUnchokedNeighbor( randomNeighbour.peerID );
				break;
			}
			else
				randomNeighbour = peer.neighbours().randomNeighbour();
		}
	}
}
