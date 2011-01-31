package flipper.jtag;

import java.util.HashMap;
import java.util.LinkedList;

public class TAPStateTree {
	private static HashMap<TAPState,TAPStateTreeNode> nodes = new HashMap<TAPState,TAPStateTreeNode>( );
	
	static {
		//                            	         			    TMS=1                       TMS=0
		nodes.put(TAPState.TestLogicReset,  new TAPStateTreeNode( TAPState.TestLogicReset,	TAPState.RunTestIdle ) );
		nodes.put(TAPState.RunTestIdle,  new TAPStateTreeNode( TAPState.SelectDRScan,		TAPState.RunTestIdle ) );
		      
		nodes.put(TAPState.SelectDRScan,  new TAPStateTreeNode( TAPState.SelectIRScan,		TAPState.CaptureDR ) );
		nodes.put(TAPState.CaptureDR,  new TAPStateTreeNode( TAPState.Exit1DR,			TAPState.ShiftDR ) );
		nodes.put(TAPState.ShiftDR,  new TAPStateTreeNode( TAPState.Exit1DR,			TAPState.ShiftDR ) );
		nodes.put(TAPState.Exit1DR,  new TAPStateTreeNode( TAPState.UpdateDR,			TAPState.PauseDR ) );
		nodes.put(TAPState.PauseDR,  new TAPStateTreeNode( TAPState.Exit2DR,			TAPState.PauseDR ) );
		nodes.put(TAPState.Exit2DR,  new TAPStateTreeNode( TAPState.UpdateDR,			TAPState.ShiftDR ) );
		nodes.put(TAPState.UpdateDR,  new TAPStateTreeNode( TAPState.SelectDRScan,		TAPState.RunTestIdle ) );
		      
		nodes.put(TAPState.SelectIRScan,  new TAPStateTreeNode( TAPState.TestLogicReset,	TAPState.CaptureIR ) );
		nodes.put(TAPState.CaptureIR,  new TAPStateTreeNode( TAPState.Exit1IR,			TAPState.ShiftIR ) );
		nodes.put(TAPState.ShiftIR,  new TAPStateTreeNode( TAPState.Exit1IR,			TAPState.ShiftIR ) );
		nodes.put(TAPState.Exit1IR,  new TAPStateTreeNode( TAPState.UpdateIR,			TAPState.PauseIR ) );
		nodes.put(TAPState.PauseIR,  new TAPStateTreeNode( TAPState.Exit2IR,			TAPState.PauseIR ) );
		nodes.put(TAPState.Exit2IR,  new TAPStateTreeNode( TAPState.UpdateIR,			TAPState.ShiftIR ) );
		nodes.put(TAPState.UpdateIR,  new TAPStateTreeNode( TAPState.SelectDRScan,		TAPState.RunTestIdle ) );
	}
	
	// returns (via path/length) the best TMS path from the state 'from' to the state 'to'.
	// the returned path is from LSB->MSB, that is, the first state change is in the LSB.
	static public BitStream getShortestStatePath( TAPState from, TAPState to ) {
		LinkedList<TAPStateTreeSearchState> queue = new LinkedList<TAPStateTreeSearchState>();
		queue.add( new TAPStateTreeSearchState( from ) );
		
		while ( queue.size() > 0 ) {
			TAPStateTreeSearchState ss = queue.removeFirst( );
			
			TAPState currentState = ss.currentState( );
			
			if ( currentState == to ) {
				BitStream path = BitStream.emptyStream( );
				
				// skip 'to', because we just need the transition, return the required TMS changes
				// (this uses the reverse order for convenience in shifting the changes in)
				for ( int i = 0; i < ss.path.length - 1; i++ ) {
					TAPState state = ss.path[i];
					TAPStateTreeNode node = nodes.get(state);
					TAPState nextState = ss.path[i+1];
					
					// if the next state is the node's highTMS, set the bit
					//path.set( length, ( nextState == node.highTMS ) );
					path.add( nextState == node.highTMS );
				}
				
				return path;
			}
			
			queue.add( ss.fork( nodes.get(currentState).highTMS ) );
			queue.add( ss.fork( nodes.get(currentState).lowTMS ) );
		}
		
		return null;
	}
	
	public static TAPState nextStateForTMS( TAPState current, boolean tms ) {
		if ( tms ) {
			return nodes.get(current).highTMS;
		} else {
			return nodes.get(current).lowTMS;
		}
	}
}
