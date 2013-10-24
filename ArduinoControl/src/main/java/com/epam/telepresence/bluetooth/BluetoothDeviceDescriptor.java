package com.epam.telepresence.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.epam.telepresence.Device;
import com.epam.telepresence.DeviceInitializationListener;

import java.io.IOException;
import java.util.UUID;

public class BluetoothDeviceDescriptor implements Device {

	private final BluetoothDevice bluetoothDevice;
	private BluetoothSocket socket;

	public BluetoothDeviceDescriptor(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}

	@Override
	public void initialize(Context applicationContext, DeviceInitializationListener listener) {
		try {
			socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
			socket.connect();
			listener.onSuccess(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return bluetoothDevice.getName();
	}

	@Override
	public String getAddress() {
		return bluetoothDevice.getAddress();
	}

	@Override
	public void writeByte(byte b) {
		try {
			socket.getOutputStream().write(new byte[]{b});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
