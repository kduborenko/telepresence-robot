package com.epam.telepresence.web;

import android.util.Log;

import com.epam.telepresence.usb.UsbService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RobotControlServiceClient {

	private static final byte FWD_PIN = 4;
	private static final byte BKWD_PIN = 5;
	private static final byte LEFT_PIN = 6;
	private static final byte RIGHT_PIN = 7;

	private static final UsbCommand DO_NOTHING = new UsbCommand() {
		@Override
		public void run(UsbService usbService, RobotControlServiceClient client) {
			client.sleep(100);
		}
	};

	private static final Map<Command, UsbCommand> COMMANDS = new HashMap<Command, UsbCommand>() {
		{
			put(Command.EMPTY, 	DO_NOTHING);
			put(Command.FORWARD, 	new SendByteUsbCommand((byte) (1 << FWD_PIN)));
			put(Command.BACKWARD, new SendByteUsbCommand((byte) (1 << BKWD_PIN)));
			put(Command.LEFT, 	new SendByteUsbCommand((byte) (1 << LEFT_PIN)));
			put(Command.RIGHT, 	new SendByteUsbCommand((byte) (1 << RIGHT_PIN)));
		}

		@Override
		public UsbCommand get(Object key) {
			UsbCommand usbCommand = super.get(key);
			return usbCommand == null ? DO_NOTHING : usbCommand;
		}
	};

	private long lastNonEmptyCommand = 0;

	public void startClient(final UsbService usbService) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						URL url = new URL("http://207.244.68.115:8080/apps/RobotApp/Commander");
						InputStream in = url.openConnection().getInputStream();
						byte[] buffer = new byte[1];
						for (int length; (length = in.read(buffer)) != -1;) {
							for (int i = 0; i < length; i++) {
								Command command = Command.BY_CODE.get(buffer[i]);
								Log.i("URL", command.toString());
								COMMANDS.get(command).run(usbService, RobotControlServiceClient.this);
							}
						}
						in.close();
					} catch (MalformedURLException e) {
						Log.e("RobotControlServiceClient", e.getMessage(), e);
					} catch (IOException e) {
						Log.e("RobotControlServiceClient", e.getMessage(), e);
					}
				}
			}
		}).start();
	}

	private void sleep(long time) {
		try {
			if ((System.currentTimeMillis() - lastNonEmptyCommand) >= 15000) {
				Thread.sleep(time);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private interface UsbCommand {
		void run(UsbService usbService, RobotControlServiceClient client);
	}

	private static class SendByteUsbCommand implements UsbCommand {

		private byte b;

		private SendByteUsbCommand(byte b) {
			this.b = b;
		}

		@Override
		public void run(UsbService usbService, RobotControlServiceClient client) {
			usbService.sendByte(b);
			client.lastNonEmptyCommand = System.currentTimeMillis();
		}
	}
}
