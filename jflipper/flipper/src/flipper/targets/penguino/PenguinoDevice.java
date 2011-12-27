package flipper.targets.penguino;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import flipper.FlipperDevice;
import flipper.ProgramBinary;
import flipper.StatusDisplay;
import flipper.jtag.TAPCommand;
import flipper.jtag.TAPResponse;
import flipper.jtag.TAPState;
import flipper.jtag.TAPStateMachine;
import flipper.usb.USBDevice;

public class PenguinoDevice extends FlipperDevice {
	
	private PenprogInterface penprog;
	private TAPStateMachine sm;
	
	private ProgressBar uploadProgress;
	private Label uploadStatus;
	
	public PenguinoDevice(USBDevice device) {
		penprog = new PenprogInterface( device );
		
		idcodeDebug( );
	}
	
	class UploadButtonHandler implements Listener {
		private Composite tabFolder;
		
		public UploadButtonHandler( Composite folder ) {
			tabFolder = folder;
		}
		
		public void handleEvent(Event arg0) {
			FileDialog fileDialog = new FileDialog( tabFolder.getShell(), SWT.OPEN );
			fileDialog.setText( "Open" );
	        String[] filterExt = { "*.bin", "*.hex" };
	        fileDialog.setFilterExtensions(filterExt);
	        String selected = fileDialog.open();
	        
	        if ( selected != null ) {
	        	SWTStatusDisplay statusDisplay = new SWTStatusDisplay( uploadProgress, uploadStatus );
	        	simpleProgramDevice( selected, statusDisplay );
	        }
		}
	}
	
	private AVRChip getChip( ) {
		AVRChip chip = new AVRChip( sm );
		chip.addMemory( "flash", new AVRFlash( chip, 32*1024 ) );
		return chip;
	}
	
	@Override
	public void programDeviceWithFile(String filename, StatusDisplay statusDisplay) {
		simpleProgramDevice( filename, statusDisplay );
	}
	
	private void simpleProgramDevice( String filename, StatusDisplay statusDisplay ) {
		System.out.println( "Uploading file: " + filename );
		
		ProgramBinary program = ProgramBinary.loadFromPath( filename );
		
		if ( program == null )
			return;
		
		AVRChip chip = getChip();
		Memory targetMemory = chip.getMemory( "flash" );
		
		statusDisplay.setStatus( "Erasing flash..." );
		targetMemory.erase( );
		
		statusDisplay.setStatus( "Writing flash..." );
		targetMemory.writeStream( program, statusDisplay );
		
		statusDisplay.setStatus( "Verifying flash..." );
		boolean success = targetMemory.verifyStream( program, statusDisplay );
		
		targetMemory.finished( );
		
		if ( success && BOOTRST != null ) { // make sure we have a UI before we do this
			doFuseBits( statusDisplay );
		}
	}
	
	void doFuseBits( StatusDisplay statusDisplay ) {
		statusDisplay.setStatus( "Preparing for post-flashing..." );
		statusDisplay.setIndeterminate( true );
		statusDisplay.setAnimating( true );
		
		AVRChip chip = getChip();
		
		statusDisplay.setStatus( "Updating fuse bit (L)..." );
		System.out.printf( "Low fuse bit: %x\n", chip.readFuseLow( ) );
		chip.writeFuseLow( 0xEF );
		
		statusDisplay.setStatus( "Updating fuse bit (H)..." );
		System.out.printf( "High fuse bit: %x\n", chip.readFuseHigh( ) );
		int highFuse = generateHighFuse( 0x88 );
		System.out.printf( "Fuse high will be: %x\n", highFuse );
		//ubyte highFuse = generateHighFuse( 0x88 );
		//Stdout.formatln( "Writing fuse high byte: {}", highFuse );
		chip.writeFuseHigh( highFuse );
		
		chip.exitProgMode( );
		
		statusDisplay.setStatus( "Upload Complete!" );
		statusDisplay.setIndeterminate( false );
		statusDisplay.setAnimating( false );
	}
	
	public TabFolder createUserInterface( Composite parent ) {
		TabFolder folder = new TabFolder( parent, SWT.BORDER );
		
		createProgrammingTab( folder );
		createFuseBitsTab( folder );
		
		updateLayoutData( folder );
		
		return folder;
	}
	
