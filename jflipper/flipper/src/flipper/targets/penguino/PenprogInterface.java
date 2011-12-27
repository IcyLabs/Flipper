package flipper.targets.penguino;

import flipper.jtag.*;
import flipper.usb.*;

/*

The firmware expects a string of bits in the format:

data[30]:

              data[0]           data[1]           data[2]     <-- array indexes
bit #    8 7 6 5 4 3 2 1   8 7 6 5 4 3 2 1   8 7 6 5 4 3 2 1  <-- in memory
bit val ......16 8 4 2 1  ......16 8 4 2 1  ......16 8 4 2 1  
         S I S I S I S I   S I S I S I S I   S I S I S I S I  <-- S=TMS, I=TDI
         3 3 2 2 1 1 0 0   7 7 6 6 5 5 4 4  11111010 9 9 8 8  <-- order of output

*/

public class PenprogInterface implements IJTAGDevice {
	@SuppressWarnings("unused")
	private static final byte jtagCommandGetBoard = 0x01;
	private static final byte jtagCommandReset = 0x02;
	private static final byte jtagCommandJumpBootloader = 0x03;

	@SuppressWarnings("unused")
	private static final byte jtagCommandClockBit = 0x20;
	private static final byte jtagCommandClockBits = 0x21;
	
	
	private static final int jtagBulkIn = 0x83;
	private static final int jtagBulkOut = 0x03;
	
	private static final int jtagInterface = 2;
	
	private USBDevice device;
	
	public PenprogInterface( USBDevice device ) {
		this.device = device;
		
		System.out.println( "PenprogInterface has device: " + device );
		
		device.open( );
		try {
			device.claimInterface( jtagInterface );
		} catch ( USBException e ) {
			e.printStackTrace( );
		}
	}
	
	protected void finalize() {
		System.out.println( "Finalizing PenProgInterface" );
		if ( device != null ) {
			System.out.println( "PenprogInterface was finalized without being closed first, cleaning up..." );
			close( );
		}
	}
	
	public void close() {
		if ( device != null ) {
			try {
				device.releaseInterface( jtagInterface );
			} catch ( USBException e ) {
				e.printStackTrace( );
			}
		}
		
		device = null;
	}
	
	public void sendJTAGReset( boolean systemReset, boolean testReset ) {
		byte[] bytes = new byte[32];
		
		bytes[0] = jtagCommandReset; // RESET
		bytes[1] = (byte)(systemReset ? 1 : 0);
		bytes[2] = (byte)(testReset ? 1 : 0);
		
		bulkWriteSafe( jtagBulkOut, bytes );
		bulkReadSafe( jtagBulkIn, bytes );
	}
	
	// usb msg = 32 bytes, 2 instruction bytes, then 4 bits per byte
	// (because 2 physical bits per logical bit/clock)
	private static final int USBMessageSize = 32;
	private static final int JTAGClockHeaderBytes = 2;
	private static final int JTAGClockBitsPerTransition = 2;
	private static final int JTAGClockBitsPerByte = 8 / JTAGClockBitsPerTransition;
	private static final int MaxBytesPerMessage = USBMessageSize - JTAGClockHeaderBytes;
	private static final int MaxTransitionsPerMessage = MaxBytesPerMessage * JTAGClockBitsPerByte;
	
	private static final int BitsPerByte = 8;
	
	private byte[] bitStreamToMessage( BitStream stream, byte[] bytes, int startIndex ) {
		int numBits = stream.length( );
		
		for ( int i = 0; i < numBits; i++ ) {
			int byteIndex = ( i / BitsPerByte );
			int bitIndex = ( i % BitsPerByte );
			if ( stream.get( i ) ) {
				bytes[startIndex + byteIndex] |= ( 1 << bitIndex );
			}
		}
		
		return bytes;
	}
	
	@SuppressWarnings("unused")
	private byte[] bitStreamToMessage( BitStream stream ) {
		int numBits = stream.length( );
		int numBytes = numBits / BitsPerByte;
		if ( numBytes * BitsPerByte < numBits ) {
			numBytes++; // round up
		}
		
		byte[] bytes = new byte[numBytes]; // bytes required, rounded up
		
		return bitStreamToMessage( stream, bytes, 0 );
	}
	
