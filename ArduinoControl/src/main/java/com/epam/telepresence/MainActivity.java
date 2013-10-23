package com.epam.telepresence;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.epam.telepresence.usb.UsbDeviceDescriptor;
import com.epam.telepresence.usb.UsbDevicesDatabase;
import com.epam.telepresence.web.RobotControlServiceClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends Activity {
	private RobotControlServiceClient client;
	private UsbDevicesDatabase usbDevicesDatabase;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		usbDevicesDatabase = new UsbDevicesDatabase(getApplicationContext());
		Spinner deviceListSpinner = (Spinner) findViewById(R.id.deviceList);
		deviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				reinitializeStartButton(parent);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				reinitializeStartButton(parent);
			}
		});
		reinitializeDeviceListValues(deviceListSpinner, getApplicationContext());
		registerUsbDeviceListener(deviceListSpinner);
	}

	private void reinitializeStartButton(AdapterView<?> adapterView) {
		findViewById(R.id.startButton).setEnabled(adapterView.getSelectedItem() != null);
	}

	private void registerUsbDeviceListener(final Spinner deviceListSpinner) {
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

		BroadcastReceiver usbDeviceListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				reinitializeDeviceListValues(deviceListSpinner, context);
			}
		};
		registerReceiver(usbDeviceListener, filter);
	}

	private void reinitializeDeviceListValues(Spinner deviceListSpinner, Context context) {
		deviceListSpinner.setAdapter(new ArrayAdapter<UsbDeviceDescriptor>(context,
			android.R.layout.simple_spinner_dropdown_item, getUsbDevices(context)));
		reinitializeStartButton(deviceListSpinner);
	}

	private List<UsbDeviceDescriptor> getUsbDevices(Context context) {
		//noinspection ConstantConditions
		Collection<UsbDevice> usbDevices = ((UsbManager) context
			.getSystemService(Context.USB_SERVICE)).getDeviceList().values();
		ArrayList<UsbDeviceDescriptor> usbDeviceDescriptors = new ArrayList<UsbDeviceDescriptor>(usbDevices.size());
		for (UsbDevice usbDevice : usbDevices) {
			usbDeviceDescriptors.add(usbDevicesDatabase.getUsbDeviceDescriptor(usbDevice));
		}
		return usbDeviceDescriptors;
	}

	public void onStartButtonPressed(final View view) {
		Spinner deviceListSpinner = (Spinner) findViewById(R.id.deviceList);
		//noinspection ConstantConditions
		((Device) deviceListSpinner.getSelectedItem())
			.initialize(getApplicationContext(), new DeviceInitializationListener() {
			@Override
			public void onSuccess(Device device) {
				EditText hostView = (EditText) findViewById(R.id.host);
				String host = hostView.getText().toString();
				client = new RobotControlServiceClient(device, host);
				client.start();
				view.setEnabled(false);
				findViewById(R.id.stopButton).setEnabled(true);
				hostView.setEnabled(false);
			}
		});
	}

	public void onStopButtonPressed(View view) {
		if (client != null) {
			client.stop();
			client = null;
			view.setEnabled(false);
			findViewById(R.id.host).setEnabled(true);
			reinitializeStartButton((AdapterView<?>) findViewById(R.id.deviceList));
		}
	}

}