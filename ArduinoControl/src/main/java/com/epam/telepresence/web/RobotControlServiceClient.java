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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RobotControlServiceClient {

	private ExecutorService executorService = Executors.newFixedThreadPool(1);

	private String host;
	private int port;
	private Device device;
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
							byte commandCode = Byte.valueOf(requestParts[1]);
							executorService.submit(new CommandRunner(commandCode, output, requestId));
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
			Thread.sleep(time);
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

	private class CommandRunner implements Runnable {
		private final byte commandCode;
		private final PrintWriter output;
		private final String requestId;

		public CommandRunner(byte commandCode, PrintWriter output, String requestId) {
			this.commandCode = commandCode;
			this.output = output;
			this.requestId = requestId;
		}

		@Override
		public void run() {
			Log.i("RobotControlServiceClient", "Command: " + commandCode);
			device.writeByte(commandCode);
			output.println(requestId);
		}
	}
}
