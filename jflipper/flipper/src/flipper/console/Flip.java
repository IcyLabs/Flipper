package flipper.console;

import flipper.FlipperCore;
import flipper.FlipperDevice;
import flipper.StatusDisplay;
import flipper.usb.DevicePlugMonitor;

public class Flip implements DevicePlugMonitor {
	private FlipperCore core;
	private String fileToUpload;
	
	private Flip( String filename ) {
		this.fileToUpload = filename;
		
		core = new FlipperCore( );
		core.initialise( );
		core.beginThreads( );
		core.addPlugMonitor( this );
	}
	
	public void letStuffHappen( ) {
		System.out.println( "Waiting for a Penguino..." );
		core.waitThreads();
	}
	
	private void cleanup( ) {
		core.cleanup( );
		System.exit( 0 );
	}
	
	@Override
	public void deviceConnected(FlipperDevice device) {
		System.out.println( "Device connected: " + device );
		
		StatusDisplay statusDisplay = new StatusDisplay( ) {
			String lastMessage = "Waiting...";
			double lastProgress = 0;

			@Override
			public void setStatus(String status) {
				lastMessage = status;
				updateStatus( );
			}

			@Override
			public void setProgress(int current, int maximum) {
				if ( maximum == 0 ) {
					lastProgress = 0;
				} else {
					lastProgress = (double)current / (double)maximum;
				}
				updateStatus( );
			}

			@Override
			public void setIndeterminate(boolean b) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setAnimating(boolean b) {
				// TODO Auto-generated method stub
				
			}
			
			private void updateStatus( ) {
				if ( lastProgress == 0 ) {
					System.out.format( "\r%-30s                             ", lastMessage );
				} else {
					System.out.format( "\r%-30s   %3.2f%%     ", lastMessage, lastProgress*100 );
				}
				
			}
		};
		
		System.out.println( "" );
		device.programDeviceWithFile(fileToUpload, statusDisplay);
		System.out.println( "" );
		System.out.println( "Done!" );
		
		cleanup( );
	}

	@Override
	public void deviceDisconnected(FlipperDevice device) {
		System.out.println( "Device disconnected: " + device );
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Flip flip = new Flip( args[0] );
		flip.letStuffHappen( );
	}

}
