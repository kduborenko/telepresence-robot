package com.epam.telepresence.usb;

import android.hardware.usb.UsbDevice;

public interface UsbDeviceFilter {

	boolean acceptDevice(UsbDevice device);

}
