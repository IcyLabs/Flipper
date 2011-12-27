#include <usb.h>

struct usb_bus *usb_bus_get_next( struct usb_bus *bus ) {
	return bus->next;
}

struct usb_device *usb_bus_get_first_device( struct usb_bus *bus ) {
	return bus->devices;
}

struct usb_device *usb_device_get_next( struct usb_device *dev ) {
	return dev->next;
}

struct usb_device_descriptor *usb_device_get_descriptor( struct usb_device *dev ) {
	return &dev->descriptor;
}
