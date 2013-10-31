package com.epam.telepresence;

import android.content.Context;

public interface Device {

	void initialize(Context applicationContext, DeviceInitializationListener listener);

	void writeByte(byte b);

	String getName();

	String getAddress();

	void dismiss();
}
