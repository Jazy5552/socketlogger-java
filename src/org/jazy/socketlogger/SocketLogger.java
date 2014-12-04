package org.jazy.socketlogger;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class SocketLogger {

	Server ser = null;
	FileWriter log = null;

	public SocketLogger(String filePath) {
		Scanner scan = new Scanner(System.in);
		String s = "";
		int p;
		//Open file for logging
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			log = new FileWriter(file, true);
		} catch (Exception e) {
			Log("Error creating log file:" + e.getMessage());
			e.printStackTrace();
		}
		//Complete opening log file
		//Initialize server
		System.out.println("Jazy's server logger\n"
				+ "Use /exit to close the program\n"
				+ "Use /close to close the last client socket\n"
				+ "Use /open IPADDRESS:PORT to connect to server\n\n");
		try {
			System.out.print("Enter port number:");
			s = scan.nextLine();
			p = Integer.parseInt(s);
			ser = new Server(p);
			Thread t = new Thread() {
				public void run() {
					try {
						ser.listen();
					} catch (Exception e) {
						Log(e.getMessage());
					}
				}
			};
			t.setDaemon(true);
			t.start();
		} catch (Exception e) {
			Log("Error starting server");
			e.printStackTrace();
		}
		//Server initialize complete
		
		while (!s.equals("/exit") && ser != null) {
			s = scan.nextLine();
			if (s.equals("/close") || s.equals("/exit")) {
				ser.closeLastClient();
			} else if (s.startsWith("/open ")) {
				ser.connectTo(s.substring(6));
			} else {
				if (ser.sendString(s)) {
					//TODO Log the sent message
					System.out.println("Message sent.");
				}
			}
		}
		scan.close();
		try {
			log.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void Log(String msg) {
		if (log != null) {
			try {
				log.write(msg + "\n");
				log.flush();
			} catch (Exception e) {
				e.printStackTrace();
				log = null;
			}
		}
		System.out.println(msg);
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
				Log("Connected to: " + con.getRemoteSocketAddress().toString());
			} catch(Exception e) {
				Log("Error connecting: " + e.getMessage());
				e.printStackTrace();
			}
		}

		public void listen() throws Exception {
			Log("Server started listening for connections...");
			do {
				Socket client = serverSocket.accept();
				lastClient = client;
				Log("Connection: " + client.getRemoteSocketAddress().toString());
				// ^^ Final! in a while loop! ew
				// Make thread that will listen and log socket input
				backgroundLogger(client);
			} while (!serverSocket.isClosed());
			Log("Server socket closed.");
		}

		public synchronized boolean sendString(String msg) {
			try {
				PrintWriter pw = new PrintWriter(lastClient.getOutputStream());
				pw.print(msg + "\r\n");
				pw.flush();
				return true;
			} catch (Exception e) {
				Log(e.getMessage());
				return false;
			}
		}

		public synchronized void closeLastClient() {
			try {
				
				lastClient.close();
			} catch (IOException e) {
				Log(e.getMessage());
			}
		}
		
		private void backgroundLogger(final Socket client) {
			//Log socket input on seperate thread.
			Thread t = new Thread() {
				public void run() {
					try {
						Log("Connection. Listening for input");
						BufferedReader br = new BufferedReader(
								new InputStreamReader(client.getInputStream()));
						String in = "";
						do {
							in = br.readLine(); //Will hold if client never closes
							if (in != null) {
								Log(in);
							}
						} while (in != null);
						Log(client.getRemoteSocketAddress().toString() + " disconnected.");
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
