/**
 * WebServer Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author 	Majid Ghaderi
 *
 */

package cpsc441.a2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class WebServer extends BasicWebServer {

	private boolean shutdown = false;
	private ServerSocket mainSocket;
	private ExecutorService threadExecutor;

	/**
	 * Creates a new server socket with the specified port and allocates a newCachedThreadPool to threads.
	 * Prints out server information.
	 * @param port The port number which will be connected to.
	 */
	public WebServer(int port) {
		super(port);

		// start server socket
		try {
			mainSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Could not create server socket: " + e.getMessage());
			return;
		}
		threadExecutor = Executors.newCachedThreadPool();
		System.out.println ("Server IP address: " + mainSocket.getInetAddress().getHostAddress() + ",  port " + port);
	}

	/**
	 * Awaits incoming client requests and attempts to accept the requests.
	 * Creates a new runnable for every client request and adds it to the thread pool.
	 */
	public void run() {
		Socket client = new Socket();
		ServerRunnable wr;

		while (!shutdown) {
			// try you to accept incoming connection
			try {
				client = mainSocket.accept();
				wr = new ServerRunnable(client);
				threadExecutor.execute(wr);

			} catch (IOException e) {
				System.out.println("Server likely closed. " + e.getMessage());
				shutdown();
			}
		}
	}

	/**
	 * Shuts down the threadExecutor and attempts to close the server socket.
	 */
	public void shutdown() {
		shutdown = true;
		System.out.println("Server shutting down");
		if (!threadExecutor.isShutdown()) {
			threadExecutor.shutdownNow();
		}

		try {
			mainSocket.close();
		} catch (IOException e) {
			System.out.println("Could not close server socket: " + e.getMessage());
		}
	}
}