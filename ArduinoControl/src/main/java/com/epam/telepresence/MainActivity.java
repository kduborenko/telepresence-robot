package com.epam.telepresence;


import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;

import com.epam.telepresence.usb.UsbDeviceFilter;
import com.epam.telepresence.usb.UsbService;
import com.epam.telepresence.web.RobotControlServiceClient;

public class MainActivity extends Activity {
	private UsbService usbService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.usbService = new UsbService(getApplicationContext(), new UsbDeviceFilter() {
			@Override
			public boolean acceptDevice(UsbDevice device) {
				return device.getVendorId() == 0x2341 && device.getProductId() == 0x0043;
			}
		});
		RobotControlServiceClient.startClient(usbService);
	}

}