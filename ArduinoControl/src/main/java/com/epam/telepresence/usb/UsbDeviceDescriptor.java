package com.epam.telepresence.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import com.epam.telepresence.Device;
import com.epam.telepresence.DeviceInitializationListener;

public class UsbDeviceDescriptor implements Device {

	private UsbService usbService;
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
	public String getName() {
		return String.format("%s, %s", getVendorName(),
			getDeviceName() == null ? "<unknown>" : getDeviceName());
	}

	@Override
	public String getAddress() {
		return String.format("%s:%s",
			toHexRepresentation(getUsbDevice().getVendorId()),
			toHexRepresentation(getUsbDevice().getProductId()));
	}

	private String toHexRepresentation(int id) {
		StringBuilder sb = new StringBuilder(Integer.toHexString(id));
		while (sb.length() < 4) {
			sb.insert(0, '0');
		}
		return sb.toString();
	}

	@Override
	public void initialize(Context context, final DeviceInitializationListener listener) {
		usbService = new UsbService(
			context, usbDevice,
			new ServiceInitializationListener() {
				@Override
				public void onServiceInitializedSuccessfully(UsbService usbService, UsbDevice usbDevice) {
					listener.onSuccess(UsbDeviceDescriptor.this);
				}
			}
		);
	}

	@Override
	public void writeByte(byte b) {
		usbService.sendByte(b);
	}
}
