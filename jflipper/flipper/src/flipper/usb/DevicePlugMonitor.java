package flipper.usb;

import flipper.FlipperDevice;

public interface DevicePlugMonitor {
	void deviceConnected( FlipperDevice device );
	void deviceDisconnected( FlipperDevice device );
}
