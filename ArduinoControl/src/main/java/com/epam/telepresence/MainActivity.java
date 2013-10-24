package com.epam.telepresence;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.epam.telepresence.bluetooth.BluetoothDeviceDescriptor;
import com.epam.telepresence.usb.UsbDeviceDescriptor;
import com.epam.telepresence.usb.UsbDevicesDatabase;
import com.epam.telepresence.web.RobotControlServiceClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

	private RobotControlServiceClient client;
	private UsbDevicesDatabase usbDevicesDatabase;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.progress);
		new Thread(new Runnable() {
			@Override
			public void run() {
				usbDevicesDatabase = new UsbDevicesDatabase(getApplicationContext());
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						onUsbDatabaseInitialized();
					}
				});
			}
		}).start();
	}

	private void onUsbDatabaseInitialized() {
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
		deviceListSpinner.setAdapter(new DeviceListDropdownAdapter(context, getDeviceList(context)));
		reinitializeStartButton(deviceListSpinner);
	}

	private List<Device> getDeviceList(Context context) {
		List<Device> devices = new ArrayList<Device>();
		devices.addAll(getUsbDevices(context));
		devices.addAll(getBluetoothDevices());
		return devices;
	}

	private List<BluetoothDeviceDescriptor> getBluetoothDevices() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			return Collections.emptyList();
		}
		List<BluetoothDeviceDescriptor> devices = new ArrayList<BluetoothDeviceDescriptor>();
		for (BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()) {
			devices.add(new BluetoothDeviceDescriptor(bluetoothDevice));
		}
		return devices;
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

	private class DeviceListDropdownAdapter extends ArrayAdapter<Device> {

		private final Map<Class, Integer> DEVICE_TYPE_ICONS = Collections.unmodifiableMap(new HashMap<Class, Integer>() {
			{
				put(BluetoothDeviceDescriptor.class, R.drawable.ic_device_access_bluetooth);
				put(UsbDeviceDescriptor.class, R.drawable.ic_device_access_usb);
			}
		});

		private LayoutInflater inflater;

		public DeviceListDropdownAdapter(Context context, List<Device> deviceList) {
			super(context, R.layout.device_dropdown_item, deviceList);
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = inflater.inflate(R.layout.device_dropdown_item, parent, false);

			Device device = getItem(position);

			((TextView) row.findViewById(R.id.name)).setText(device.getName());
			((TextView) row.findViewById(R.id.address)).setText(device.getAddress());
			((ImageView) row.findViewById(R.id.imageView)).setImageResource(DEVICE_TYPE_ICONS.get(device.getClass()));

			return row;
		}
	}
}