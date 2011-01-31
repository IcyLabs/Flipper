package flipper.targets.penguino;

import flipper.FlipperCore;
import flipper.FlipperDevice;
import flipper.FlipperPlugin;
import flipper.usb.USBDevice;
import flipper.usb.USBDeviceManager;
import flipper.usb.USBDeviceMatcher;
import libusb4j.LibUSBConstants;

public class Plugin implements FlipperPlugin {
	private static final int usbVendorId = 0x16D0;
	private static final int usbProductId = 0x04CA;

	public String getName() {
		return "Penguino AVR";
	}
	
	public void activatePlugin(FlipperCore core) {
		System.out.println( "Penguino plugin activated." );
		
		System.out.println( "XXX: path max from libusb is: " + LibUSBConstants.PATH_MAX );
		
		// add our device matcher
		USBDeviceManager manager = USBDeviceManager.getDeviceManager( );
		manager.addMatcher( new USBDeviceMatcher( ) {

			public boolean matchesDevice(USBDevice device) {
				System.out.printf( "Matching 0x%x:0x%x against Penguino\n", device.getVendorId(), device.getProductId() );
				return ( device.getVendorId() == usbVendorId &&
						 device.getProductId() == usbProductId );
			}

			public FlipperDevice getFlipperDevice(USBDevice device) {
				return new PenguinoDevice( device );
			}
			
		} );
		
		System.out.println( "Matcher added for Penguino." );
	}

	public void deactivatePlugin(FlipperCore core) {
		// TODO Auto-generated method stub

	}
	
}
