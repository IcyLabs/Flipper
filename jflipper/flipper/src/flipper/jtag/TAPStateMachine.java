package flipper.jtag;

public class TAPStateMachine {
	private IJTAGDevice iface;
	
	private TAPState currentState = TAPState.Unknown;
	
	public TAPStateMachine( IJTAGDevice iface ) {
		this.iface = iface;
	}
	
	public TAPResponse sendCommand( TAPCommand cmd ) {
		TAPResponse ret = iface.sendJTAGCommand( cmd );
		
		// if we're in an unknown state, we'll remain there
		if ( currentState != TAPState.Unknown ) {
			for ( int i = 0; i < cmd.bitLength; i++ ) {
				currentState = TAPStateTree.nextStateForTMS( currentState, cmd.getTMSBit( i ) );
			}
		}
		
		//writefln( "State: {0}", currentState );
		
		return ret;
	}
	
	public void gotoState( TAPState newState ) {
		if ( currentState == TAPState.Unknown ) {
			// when in an unknown state, start by returning to the reset state
			// by clocking 5 bits with TMS=1. Then set our state, because we know it.
			sendCommand( TAPCommand.clockTMS( "11111" ) );
			
			currentState = TAPState.TestLogicReset;
		}
		
		if ( currentState != newState ) {
			BitStream path = TAPStateTree.getShortestStatePath( currentState, newState );
			sendCommand( TAPCommand.clockTMS( path ) );
		}
	}
	
	public TAPResponse scanIR( int bitLength, BitStream dataBits, boolean receive ) {
		gotoState( TAPState.ShiftIR ); // get to ShiftIR
		BitStream tmsStream = BitStream.ofLength( bitLength ).set( bitLength - 1 );
		TAPResponse response = sendCommand( TAPCommand.sendData( bitLength, dataBits, tmsStream, receive ) ); // TMS on last bit only
		gotoState( TAPState.RunTestIdle ); // return to RunTestIdle
		return response;
	}
	
	public TAPResponse scanIR( int bitLength, BitStream dataBits ) {
		return scanIR( bitLength, dataBits, false );
	}
	
	public TAPResponse scanDR( int bitLength, BitStream dataBits, boolean receive ) {
		gotoState( TAPState.ShiftDR ); // get to ShiftIR
		BitStream tmsStream = BitStream.ofLength( bitLength ).set( bitLength - 1 );
		TAPResponse response = sendCommand( TAPCommand.sendData( bitLength, dataBits, tmsStream, receive ) ); // TMS on last bit only
		gotoState( TAPState.RunTestIdle ); // return to RunTestIdle
		return response;
	}
	
	public TAPResponse scanDR( int bitLength, BitStream dataBits ) {
		return scanDR( bitLength, dataBits, false );
	}

	public TAPResponse scanDR(int numBits, int bits) {
		return scanDR( numBits, bits, false );
	}

	public TAPResponse scanIR(int numBits, int bits) {
		//System.out.printf( "numBits=%d, bits=%x, stream=%s\n", numBits, bits, BitStream.fromInt(bits, numBits) );
		return scanIR( numBits, BitStream.fromInt(bits, numBits), false );
	}

	public TAPResponse scanDR(int numBits, int bits, boolean receive) {
		return scanDR( numBits, BitStream.fromInt(bits, numBits), receive );
	}

	public TAPResponse scanDR(int numBits, byte[] bytes) {
		return scanDR( numBits, bytes, false );
	}

	public TAPResponse scanDR(int numBits, byte[] bytes, boolean receive) {
		return scanDR( numBits, BitStream.fromBytes(bytes, numBits), receive );
	}
}