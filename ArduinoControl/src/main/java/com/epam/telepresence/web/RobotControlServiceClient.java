package com.epam.telepresence.web;

import android.util.Log;

import com.epam.telepresence.Device;

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

	private static final Action DO_NOTHING = new Action() {
		@Override
		public void run(Device device, RobotControlServiceClient client) {
			client.sleep(100);
		}
	};

	private static final Map<Command, Action> COMMANDS = new HashMap<Command, Action>() {
		{
			put(Command.EMPTY, DO_NOTHING);
			put(Command.FORWARD, new SendByteAction(FWD_CODE));
			put(Command.BACKWARD, new SendByteAction(BKWD_CODE));
			put(Command.LEFT, new SendByteAction(LEFT_CODE));
			put(Command.RIGHT, new SendByteAction(RIGHT_CODE));
		}

		@Override
		public Action get(Object key) {
			Action action = super.get(key);
			return action == null ? DO_NOTHING : action;
		}
	};

	private String host;
	private int port;
	private Device device;
	private long lastNonEmptyCommand = 0;
	private Thread clientThread;

	public RobotControlServiceClient(Device device, String host) {
		String[] hostParts = host.split(":", 2);
		this.host = hostParts[0];
		this.port = hostParts.length == 1 ? 80 : Integer.parseInt(hostParts[1]);
		this.device = device;
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
							COMMANDS.get(command).run(device, RobotControlServiceClient.this);
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
		device.dismiss();
	}

	public Device getDevice() {
		return device;
	}

	private interface Action {
		void run(Device device, RobotControlServiceClient client);
	}

	private static class SendByteAction implements Action {

		private byte code;

		private SendByteAction(byte code) {
			this.code = code;
		}

		@Override
		public void run(Device device, RobotControlServiceClient client) {
			device.writeByte(code);
			client.lastNonEmptyCommand = System.currentTimeMillis();
		}
	}
}
