package flipper.usb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import libusb4j.*;

import flipper.FlipperDevice;

/**
 * The USBDeviceManager is responsible for enumerating and matching
 * USB devices, and instantiating the appropriate classes.
 */
public class USBDeviceManager {
	private static LibUSB libUSB = LibUSB.libUSB;
	private static LibUSBHelpers libUSBHelper = LibUSBHelpers.libUSBHelper;
	
	private static final long devicePollDelay = 1000;
	
	static {
		libUSB.usb_init();
	}
	
	private USBDeviceManager( ) {
		
	}
	
	private static final USBDeviceManager deviceManager = new USBDeviceManager( );
	public static USBDeviceManager getDeviceManager( ) {
		return deviceManager;
	}
	
	ArrayList<USBDeviceMatcher> matchers = new ArrayList<USBDeviceMatcher>( );
	HashMap<USBDevice,FlipperDevice> matchedDevices = new HashMap<USBDevice,FlipperDevice>( );
	
	public void addMatcher( USBDeviceMatcher matcher ) {
		matchers.add( matcher );
	}
	
	ArrayList<DevicePlugMonitor> plugMonitors = new ArrayList<DevicePlugMonitor>( );
	public void addPlugMonitor( DevicePlugMonitor monitor ) {
		plugMonitors.add( monitor );
	}
	
	Timer devicePollTimer;
	
	class PollTask extends TimerTask {
        public void run() {
        	//System.out.println( "USB Device Manager polling!" );
        	
        	libUSB.usb_find_busses();
    		int changedDevices = libUSB.usb_find_devices();
    		
    		if ( changedDevices > 0 ) {
    			ArrayList<USBDevice> disconnectedDevices = new ArrayList<USBDevice>( matchedDevices.keySet( ) );
    			
    			System.out.println( "USB Device Changes: " + changedDevices );
    			
    			for ( usb_bus bus = libUSB.usb_get_busses(); bus != null; bus = libUSBHelper.usb_bus_get_next( bus ) ) {
    				System.out.println( "Browsing bus..." );
    				for ( usb_device dev = libUSBHelper.usb_bus_get_first_device( bus ); dev != null; dev = libUSBHelper.usb_device_get_next( dev ) ) {
    					System.out.println( "Looking at device: " + dev );
    					
    					USBDevice udev = USBDevice.fromRawDevice( dev );
    					
    					System.out.println( "Found device: " + udev );
    					
    					if ( matchedDevices.containsKey( udev ) ) {
    						// we already know about this device, and it's still here
    						//System.out.println( udev + " still there" );
    						disconnectedDevices.remove( udev );
    						continue; // don't process it
    					}
    					
    					// ask our matchers if they understand this device...
    					for ( USBDeviceMatcher matcher : matchers ) {
    						if ( matcher.matchesDevice( udev ) ) {
    							System.out.println( "Matched device: " + udev );
    							
    							FlipperDevice fDevice = matcher.getFlipperDevice( udev );
    							
    							matchedDevices.put( udev, fDevice );
    							
    							for ( DevicePlugMonitor monitor : plugMonitors ) {
    								monitor.deviceConnected( fDevice );
    							}
    						}
    					}
    				}
    			}
    			
    			System.out.println( "Disconnect: " + disconnectedDevices );
    			for ( USBDevice disconnectedDevice : disconnectedDevices ) {
    				for ( DevicePlugMonitor monitor : plugMonitors ) {
						monitor.deviceDisconnected( matchedDevices.get(disconnectedDevice) );
					}
    				
    				matchedDevices.remove( disconnectedDevice );
    			}
    		}
        }
    }
	
	public void runAsThread() {
		devicePollTimer = new Timer();
		devicePollTimer.scheduleAtFixedRate( new PollTask(), 0, devicePollDelay );
	}
}
