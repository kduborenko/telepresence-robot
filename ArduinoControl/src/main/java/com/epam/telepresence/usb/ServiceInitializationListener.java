package com.epam.telepresence.usb;

import android.hardware.usb.UsbDevice;

public interface ServiceInitializationListener {

	void onServiceInitializedSuccessfully(UsbService usbService, UsbDevice usbDevice);

}
