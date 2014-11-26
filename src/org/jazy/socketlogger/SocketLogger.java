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
			e.printStackTrace();
		}
		System.out.println("Use /exit to close the program\n"
				+ "Use /close to close the last client socket\n"
				+ "Use /open IPADDRESS:PORT to connect to server");
		while (!s.equals("/exit") && ser != null) {
			s = scan.nextLine();
			if (s.equals("/close") || s.equals("/exit")) {
				ser.closeLastClient();
			} else if (s.startsWith("/open ")) {
				ser.connectTo(s.substring(6));
			} else {
				if (ser.sendString(s)) {
					System.out.println("Message sent.");
				}
			}
		}
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
		
		public synchronized void connectTo(String ip) {
			try {
				InetSocketAddress addr = parseAddress(ip);
				Socket con = new Socket(addr.getAddress(), addr.getPort());
				lastClient = con;
				backgroundLogger(con);
				System.out.println("Connected to: " + con.getRemoteSocketAddress().toString());
			} catch(Exception e) {
				System.out.println("Error connecting: " + e.getMessage());
				e.printStackTrace();
			}
		}

		public void listen() throws Exception {
			// TODO Add file log
			System.out.println("Listening for connections");
			do {
				System.out.println("Waiting for connections...");
				Socket client = serverSocket.accept();
				lastClient = client;
				System.out.println("Connection: " + client.getRemoteSocketAddress().toString());
				// ^^ Final! in a while loop! ew
				// Make thread that will listen and log socket input
				backgroundLogger(client);
			} while (!serverSocket.isClosed());
		}

		public synchronized boolean sendString(String msg) {
			try {
				PrintWriter pw = new PrintWriter(lastClient.getOutputStream());
				pw.print(msg + "\r\n");
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
		
		private void backgroundLogger(final Socket client) {
			//Log socket input on seperate thread.
			Thread t = new Thread() {
				public void run() {
					try {
						System.out.println("Connection. Listening for input");
						BufferedReader br = new BufferedReader(
								new InputStreamReader(client.getInputStream()));
						String in = "";
						do {
							in = br.readLine();
							if (in != null) {
								System.out.println(in);
							}
						} while (in != null);
						System.out.println(client.getRemoteSocketAddress().toString() + " disconnected.");
					} catch (Exception e) {
						// Toss it out
					}
				}
			};
			t.setDaemon(true);
			t.start();
		}
		
		private InetSocketAddress parseAddress(String ip) throws Exception{
			InetSocketAddress addr = null;
			if (ip.contains(":")) {
				addr = new InetSocketAddress(ip.split(":")[0],Integer.parseInt(ip.split(":")[1]));
			} else {
				System.out.print("Enter the port:");
				@SuppressWarnings("resource")
				Scanner tmpScan = new Scanner(System.in);
				addr = new InetSocketAddress(ip, Integer.parseInt(tmpScan.nextLine()));
				//tmpScan.close(); //Cant close because the main loop will be killed
				//ew
			}
			return addr;
		}

	}

}
