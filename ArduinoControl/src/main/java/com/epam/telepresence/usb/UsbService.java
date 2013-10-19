package com.epam.telepresence.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbService {

	protected static final String ACTION_USB_PERMISSION = "com.epam.telepresence.USB";
	private final UsbManager usbManager;
	private UsbDeviceConnection conn;
	private UsbEndpoint output;

	public UsbService(Context appContext, UsbDevice d, DeviceInitializationListener listener) {
		this.usbManager = (UsbManager) appContext.getSystemService(Context.USB_SERVICE);
		if (!usbManager.hasPermission(d)) {
			PendingIntent pi = PendingIntent.getBroadcast(appContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
			appContext.registerReceiver(new UsbDeviceBroadcastReceiver(listener), new IntentFilter(ACTION_USB_PERMISSION));
			usbManager.requestPermission(d, pi);
		} else {
			initializeDevice(d, listener);
		}
	}

	private void initializeDevice(UsbDevice device, DeviceInitializationListener listener) {
		conn = usbManager.openDevice(device);
		UsbInterface usbInterface = device.getInterface(1);
		if (!conn.claimInterface(usbInterface, true)) {
			return;
		}

		output = getEndpoint(usbInterface, UsbConstants.USB_DIR_OUT);
		listener.onDeviceInitializedSuccessfully(this, device);
	}

	private UsbEndpoint getEndpoint(UsbInterface usbInterface, int direction) {
		for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
			UsbEndpoint endpoint = usbInterface.getEndpoint(i);
			if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && endpoint.getDirection() == direction) {
				return endpoint;
			}
		}
		return null;
	}

	public void sendByte(byte b) {
		send(new byte[]{b});
	}

	public void send(byte[] bytes) {
		conn.bulkTransfer(output, bytes, bytes.length, 0);
	}

	private class UsbDeviceBroadcastReceiver extends BroadcastReceiver {
		private final DeviceInitializationListener listener;

		public UsbDeviceBroadcastReceiver(DeviceInitializationListener listener) {
			this.listener = listener;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			context.unregisterReceiver(this);
			if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (device == null) {
					Log.d("UsbService", "Device not present");
					return;
				}
				if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					Log.d("UsbService", "Permission denied on " + device.getDeviceId());
				} else {
					Log.d("UsbService", "Permission granted");
					initializeDevice(device, listener);
				}
			}
		}
	}
}
