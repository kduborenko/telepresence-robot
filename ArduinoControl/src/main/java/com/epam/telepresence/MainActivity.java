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

import com.epam.telepresence.usb.DeviceInitializationListener;
import com.epam.telepresence.usb.UsbService;
import com.epam.telepresence.web.RobotControlServiceClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
	private RobotControlServiceClient client;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
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
		deviceListSpinner.setAdapter(new ArrayAdapter<UsbDevice>(context,
			android.R.layout.simple_spinner_dropdown_item, getUsbDevices(context)));
		reinitializeStartButton(deviceListSpinner);
	}

	private List<UsbDevice> getUsbDevices(Context context) {
		//noinspection ConstantConditions
		return new ArrayList<UsbDevice>(((UsbManager) context
			.getSystemService(Context.USB_SERVICE)).getDeviceList().values());
	}

	public void onStartButtonPressed(final View view) {
		Spinner deviceListSpinner = (Spinner) findViewById(R.id.deviceList);
		new UsbService(
			getApplicationContext(),
			(UsbDevice) deviceListSpinner.getSelectedItem(),
			new DeviceInitializationListener() {
				@Override
				public void onDeviceInitializedSuccessfully(UsbService usbService, UsbDevice usbDevice) {
					EditText hostView = (EditText) findViewById(R.id.host);
					String host = hostView.getText().toString();
					client = new RobotControlServiceClient(usbService, host);
					client.start();
					view.setEnabled(false);
					findViewById(R.id.stopButton).setEnabled(true);
					hostView.setEnabled(false);
				}
			}
		);
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