	private void responseToBitStream( byte[] bytes, int startByte, BitStream response, int startIndex, int numBits ) {
		for ( int i = 0; i < numBits; i++ ) {
			int byteIndex = ( i / BitsPerByte );
			int bitIndex = ( i % BitsPerByte );
			
			boolean value = ( bytes[startByte+byteIndex] & (1 << bitIndex) ) != 0;
			
			response.set( startIndex+i, value );
		}
	}
	
	private void processChunk( TAPCommand cmd, BitStream response, int startIndex, int numBits ) {
		BitStream outData = BitStream.emptyStream( );
		//System.out.printf( "Processing chunk: %s from %d for %d bits\n", cmd, startIndex, numBits );
		
		// prepare our output data
		for ( int i = startIndex; i < startIndex+numBits; i++ ) {
			boolean dataBit = cmd.getDataBit(i);
			boolean tmsBit = cmd.getTMSBit(i);
			
			//outData.setBit( i - startIndex, dataBit, tmsBit );
			outData.add( dataBit );
			outData.add( tmsBit );
			
			//System.out.printf( "%d: %b, %b\n", i, dataBit, tmsBit );
		}
		
		//System.out.println( "out: " + outData );
		
		byte[] usbMessage = new byte[USBMessageSize];
		usbMessage[0] = jtagCommandClockBits;
		usbMessage[1] = (byte)numBits;
		bitStreamToMessage( outData, usbMessage, 2 );
		
		// write to usb!
		bulkWriteSafe( jtagBulkOut, usbMessage );
		
		// read back the response
		byte[] responseBytes = bulkReadSafe( jtagBulkIn, 32 );
		
		responseToBitStream( responseBytes, 2, response, startIndex, numBits );
	}
	
	private void bulkWriteSafe( int endpoint, byte[] bytes ) {
		int ret = 0;
		
		/*
		System.out.println( "Last error: " + device.lastError() );
		System.out.println( "Oh hai!" );
		System.out.println( endpoint );
		for (int j=0; j<bytes.length; j++) {
			System.out.format("%02X ", bytes[j]);
		}
		System.out.println();
		*/
		while ( (ret = device.bulkWrite( endpoint, bytes )) != bytes.length ) {
			//reportUSBError( "USB Bulk Write (chunk)", device, ret );
			System.out.println( "Last error: " + device.lastError() );
			System.out.println( "Safe USB Bulk Write failed (" + ret + "), retrying... " );
			
			device.clearHalt( endpoint );
		}
	}
	
	private byte[] bulkReadSafe( int endpoint, byte[] buf ) {
		int ret;
		
		while ( (ret = device.bulkRead( endpoint, buf )) != buf.length ) {
			//reportUSBError( "USB Bulk Read (chunk)", device, ret );
			System.out.println( "Safe USB Bulk Read failed (" + ret + "), retrying... " );
			
			device.clearHalt( endpoint );
		}
		
		return buf;
	}
	
	private byte[] bulkReadSafe( int endpoint, int numBytes ) {
		byte[] bytes = new byte[numBytes];
		
		return bulkReadSafe( endpoint, bytes );
	}
	
	public TAPResponse sendJTAGCommand( TAPCommand cmd ) {
		//int numBytes = cmd.neededBytes( );
		//byte[] responseBytes = new byte[numBytes];
		
		BitStream response = BitStream.emptyStream( );
		
		int bitsRemaining = cmd.bitLength;
		
		int startIndex = 0;
		while ( startIndex < cmd.bitLength ) {
			int numBitsToSend = bitsRemaining;
			
			if ( numBitsToSend > MaxTransitionsPerMessage )
				numBitsToSend = MaxTransitionsPerMessage;
			
			processChunk( cmd, response, startIndex, numBitsToSend );
			
			startIndex += numBitsToSend;
			bitsRemaining -= numBitsToSend;
		}
		
		return new TAPResponse( response );
	}
	
	@SuppressWarnings("unused")
	private void enterDFUMode( ) {
		byte[] bytes = new byte[32];
		//int ret;
		
		bytes[0] = jtagCommandJumpBootloader;
		
		bulkWriteSafe( jtagBulkOut, bytes );
	}
}
