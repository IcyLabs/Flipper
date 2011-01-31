package flipper.jtag;

import java.util.BitSet;

public class BitStream {
	private int bitLength;
	private BitSet bitSet;
	
	private BitStream( int bitLength ) {
		this.bitLength = bitLength;
		bitSet = new BitSet( );
	}
	
	public static BitStream ofLength( int bitLength ) {
		return new BitStream( bitLength );
	}
	
	public static BitStream emptyStream( ) {
		return new BitStream( 0 );
	}
	
	public static BitStream fromString( String bits ) {
		return BitStream.emptyStream( ).add( bits );
	}
	
	public static BitStream fromInt( int bits, int numBits ) {
		return BitStream.emptyStream( ).addBits( bits, numBits );
	}
	
	private BitStream addBits(int bits, int numBits) {
		for ( int i = 0; i < numBits; i++ ) {
			set( i, ( bits & (1<<i) ) != 0 );
		}
		return this;
	}
	
	public static BitStream fromBytes(byte[] bytes, int numBits) {
		StringBuffer buf = new StringBuffer( );
		for ( int i = 0; i < numBits; i++ ) {
			int byteIndex = i / 8;
			int bitIndex = i % 8;
			int bitValue = ( 1 << bitIndex );
			
			boolean isSet = ( (bytes[byteIndex] & bitValue) != 0 );
			
			buf.append( isSet ? '1' : '0' );
		}

		return BitStream.fromString( buf.toString() );
	}

	public int length( ) {
		return bitLength;
	}
	
	public BitStream set( int bit, boolean value ) {
		bitSet.set( bit, value );
		
		// expand if we set a bit outside our range
		if ( bit >= bitLength ) {
			bitLength = bit + 1;
		}
		
		return this;
	}
	
	public BitStream set( int bit ) {
		return set( bit, true );
	}
	
	public BitStream add( boolean bit ) {
		// set() automatically increases our size
		set( bitLength, bit );
		return this;
	}
	
	public BitStream add( String bits ) {
		for ( int i = 0; i < bits.length(); i++ ) {
			add( bits.charAt( i ) == '1' );
		}
		return this;
	}
	
	public BitStream get( int startIndex, int length ) {
		return BitStream.fromString( toString().substring( startIndex, startIndex+length ) );
	}
	
	public boolean get( int index ) {
		return bitSet.get( index );
	}
	
	
	public long getInt32( int firstBit ) {
		long temp = 0;
		
		for ( int i = 0; i < 32; i++ ) {
			if ( get( firstBit + i ) ) {
				temp |= 1 << i;
			}
		}
		
		return temp;
	}
	
	public int getInt16( int firstBit ) {
		int temp = 0;
		
		for ( int i = 0; i < 16; i++ ) {
			if ( get( firstBit + i ) ) {
				temp |= 1 << i;
			}
		}
		
		return temp;
	}
	
	public String toString( ) {
		StringBuilder sb = new StringBuilder( );
		
		for ( int i = 0; i < bitLength; i++ ) {
			if ( bitSet.get( i ) ) {
				sb.append( "1" );
			} else {
				sb.append( "0" );
			}
		}
		
		return sb.toString( );
	}
}
