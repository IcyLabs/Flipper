package flipper.targets.penguino;

import flipper.jtag.BitStream;
import flipper.jtag.TAPResponse;
import flipper.jtag.TAPStateMachine;

public class AVRFlash extends Memory {
	private TAPStateMachine tapState;
	
	private AVRChip chip;
	
	public AVRFlash( AVRChip c, int flashSize ) {
		super( );
		
		chip = c;
		
		tapState = chip.tapState;
		
		this.memoryBytes = flashSize;
		this.pageBytes = 128; // FIXME: always the same? 64 words, 128 bytes
	}
	
	public void erase( ) {
		chip.fullChipErase( );
	}
	
	public void writePage( int pageIndex, byte[] data ) {
		chip.enterProgMode( );
		chip.enterProgCommands( );
		
		if ( data.length > pageBytes ) {
			throw new Error( "Data cannot be larger than page size!" );
		}
		
		// create a new byte array that always spans the page
		byte[] fullPage = new byte[pageBytes];
		for ( int i = 0; i < data.length; i++ ) {
			fullPage[i] = data[i];
		}
		
		int address = pageIndex * 0x40;
		
		//System.out.println( "Enter flash write:" );
		tapState.scanDR( 15, 0x2310 ); // 2a. Enter Flash Write
		
		boolean fastMethod = true;
		
		//System.out.printf( "--------------- START DATA PAGE %s ---------------\n", pageIndex );
		if ( fastMethod ) {
			//System.out.println( "Load address high byte:" );
			tapState.scanDR( 15, 0x0700 | ((address>>8)&0xff) ); // 2b. Load Address High Byte
			//System.out.println( "Load address low byte:" );
			tapState.scanDR( 15, 0x0300 | ((address)&0xff) ); // 2c. Load Address Low Byte
		
			//System.out.println( "PROG_PAGELOAD:" );
			tapState.scanIR( 4, 0x6 ); // PROG_PAGELOAD
			
			tapState.scanDR( 1024, fullPage ); // Load Data Page
		} else {
			// slow method, but works
			//System.out.println( "Load address high byte:" );
			tapState.scanDR( 15, 0x0700 | ((address>>8)&0xff) ); // 2b. Load Address High Byte
		
			// 2d. Load Data Low Byte
			for ( int i = 0; i < pageBytes / 2; i++ ) {
				//System.out.println( "Load address low byte:" );
				tapState.scanDR( 15, 0x0300 | ((address+i)&0xff) ); // 2c. Load Address Low Byte
				
				tapState.scanDR( 15, 0x1300 | (fullPage[(i*2)]&0xff) );
				tapState.scanDR( 15, 0x1700 | (fullPage[(i*2) + 1]&0xff) );
				
				// latch
				tapState.scanDR( 15, 0x3700 );
				tapState.scanDR( 15, 0x7700 );
				tapState.scanDR( 15, 0x3700 );
			}
		}
		
		//System.out.println( "--------------- END DATA PAGE ---------------" );
		
		chip.enterProgCommands( );
		
		tapState.scanDR( 15, 0x3700 ); // Write Page
		tapState.scanDR( 15, 0x3500 );
		tapState.scanDR( 15, 0x3700 );
		tapState.scanDR( 15, 0x3700 );
		
		boolean writeComplete = false;
		
		while ( !writeComplete ) {
			// 4g. Poll for Page Write complete
			TAPResponse poll = tapState.scanDR( 15, BitStream.fromString("011011100000000"), true );
			writeComplete = poll.getBit(5); // success stored in magic location
		}
		/*
		try {
			Thread.sleep( 100 );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	public byte[] readPage( int pageIndex ) {
		byte[] dummyData = new byte[pageBytes + 1]; // dummy data
		// AVR returns 1 extra byte at the start
		
		chip.enterProgMode( );
		chip.enterProgCommands( );
		
		int address = pageIndex * 0x40;
		
		tapState.scanDR( 15, 0x2302 ); // 2a. Enter Flash Write
		tapState.scanDR( 15, 0x0700 | ((address>>8)&0xff) ); // 2b. Load Address High Byte
		tapState.scanDR( 15, 0x0300 | ((address)&0xff) ); // 2c. Load Address Low Byte
		
		tapState.scanIR( 4, 0x7 ); // PROG_PAGEREAD
		int numReceivedBits = dummyData.length * 8;
		TAPResponse pageData = tapState.scanDR( numReceivedBits, dummyData, true ); // Load Data Page
		
		byte[] outData = pageData.getBytesFrom( 1 );
		
		return outData;
	}
	
	public void finished( ) {
		chip.exitProgMode( );
	}
}
