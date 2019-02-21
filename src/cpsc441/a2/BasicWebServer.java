/**
 * BasicWebServer Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author 	Majid Ghaderi
 *
 */

package cpsc441.a2;

public abstract class BasicWebServer extends Thread {
	
	protected int serverPort;
	
	
    /**
     * Constructor to initialize the server
     * You may override this method if needed
     * 
     * @param port 	The server port at which the web server listens > 1024
     * 
     */
	public BasicWebServer(int port) {
		serverPort = port;
	}

	
    /**
	 * Starts the we bserver:
	 * 
	 *	open server socket
	 *	while (!shutdown) 
	 *		accept client connection
	 *		start a new worker thread to process client request
	 *
     */
	public abstract void run();

	
    /**
     * Signals to server to shutdown
	 *
     */
	public abstract void shutdown();
	
}