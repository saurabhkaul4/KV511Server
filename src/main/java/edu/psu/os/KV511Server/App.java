package edu.psu.os.KV511Server;

import java.io.IOException;

public class App {
	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args[0].equalsIgnoreCase("1")) {
			System.out.println("Running Multi-Threaded server");
			Frontend.run(Integer.parseInt(args[1]));
		} else if (args[0].equalsIgnoreCase("2")) {
			System.out.println("Running Single Threaded server");
			AsyncFrontend.run(Integer.parseInt(args[1]));
		} else {
			System.out.println("Please select correctly");
		}

	}
}
