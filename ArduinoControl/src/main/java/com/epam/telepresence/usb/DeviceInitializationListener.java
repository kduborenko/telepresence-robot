package com.epam.telepresence.usb;

import android.hardware.usb.UsbDevice;

public interface DeviceInitializationListener {

	void onDeviceInitializedSuccessfully(UsbService usbService, UsbDevice usbDevice);

}
