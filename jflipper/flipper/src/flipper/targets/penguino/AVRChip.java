package flipper.targets.penguino;

import flipper.jtag.TAPResponse;
import flipper.jtag.TAPState;
import flipper.jtag.TAPStateMachine;

public class AVRChip extends Chip {
	boolean _avrReset = false;
	boolean _progEnable = false;
	
	public AVRChip(TAPStateMachine stateMachine) {
		super( stateMachine );
		
		showInformation();
	}

	public byte readFuseLow() {
		enterProgMode( );
		enterProgCommands( );
		
		tapState.scanDR( 15, 0x2304 ); // 8a. Enter Fuse/Lock Bit Read
		tapState.scanDR( 15, 0x3200 ); // 8c. Read Fuse Low Byte
		TAPResponse response = tapState.scanDR( 15, 0x3300 );
		
		return (byte) (response.getInt16( ) & 0xFF);
	}

	public byte readFuseHigh() {
		enterProgMode( );
		enterProgCommands( );
		
		tapState.scanDR( 15, 0x2304 ); // 8a. Enter Fuse/Lock Bit Read
		tapState.scanDR( 15, 0x3E00 ); // 8c. Read Fuse Low Byte
		TAPResponse response = tapState.scanDR( 15, 0x3F00 );
		
		return (byte) (response.getInt16( ) & 0xFF);
	}

	public void exitProgMode() {
		enterProgCommands( );
		
		tapState.scanDR( 15, 0x2300 ); // 11a. Load No Operation Command
		tapState.scanDR( 15, 0x3300 );
		
		// order is important
		setProgEnable( false );
		setAVRReset( false );
	}
	
	
	
	private void setAVRReset(boolean val) {
		if ( _avrReset == val ) {
			return;
		}
		
		_avrReset = val;
		
		//System.out.println( "Sending AVR Reset... " + _avrReset );
		tapState.gotoState( TAPState.TestLogicReset );
		tapState.scanIR( 4, 0xc );
		
		if ( val ) {
			tapState.scanDR( 1, 0x01 );
		} else {
			tapState.scanDR( 1, 0x00 );
		}
	}

	private void setProgEnable(boolean val) {
		if ( _progEnable == val ) {
			return;
		}
		
		_progEnable = val;
		
		//System.out.println( "Sending ProgEnable... " + _progEnable );
		tapState.gotoState( TAPState.TestLogicReset );
		tapState.scanIR( 4, 0x4 );
		
		if ( val ) {
			tapState.scanDR( 16, 0xa370 );
		} else {
			tapState.scanDR( 16, 0x0000 );
		}
	}
	
	public void enterProgMode( ) {
		//System.out.println( "--- enter prog mode ---" );
		// order is important
		setAVRReset( true );
		setProgEnable( true );
	}
	
	public void enterProgCommands( ) {
		//System.out.println( "Enter prog commands..." );
		// PROG_COMMANDS
		tapState.scanIR( 4, 0x5 );
	}
	
	public void writeFuseLow( int fuseValue ) {
		enterProgMode( );
		enterProgCommands( );
		
		System.out.printf( "fuseL=%s\n", fuseValue );
		
		tapState.scanDR( 15, 0x2340 ); // 6a. Enter Fuse Write
		tapState.scanDR( 15, 0x1300 | fuseValue ); // 6e. Load Data Low Byte
		tapState.scanDR( 15, 0x3300 ); // 6f. Write Fuse Low byte 
		tapState.scanDR( 15, 0x3100 );
		tapState.scanDR( 15, 0x3300 );
		tapState.scanDR( 15, 0x3300 );
		
		// FIXME: Should do "6g. Poll for Fuse Write complete" here
		
		try {
			Thread.sleep( 500 );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeFuseHigh( int fuseValue ) {
		enterProgMode( );
		enterProgCommands( );
		
		System.out.printf( "fuseH=%s\n", fuseValue );
		
		final byte JTAGEN = (1<<6);
		
		// we don't want JTAG disabled. ever, ever, ever. catch it, just in case :)
		assert (fuseValue & JTAGEN) == 0: "I don't think you really want JTAG disabled.";
		// i don't even trust assert always being right. bricking sucks.
		//   ahahaha, especially now this is java and it is stupid with having -ea
		if ( !((fuseValue & JTAGEN) == 0) ) {
			throw new Error( "NO BRICKING" );
		}
		
		tapState.scanDR( 15, 0x2340 ); // 6a. Enter Fuse Write
		tapState.scanDR( 15, 0x1300 | fuseValue ); // 6b. Load Data Low Byte
		tapState.scanDR( 15, 0x3700 ); // 6c. Write Fuse High byte
		tapState.scanDR( 15, 0x3500 );
		tapState.scanDR( 15, 0x3700 );
		tapState.scanDR( 15, 0x3700 );
		
		// FIXME: Should do "6d. Poll for Fuse Write complete" here
		
		try {
			Thread.sleep( 500 );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void fullChipErase( ) {
		enterProgMode( );
		enterProgCommands( );
		
		tapState.scanDR( 15, 0x2380 ); // Chip Erase
		tapState.scanDR( 15, 0x3180 );
		tapState.scanDR( 15, 0x3380 );
		tapState.scanDR( 15, 0x3380 );
		
		try {
			Thread.sleep( 10 );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		exitProgMode( );
	}

	@Override
	public void showInformation() {
		enterProgMode( );
		enterProgCommands( );
		
		TAPResponse response;
		
		tapState.scanDR( 15, 0x2308 ); // 9a. Enter Signature Byte Read
		
		tapState.scanDR( 15, 0x0300 ); // 9b. Load Address Byte 0x00
		tapState.scanDR( 15, 0x3200 ); // 9c. Read Signature Byte
		response = tapState.scanDR( 15, 0x3300, true );
		System.out.printf( "manu: %x\n", response.getInt16( ) & 0xff );
		if ( (response.getInt16( ) & 0xFF) == 0x1E ) {
			System.out.println( "Manufacturer: Atmel [0x1E]" );
		} else {
			throw new Error( "I don't understand other parts, sorry!" );
		}
		
		tapState.scanDR( 15, 0x0301 ); // 9b. Load Address Byte 0x01
		tapState.scanDR( 15, 0x3200 ); // 9c. Read Signature Byte
		response = tapState.scanDR( 15, 0x3300, true );
		System.out.printf( "flash: %x\n", response.getInt16( ) & 0xff );
		if ( (response.getInt16( ) & 0xFF) == 0x95 ) {
			System.out.println( "Flash capacity: 32KB [0x95]" );
		} else {
			throw new Error( "I don't understand other parts, sorry!" );
		}
		
		tapState.scanDR( 15, 0x0302 ); // 9b. Load Address Byte 0x02
		tapState.scanDR( 15, 0x3200 ); // 9c. Read Signature Byte
		response = tapState.scanDR( 15, 0x3300, true );
		System.out.printf( "part: %x\n", response.getInt16( ) & 0xff );
		if ( (response.getInt16( ) & 0xFF) == 0x02 ) {
			System.out.println( "Part: ATMega32A [0x02]" );
		} else {
			throw new Error( "I don't understand other parts, sorry!" );
		}
		
		exitProgMode( );
	}

}
