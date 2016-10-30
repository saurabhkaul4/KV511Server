package edu.psu.os.KV511Server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Frontend {

	public final static int THREAD_POOL_SIZE = 500;
	public static long time1 = System.nanoTime();
	public static int count = 0;
	public static Map<Long, String> myMap = new ConcurrentHashMap<Long, String>();
	public final static ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

	public static int total_thread = 0;

	private static class Simplethread implements Runnable {

		private Socket socket = null;

		private Simplethread(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				String data;
				InputStream dataStream = this.socket.getInputStream();
				BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());

				while (true) {
					String result;
					byte[] buff = new byte[100];
					dataStream.read(buff, 0, 100);
					data = new String(buff).trim();
					//System.out.println(data);
					String[] parts = data.split(":");

					if (parts[0].trim().equalsIgnoreCase("STOP")) {
						measure();
						return;
					}
					Long key = Long.parseLong(parts[0]);
					String value = null;
					if (parts.length == 2) {
						String val = parts[1].trim();
						value = val;
					}

					if (value != null) {
						myMap.put(key, value);
						result = "0";

					} else if (!myMap.containsKey(key)) {
						result = "-1";
					} else {
						result = String.valueOf(myMap.get(key));
					}

					output.write(result.getBytes());
					output.flush();

				}
			} catch (IOException ex) {
				Logger.getLogger(Frontend.class.getName()).log(Level.SEVERE, null, ex);

			}
		}
	}

	public synchronized static void measure() {
		long time2 = System.nanoTime();
		count++;
		if (time2 - time1 > 24000000000L) {
			System.out.println("Number of requests" + count);
			count = 0;
			time1 = System.nanoTime();
		}
	}

	public static void run(int port) throws IOException {

		ServerSocket listener = new ServerSocket(port);
		try {
			while (true) {
				Socket socket = listener.accept();
				pool.execute(new Simplethread(socket));
			}
		} finally {
			listener.close();
		}
	}
}
