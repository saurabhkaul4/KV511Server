package edu.psu.os.KV511Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class AsyncFrontend {

	public static Map<Long, String> myMap = new HashMap<Long, String>();
	public static Map<SocketChannel, ArrayList<String>> pendingRequest = new HashMap<SocketChannel, ArrayList<String>>();
	public static long time1 = System.nanoTime();
	public static int count = 0;

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

		class writeInfo {
			SocketChannel client = null;
			String value = null;

			writeInfo(SocketChannel client, String value) {
				this.client = client;
				this.value = value;
			}
		}

		Queue<writeInfo> myQueue = new LinkedList<writeInfo>();

		Selector selector = Selector.open();
		ServerSocketChannel socketChannel = ServerSocketChannel.open();
		InetSocketAddress socketAddress = new InetSocketAddress(port);

		socketChannel.bind(socketAddress);
		socketChannel.configureBlocking(false); // For non-blocking

		int operations = socketChannel.validOps();
		SelectionKey sKey = socketChannel.register(selector, operations, null);

		while (true) {
			selector.select();

			Set<SelectionKey> selectKeys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectKeys.iterator();
			int i = 1;
			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();
				keyIterator.remove();

				if (key.isAcceptable()) {
					// to check if key's channel is ready to accept new
					// connection
					//Logger.getLogger(AsyncFrontend.class.getName()).log(Level.SEVERE, Integer.toString(i), "r");

					SocketChannel client = socketChannel.accept();
					Socket socket = client.socket();

					client.configureBlocking(false); // Adjust channel's
														// blocking mode
					client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
				} else if (key.isReadable()) {
					SocketChannel client = (SocketChannel) key.channel();
					ByteBuffer buffer = ByteBuffer.allocate(256);
					client.read(buffer);
					String result = new String(buffer.array()).trim();
					buffer.clear();

					String[] parts = result.split(":");
					// Logger.getLogger(AsyncFrontend.class.getName()).log(Level.SEVERE,
					// result, "r");

					if (parts[0].equals("STOP")) {
						key.cancel();
						client.close();
						measure();
						continue;
					}

					Long mapkey = Long.parseLong(parts[0], 10);
					String mapValue = null;
					if (parts.length == 2) {
						String val = parts[1].trim();
						mapValue = val;
					}

					// Output
					String output = "";
					if (mapValue != null) {
						myMap.put(mapkey, mapValue);
						output = "0";
					} else if (!myMap.containsKey(mapkey)) {
						output = "-1";
					} else {
						output = myMap.get(mapkey);
					}

					if (pendingRequest.containsKey(client)) {
						ArrayList<String> clientRequest = pendingRequest.get(client);
						clientRequest.add(result);
						pendingRequest.put(client, clientRequest);

					}

					ArrayList<String> newString = new ArrayList<String>();
					newString.add(output);
					pendingRequest.put(client, newString);

				} else if (key.isWritable()) {
					SocketChannel channel = (SocketChannel) key.channel();

					if (!pendingRequest.containsKey(channel)) {
						continue;
					}

					if (pendingRequest.get(channel).isEmpty()) {
						continue;
					}
					int a = 0;

					ArrayList<String> request = pendingRequest.get(channel);
					int flag = 0;
					for (String s : request) {
						if (s.equals("STOP")) {
							key.cancel();
							channel.close();
							pendingRequest.put(channel, new ArrayList());
							flag = 1;
							break;

						}
						if (flag == 1) {
							continue;
						}
						ByteBuffer b = ByteBuffer.allocate(100);
						byte[] bytes = s.getBytes(Charset.forName("UTF-8"));
						b = ByteBuffer.wrap(bytes);
						b.compact();
						b.flip();
						a = channel.write(b);
					}

					pendingRequest.put(channel, new ArrayList());
					break;

				}
			}

		}
	}
}