	private void createProgrammingTab( TabFolder folder ) {
		TabItem programmingTab = new TabItem( folder, SWT.NULL );
		programmingTab.setText( "Programming" );
		
		// create the area to put stuff
		Composite programming = new Composite( folder, SWT.NULL );
		programmingTab.setControl( programming );
		
		// give it a layout
		GridLayout layout = new GridLayout( 1, true );
		programming.setLayout( layout );
		
		// create the upload button and add events
		Button uploadFlash = new Button( programming, SWT.NULL );
		uploadFlash.setText( "Upload Flash..." );
		uploadFlash.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL ) );
		
		// upload status
		uploadStatus = new Label( programming, SWT.BORDER );
		uploadStatus.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL ) );
		uploadStatus.setAlignment( SWT.LEFT );
		uploadStatus.setText( "Waiting for upload..." );
		
		uploadProgress = new ProgressBar( programming, SWT.NONE );
		uploadProgress.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL ) );
		
		uploadFlash.addListener( SWT.Selection, new UploadButtonHandler(folder.getParent()) );
	}
	
	Button BOOTRST, BOOTSZ0, BOOTSZ1;
	
	static final int BIT_BOOTRST = 0x01;
	static final int BIT_BOOTSZ0 = 0x02;
	static final int BIT_BOOTSZ1 = 0x04;
	
	private void createFuseBitsTab( TabFolder folder ) {
		TabItem configurationTab = new TabItem( folder, SWT.NULL );
		configurationTab.setText( "Configuration" );
		
		// create the area to put stuff
		Composite configuration = new Composite( folder, SWT.NULL );
		configurationTab.setControl( configuration );
		
		// give it a layout
		GridLayout layout = new GridLayout( 1, true );
		configuration.setLayout( layout );
		
		// display some information
		Label infoLabel = new Label( configuration, SWT.BORDER );
		infoLabel.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL ) );
		infoLabel.setAlignment( SWT.LEFT );
		infoLabel.setText( "Once changed, upload a program in the 'Programming' tab\nto write the new fuse bits.\n\nChecks below reflect a setting of '1', which means unprogrammed.\n\nSee the ATMega32A datasheet for details.\n\n" );
		
		// create the fuse bit checkboxes
		BOOTSZ0 = new Button( configuration, SWT.CHECK );
		BOOTSZ0.setText( "BOOTSZ0" );
		
		BOOTSZ1 = new Button( configuration, SWT.CHECK );
		BOOTSZ1.setText( "BOOTSZ1" );
		
		BOOTRST = new Button( configuration, SWT.CHECK );
		BOOTRST.setText( "BOOTRST" );
		
		// update checkboxes from fuse bits
		int fuseByte = getChip().readFuseHigh( );
		
		BOOTRST.setSelection( ( fuseByte & BIT_BOOTRST ) != 0 );
		BOOTSZ0.setSelection( ( fuseByte & BIT_BOOTSZ0 ) != 0 );
		BOOTSZ1.setSelection( ( fuseByte & BIT_BOOTSZ1 ) != 0 );
	}
	
	int generateHighFuse( int rest ) {
		int ret = rest & 0xf8;
		
		if ( BOOTRST.getSelection() )
			ret |= BIT_BOOTRST;
		
		if ( BOOTSZ0.getSelection() )
			ret |= BIT_BOOTSZ0;
		
		if ( BOOTSZ1.getSelection() )
			ret |= BIT_BOOTSZ1;
		
		return ret;
	}
	
	private static void updateLayoutData( Composite programming ) {
		GridData deviceData = new GridData( );
		// stretch to fill the cell vertically
		deviceData.verticalAlignment = SWT.FILL;
		deviceData.grabExcessVerticalSpace = true;
		// and horizontally
		deviceData.horizontalAlignment = SWT.FILL;
		deviceData.grabExcessHorizontalSpace = true;
		
		programming.setLayoutData( deviceData );
	}
	
	void idcodeDebug( ) {
		sm = new TAPStateMachine( penprog );
		
		try {
			penprog.sendJTAGReset( true, true );
			
			sm.gotoState( TAPState.TestLogicReset );
			sm.gotoState( TAPState.ShiftDR );
			
			TAPResponse response = sm.sendCommand( TAPCommand.receiveData( 32 ) );
			int id = (int)response.getInt32( );
			
			System.out.printf( "Received IDCODE = %x (%s)\n", id, response );
			System.out.printf( "Manufacturer ID: 0x%x\n", (id >> 1) & 0x7FF );
			System.out.printf( "Part Number: 0x%x\n", (id >> 12) & 0xFFFF );
			System.out.printf( "Version: 0x%x\n", (id >> 28) & 0xF );
			
			if ( id != 0x8950203f ) {
				throw new Error( "Chip not recognised!" );
			}
			
		} finally {
			penprog.sendJTAGReset( false, false );
		}
	}
	
	public String toString( ) {
		return "Penguino AVR";
	}

	@Override
	public void cleanup() {
		System.out.println( "Cleaning up " + this );
		penprog.close( );
	}
	
}
