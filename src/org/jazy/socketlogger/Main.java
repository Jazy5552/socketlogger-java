package org.jazy.socketlogger;

public class Main {

	public static void main(String[] args) {
		String path = "logfile.txt";
		if (args.length > 0) {
			path = args[0];
		}
		new SocketLogger(path);
	}

}
