package org.jazy.socketlogger;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class SocketLogger {

	Server ser = null;
	FileWriter log = null;
	Scanner scan = new Scanner(System.in);

	public SocketLogger(String filePath) {
		// Open file for logging
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
		// Complete opening log file
		
		ser = new Server();

		//Loop to listen for user commands
		String s = "";
		while (!s.equals("/exit") && ser != null) {
			s = scan.nextLine();
			if (s.equals("/close") || s.equals("/exit")) {
				ser.closeLastClient();
			} else if (s.startsWith("/open ")) {
				ser.connectTo(s.substring(6));
			} else if (s.equals("/restart")) {
				ser.close();
				ser = new Server();
			} else {
				if (ser.sendString(s)) {
					try {
						log.write(s);
					} catch (IOException e) {
						e.printStackTrace();
					}
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

	public synchronized String GetDateTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
	}

	private class Server {
		// Server class will run on it's own thread and listen for connections
		// the /close command will be used to close the lastClient socket.

		// Socket[] cons;
		Socket lastClient;
		ServerSocket serverSocket;
		int conNum;

		public Server() {
			lastClient = null;
			conNum = 0;
			// cons = new Socket[10];
			// Initialize server
			String s = "";
			System.out.println("Jazy's server logger\n"
							+ "Start the app AppName.exe [FILENAME] to change log file name\n"
							+ "Use /exit to close the program\n"
							+ "Use /close to close the last client socket\n"
							+ "Use /restart to restart\n"
							+ "Use /open IPADDRESS:PORT to connect to server");
			try {
				System.out.print("Enter port number:");
				s = scan.nextLine();
				final int p = Integer.parseInt(s);
				Thread t = new Thread() {
					public void run() {
						try {
							listen(p);
						} catch (Exception e) {
							Log("Error starting server");
							e.printStackTrace();
						}
					}
				};
				t.setDaemon(true);
				t.start();
			} catch (Exception e) {
				Log("Error starting server");
				e.printStackTrace();
			}
			//scan.close();
		}

		public synchronized void connectTo(String ip) {
			try {
				InetSocketAddress addr = parseAddress(ip);
				Socket con = new Socket(addr.getAddress(), addr.getPort());
				lastClient = con;
				backgroundLogger(con);
				Log("Connected to: " + con.getRemoteSocketAddress().toString());
			} catch (Exception e) {
				Log("Error connecting: " + e.getMessage());
				e.printStackTrace();
			}
		}

		public void listen(int port) throws Exception {
			// Server initialize complete
			serverSocket = new ServerSocket(port);
			Log("Server started listening for connections...");
			do {
				Socket client = serverSocket.accept();
				lastClient = client;
				Log(++conNum + ".Connection: " + client.getRemoteSocketAddress().toString()
						+ " at " + GetDateTime());
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
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
			} catch (Exception e) {
				//Log(e.getMessage());
			}
			return false;
		}

		public synchronized void closeLastClient() {
			try {
				lastClient.close();
			} catch (IOException e) {
				Log(e.getMessage());
			}
		}
		
		public void close() {
			try {
				serverSocket.close();
				serverSocket = null;
				Log("Server closed.");
			} catch (Exception e) {
				//Throw away
			}
		}

		private void backgroundLogger(final Socket client) {
			// Log socket input on seperate thread.
			Thread t = new Thread() {
				public void run() {
					try {
						Log("Connected. Listening for input...");
						BufferedReader br = new BufferedReader(
								new InputStreamReader(client.getInputStream()));
						String in = "";
						do {
							in = br.readLine(); // Will hold if client never
												// closes
							if (in != null) {
								Log(in);
							}
						} while (in != null);
						Log(client.getRemoteSocketAddress().toString()
								+ " disconnected.");
					} catch (Exception e) {
						// Toss it out
					}
				}
			};
			t.setDaemon(true);
			t.start();
		}

		private InetSocketAddress parseAddress(String ip) throws Exception {
			InetSocketAddress addr = null;
			if (ip.contains(":")) {
				addr = new InetSocketAddress(ip.split(":")[0],
						Integer.parseInt(ip.split(":")[1]));
			} else {
				System.out.print("Enter the port:");
				@SuppressWarnings("resource")
				Scanner tmpScan = new Scanner(System.in);
				addr = new InetSocketAddress(ip, Integer.parseInt(tmpScan
						.nextLine()));
				// tmpScan.close(); //Cant close because the main loop will be
				// killed
				// ew
			}
			return addr;
		}

	}

}
