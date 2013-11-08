package com.epam.telepresence.web;

import android.util.Log;

import com.epam.telepresence.Device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RobotControlServiceClient {

	private static final byte FWD_CODE = 5;
	private static final byte BKWD_CODE = 6;
	private static final byte RIGHT_CODE = 10;
	private static final byte LEFT_CODE = 11;

	private static final Map<Command, Action> COMMANDS = new HashMap<Command, Action>() {
		{
			put(Command.FORWARD, new SendByteAction(FWD_CODE));
			put(Command.BACKWARD, new SendByteAction(BKWD_CODE));
			put(Command.LEFT, new SendByteAction(LEFT_CODE));
			put(Command.RIGHT, new SendByteAction(RIGHT_CODE));
		}
	};

	private ExecutorService executorService = Executors.newFixedThreadPool(1);

	private String host;
	private int port;
	private Device device;
	private long lastNonEmptyCommand = 0;
	private Thread clientThread;
	private Socket socket;

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
						socket = new Socket();
						socket.connect(new InetSocketAddress(host, port));
						PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
						InputStream inputStream = socket.getInputStream();
						BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
						for (String request; (request = br.readLine()) != null; ) {
							String[] requestParts = request.split(":", 2);
							String requestId = requestParts[0];
							String commandName = requestParts[1];
							Command command = Command.valueOf(commandName);
							if (command == null) {
								continue;
							}
							executorService.submit(new CommandRunner(command, output, requestId));
						}
						inputStream.close();
					} catch (Exception e) {
						Log.e("RobotControlServiceClient", e.getMessage(), e);
					}
					sleep(1000);
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
		if (socket != null) {
			try {
				socket.close();
				socket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	private class CommandRunner implements Runnable {
		private final Command command;
		private final PrintWriter output;
		private final String requestId;

		public CommandRunner(Command command, PrintWriter output, String requestId) {
			this.command = command;
			this.output = output;
			this.requestId = requestId;
		}

		@Override
		public void run() {
			Log.i("RobotControlServiceClient", "Command: " + command.toString());
			COMMANDS.get(command).run(device, RobotControlServiceClient.this);
			output.println(requestId);
		}
	}
}
