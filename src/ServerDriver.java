/**
 * A simple driver for WebServer class
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author 	Majid Ghaderi
 *
 */

import java.io.*;
import java.util.*;
import cpsc441.a2.WebServer;


public class ServerDriver {
	
	private static final int TERM_WAIT_TIME = 2000; // 2 seconds
	private static final int DEFAULT_SERVER_PORT = 2525; // default server port
	
	/**
	 * running the server
	 */
	public static void main(String[] args) {
		
		int serverPort = DEFAULT_SERVER_PORT; 
		
		// parse command line args
		if (args.length == 1)
			serverPort = Integer.parseInt(args[0]);
		
		System.out.println("starting server on port " + serverPort);
		
		WebServer server = new WebServer(serverPort);
		
		server.start();
		System.out.println("server started, type \"quit\" to stop");
		System.out.println(".....................................");

		Scanner keyboard = new Scanner(System.in);
		while ( !keyboard.next().equals("quit") );
		
		System.out.println();
		System.out.println("server is shutting down...");
		server.shutdown();
		
		try {
            server.join(TERM_WAIT_TIME);
        } catch (InterruptedException e) {
            // Ok, ignore
        }
		
		System.out.println("server stopped");
		System.exit(0);
	}
	
}