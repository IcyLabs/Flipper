/*
 * libusb4j - libusb for java using JNA.
 * Copyright (C) 2008  Mario Boikov <mario@beblue.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package libusb4j;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface LibUSBHelpers extends Library {
	LibUSBHelpers libUSBHelper = (LibUSBHelpers) Native.loadLibrary("usbhelper", LibUSBHelpers.class);
	
	usb_bus usb_bus_get_next( usb_bus bus );
	
	usb_device usb_bus_get_first_device( usb_bus bus );
	usb_device usb_device_get_next( usb_device device );
	
	usb_device_descriptor usb_device_get_descriptor( usb_device device );
}
