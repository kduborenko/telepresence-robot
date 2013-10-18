package com.epam.telepresence.web;

import android.util.Log;

import com.epam.telepresence.usb.UsbService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class RobotControlServiceClient {

	private static final byte FWD_CODE = 5;
	private static final byte BKWD_CODE = 6;
	private static final byte RIGHT_CODE = 10;
	private static final byte LEFT_CODE = 11;

	private static final UsbCommand DO_NOTHING = new UsbCommand() {
		@Override
		public void run(UsbService usbService, RobotControlServiceClient client) {
			client.sleep(100);
		}
	};

	private static final Map<Command, UsbCommand> COMMANDS = new HashMap<Command, UsbCommand>() {
		{
			put(Command.EMPTY, DO_NOTHING);
			put(Command.FORWARD, new SendByteUsbCommand(FWD_CODE));
			put(Command.BACKWARD, new SendByteUsbCommand(BKWD_CODE));
			put(Command.LEFT, new SendByteUsbCommand(LEFT_CODE));
			put(Command.RIGHT, new SendByteUsbCommand(RIGHT_CODE));
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
						Socket socket = new Socket("207.244.68.115", 8080);
						PrintWriter pw = new PrintWriter(socket.getOutputStream());
						pw.print("GET /apps/RobotApp/Commander\r\n");
						pw.flush();
						BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						for (String commandName; (commandName = br.readLine()) != null; ) {
							Command command = Command.valueOf(commandName);
							Log.i("RobotControlServiceClient", "Command: " + command.toString());
							COMMANDS.get(command).run(usbService, RobotControlServiceClient.this);
						}
						socket.getInputStream().close();
					} catch (Exception e) {
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

		private byte code;

		private SendByteUsbCommand(byte code) {
			this.code = code;
		}

		@Override
		public void run(UsbService usbService, RobotControlServiceClient client) {
			usbService.sendByte(code);
			client.lastNonEmptyCommand = System.currentTimeMillis();
		}
	}
}
