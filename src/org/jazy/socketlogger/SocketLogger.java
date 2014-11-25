package org.jazy.socketlogger;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class SocketLogger {

	Server ser;

	public SocketLogger() {
		ser = null;
		Scanner scan = new Scanner(System.in);
		String s;
		int p;
		System.out.print("Enter port number:");
		s = scan.nextLine();
		p = Integer.parseInt(s);
		try {
			ser = new Server(p);
			Thread t = new Thread() {
				public void run() {
					try {
						ser.listen();
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			};
			t.setDaemon(true);
			t.start();
		} catch (Exception e) {
			System.out.println("Error starting server");
		}
		System.out.println("Use /exit to close the program\n"
				+ "Use /close to close the last client socket");
		do {
			s = scan.nextLine();
			if (s.equals("/close") || s.equals("/exit")) {
				ser.closeLastClient();
			} else {
				if (ser.sendString(s)) {
					System.out.println("Message sent.");
				}
			}
		} while (s.equals("/exit"));
		scan.close();
	}

	private class Server {
		// Server class will run on it's own thread and listen for connections
		// the /close command will be used to close the lastClient socket.

		// Socket[] cons;
		Socket lastClient;
		ServerSocket serverSocket;

		public Server(int port) throws Exception {
			lastClient = null;
			// cons = new Socket[10];
			serverSocket = new ServerSocket(port);
		}

		public void listen() throws Exception {
			// TODO Add file log
			System.out.println("Listening for connections");
			do {
				Socket client = serverSocket.accept();
				lastClient = client;
				final BufferedReader br = new BufferedReader(
						new InputStreamReader(client.getInputStream()));
				// ^^ Final! in a while loop! ew
				// Make thread that will listen and log socket input
				Thread t = new Thread() {
					public void run() {
						try {
							System.out.println("Listening for input");
							while (true) {
								System.out.println(br.readLine());
							}
						} catch (Exception e) {
							// Toss it out
						}
					}
				};
				t.setDaemon(true);
				t.start();
			} while (!serverSocket.isClosed());
		}

		public synchronized boolean sendString(String msg) {
			try {
				PrintWriter pw = new PrintWriter(lastClient.getOutputStream());
				pw.println(msg);
				pw.close();
				pw.flush();
				return true;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				return false;
			}
		}

		public synchronized void closeLastClient() {
			try {
				lastClient.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}

	}

}
