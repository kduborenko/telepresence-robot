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

	private String host;
	private int port;
	private UsbService usbService;
	private long lastNonEmptyCommand = 0;
	private Thread clientThread;

	public RobotControlServiceClient(UsbService usbService, String host) {
		String[] hostParts = host.split(":", 2);
		this.host = hostParts[0];
		this.port = hostParts.length == 1 ? 80 : Integer.parseInt(hostParts[1]);
		this.usbService = usbService;
	}

	public void start() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Socket socket = new Socket(host, port);
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
						sleep(1000);
					}
				}
			}
		});
		thread.start();
		clientThread = thread;
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

	public void stop() {
		if (clientThread != null) {
			clientThread.interrupt();
			clientThread = null;
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
