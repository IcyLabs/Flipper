package flipper.usb;

import java.util.HashMap;

import libusb4j.*;

import com.sun.jna.Pointer;

public class USBDevice {
	private usb_device device;
	private usb_dev_handle handle;
	
	private static LibUSB libUSB = LibUSB.libUSB;
	private static LibUSBHelpers libUSBHelper = LibUSBHelpers.libUSBHelper;
	
	private USBDevice( usb_device device ) {
		this.device = device;
	}
	
	private static HashMap<Pointer,USBDevice> deviceMapping = new HashMap<Pointer,USBDevice>();
	public static USBDevice fromRawDevice( usb_device device ) {
		Pointer key = device.getPointer();
		
		if ( !deviceMapping.containsKey(key) ) {
			deviceMapping.put( key, new USBDevice(device) );
		}
		
		return deviceMapping.get( key );
	}
	
	public void open( ) {
		handle = libUSB.usb_open( device );
	}
	
	public usb_device_descriptor getDescriptor( ) {
		return libUSBHelper.usb_device_get_descriptor( device ); 
		//device.descriptor;
	}
	
	public void claimInterface( int ifaceNum ) throws USBException {
		int ret = libUSB.usb_claim_interface( handle, ifaceNum );
		if ( ret != 0 ) {
			throw new USBException( "usb_claim_interface failed, returned " + ret );
		}
	}
	
	public void releaseInterface( int ifaceNum ) throws USBException {
		int ret = libUSB.usb_release_interface( handle, ifaceNum );
		if ( ret != 0 ) {
			throw new USBException( "usb_release_interface failed, returned " + ret );
		}
	}
	
	public int bulkWrite( int endpoint, byte[] bytes ) {
		return libUSB.usb_bulk_write( handle, endpoint, bytes, bytes.length, getTimeout( ) );
	}
	
	public int bulkRead( int endpoint, byte[] bytes ) {
		return libUSB.usb_bulk_read( handle, endpoint, bytes, bytes.length, getTimeout( ) );
	}
	
	public String lastError( ) {
		return libUSB.usb_strerror();
	}
	
	private int getTimeout( ) {
		return 10000;
	}
	
	public void clearHalt( int endpoint ) {
		libUSB.usb_clear_halt( handle, endpoint );
	}
	
	public int getVendorId( ) {
		return getDescriptor().idVendor;
	}
	
	public int getProductId( ) {
		return getDescriptor().idProduct;
	}
}
