package flipper.usb;

import flipper.FlipperDevice;

public interface USBDeviceMatcher {
	boolean matchesDevice( USBDevice device );
	FlipperDevice getFlipperDevice( USBDevice device );
}
