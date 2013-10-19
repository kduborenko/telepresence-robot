package com.epam.telepresence.usb;

import android.hardware.usb.UsbDevice;

public class UsbDeviceDescriptor {

	private final UsbDevice usbDevice;
	private final String vendorName;
	private final String deviceName;

	UsbDeviceDescriptor(UsbDevice usbDevice, String vendorName, String deviceName) {
		this.usbDevice = usbDevice;
		this.vendorName = vendorName;
		this.deviceName = deviceName;
	}

	public UsbDevice getUsbDevice() {
		return usbDevice;
	}

	public String getVendorName() {
		return vendorName;
	}

	public String getDeviceName() {
		return deviceName;
	}

	@Override
	public String toString() {
		return String.format("%s:%s %s, %s",
			Integer.toHexString(getUsbDevice().getVendorId()),
			Integer.toHexString(getUsbDevice().getProductId()),
			getVendorName(),
			getDeviceName());
	}
}
