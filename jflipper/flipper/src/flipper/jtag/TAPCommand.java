package flipper.jtag;

import java.util.*;

public class TAPCommand {
	public int bitLength;
	
	public BitStream tmsStream;
	public BitStream dataStream;
	
	public boolean sendData = false;
	public boolean receiveData = false;
	public boolean sendTMS = false;
	
	private TAPCommand( ) {
		
	}
	
	public String toString( ) {
		StringBuilder s = new StringBuilder();
		s.append( "<TAPCommand length=" + bitLength );
		
		if ( sendData ) {
			s.append( " sendData=" );
			s.append( dataStream );
		}
		
		if ( sendTMS ) {
			s.append( " sendTMS=" );
			s.append( tmsStream );
		}
		
		if ( receiveData ) {
			s.append( " receive" );
		}
		
		s.append( ">" );
		return s.toString( );
	}
	
	public static TAPCommand clockTMS( BitStream tms ) {
		return sendData( tms.length( ), null, tms, false );
	}
	
	public static TAPCommand clockTMS( String bits ) {
		return sendData( bits.length( ), null, BitStream.fromString( bits ), false );
	}
	
	public static TAPCommand sendData( int bitLength, BitStream data, BitStream tms, boolean receive ) {
		TAPCommand cmd = new TAPCommand( );
		
		cmd.bitLength = bitLength;
		cmd.tmsStream = tms;
		cmd.dataStream = data;
		
		cmd.sendData = ( data != null );
		cmd.receiveData = receive;
		cmd.sendTMS = ( tms != null );
		
		return cmd;
	}
	
	public static TAPCommand receiveData( int bitLength ) {
		return sendData( bitLength, null, null, true );
	}
	
	public int neededBytes( ) {
		int byteCount = bitLength / 8;
		
		if ( (bitLength % 8) > 0 )
			byteCount++;
		
		//writefln( "{0} bits need {1} bytes", bitLength, byteCount );
		
		return byteCount;
	}
	
	public boolean getDataBit( int index ) {
		if ( dataStream == null ) {
			return false;
		}
		
		return dataStream.get( index );
	}
	
	public boolean getTMSBit( int index ) {
		if ( tmsStream == null ) {
			return false;
		}
		
		return tmsStream.get( index );
	}
	
	// Splits a TAPCommand with double transitions (where both TMS and TDI change)
	// into multiple TAPCommands, each with only single transitions
	// (TMS or TDI stay stable the whole duration)
	public List<TAPCommand> splitByDoubleTransitions( ) {
		boolean splitNeeded = true;
		
		//if ( (method & (Method.SendData | Method.SendTMS)) != (Method.SendData | Method.SendTMS) ) {
		if ( !sendData || !sendTMS ) {
			// skip complex checking if TMS and TDI are not both being used
			splitNeeded = false;
		}
		
		if ( bitLength == 1 ) {
			// skip complex checking if we're only 1 bit long
			splitNeeded = false;
		}
		
		ArrayList<TAPCommand> cmdList = new ArrayList<TAPCommand>();
		
		if ( !splitNeeded ) {
			//TAPCommand[] tapCommands = new TAPCommand[1];
			//tapCommands[0] = this;
			//return tapCommands;
			cmdList.add( this );
			return cmdList;
		}
		
		System.out.println( "Preparing to split..." );
		//writefln( "Preparing to split..." );
		
		int currentBit = 0;
		boolean transitionFound = false;
		boolean transitionIsTMS = false;
		
		boolean currentData, currentTMS;
		int tmpBit;
		
		while ( currentBit < bitLength ) {
			if ( !transitionFound ) {
				// find a transition by skipping ahead until 1 bit changes
				currentData = getDataBit( currentBit );
				currentTMS = getTMSBit( currentBit );
				
				tmpBit = currentBit + 1;
				while ( tmpBit < bitLength ) {
					if ( currentData != getDataBit( tmpBit ) ) {
						// data bit changed first
						transitionFound = true;
						transitionIsTMS = false;
						
						break;
					} else if ( currentTMS != getTMSBit( currentBit ) ) {
						// TMS bit changed first
						transitionFound = true;
						transitionIsTMS = true;
						
						break;
					}
					
					tmpBit++;
				}
			}
			
			// we now have found the first transition (defined by transitionIsTMS).. 
			// now do another scan, looking as far as we can until the OTHER value changes
			currentData = getDataBit( currentBit );
			currentTMS = getTMSBit( currentBit );
			
			tmpBit = currentBit + 1;
			while ( tmpBit < bitLength ) {
				if ( (transitionIsTMS && currentData != getDataBit( tmpBit )) ||
				 	 ((!transitionIsTMS) && currentTMS != getTMSBit( tmpBit )) ) {
					// double transition found at tmpBit
					
					System.out.println( "Double transition found at " + tmpBit );
					//writefln( "Double transition found at {0}", tmpBit );
					
					int numBits = tmpBit - currentBit;
					TAPCommand cmd = this.subCommand( currentBit, numBits );
					cmdList.add( cmd );
					
					// continue from that bit
					transitionFound = false;
					currentBit = tmpBit;
					break;
				}
				
				tmpBit++;
			}
			
			if ( tmpBit == bitLength ) {
				int numBits = tmpBit - currentBit;
				TAPCommand cmd = this.subCommand( currentBit, numBits );
				cmdList.add( cmd );
				
				break;
			}
		}
		
		//writefln( "Split {0} bits into {1} parts", bitLength, cmdList.Count );
		
		return cmdList;
	}
	
	public TAPCommand subCommand( int fromIndex, int numBits ) {
		TAPCommand cmd = new TAPCommand( );
		cmd.bitLength = numBits;
		cmd.tmsStream = tmsStream.get( fromIndex, numBits );
		cmd.dataStream = dataStream.get( fromIndex, numBits );
		cmd.sendData = sendData;
		cmd.receiveData = receiveData;
		cmd.sendTMS = sendTMS;
		return cmd;
	}
}
