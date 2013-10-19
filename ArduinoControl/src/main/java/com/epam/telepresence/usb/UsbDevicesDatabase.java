package com.epam.telepresence.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsbDevicesDatabase {

	private final Map<Long, String> vendors;
	private final Map<Long, String> devices;

	public UsbDevicesDatabase(Context context) {
		Map<Long, String> vendors = new HashMap<Long, String>();
		Map<Long, String> devices = new HashMap<Long, String>();
		Long currentVendor = null;
		try {
			InputStream is = context.getAssets().open("usb.ids");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			for (String line; (line = br.readLine()) != null;) {
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				Matcher matcher = Pattern.compile("(\\s+)?([0-9a-f]{4})\\s{2}(.*$)").matcher(line);
				if (matcher.find()) {
					String indent = matcher.group(1);
					Long id = Long.parseLong(matcher.group(2), 16);
					String name = matcher.group(3);
					if (indent == null) {
						currentVendor = id;
						vendors.put(id, name);
					} else {
						devices.put((currentVendor << 16) + id, name);
					}
					System.out.println();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.vendors = Collections.unmodifiableMap(vendors);
		this.devices = Collections.unmodifiableMap(devices);
	}

	public UsbDeviceDescriptor getUsbDeviceDescriptor(UsbDevice device) {
		return new UsbDeviceDescriptor(
			device,
			vendors.get((long) device.getVendorId()),
			devices.get(((long) device.getVendorId() << 16) + device.getProductId())
		);
	}
}
