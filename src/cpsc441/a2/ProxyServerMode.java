package cpsc441.a2;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyServerMode extends BasicServerMode{
	private String requestHeaders;
	private OutputStream clientOut;

	/**
	 * Allows base ServerRunnable to behave like a proxy server.
	 * @param headers The client request headers.
	 * @param out The client socket's output stream 
	 */
	public ProxyServerMode(String headers, OutputStream out) {
		setRequestHeaders(headers);
		setClientOut(out);
	}

	@Override
	public boolean processRequest() {
		// Get non-local host name
		String remoteHost = findPattern(getRequestHeaders(), "Host: ([\\w\\-\\.]+)");
		
		// Set the port number
		Integer remotePort;
		String portPattern = findPattern(getRequestHeaders(), "Host: \\S+:(\\d+)\\s\\s");
		if (portPattern.equals("")) {
			remotePort = 80;
		} else {
			remotePort = Integer.parseInt(portPattern);
		}
		boolean nonPersistent = isNonPersistent(getRequestHeaders());
		if (!nonPersistent) {
			setRequestHeaders(setToNonPersistent(getRequestHeaders()));
		}
		
		// Send request to the remote host and send the response to the client 
		try {
			Socket remoteSock = new Socket(remoteHost, remotePort);
			OutputStream remoteOut = remoteSock.getOutputStream();
			InputStream remoteIn = remoteSock.getInputStream();
			remoteOut.write(getRequestHeaders().getBytes("US-ASCII"));
			remoteOut.flush();
			
			byte[] chunk = new byte[2048];
			int bytesRead;
			while ((bytesRead = remoteIn.read(chunk)) > 0) {
				getClientOut().write(chunk, 0, bytesRead);
			}
			
			// Close remote connection
			remoteOut.close();
			remoteIn.close();
			remoteSock.close();
			
		} catch (UnknownHostException e) {
			sendErrorResponse(getClientOut(), "400 Bad Request");
			return false;
		} catch (IOException e) {
			System.out.println("Error processing client proxy request: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Inserts Connection: close header if it is not already present in oldHeaders. If Connection header is present, it will set it to close.
	 * @param oldHeaders
	 * @return newHeaders containing the Connection: close header
	 */
	private String setToNonPersistent(String oldHeaders) {
		String newHeaders = "";
		if (oldHeaders.contains("Connection:")) {
			newHeaders = oldHeaders.replaceFirst("Connection: \\S+\r\n", "Connection: close\r\n");
		} else {
			newHeaders = oldHeaders.trim();
			newHeaders = newHeaders + "\r\nConnection: close\r\n\r\n";
		}
		return newHeaders;
	}

	/**
	 * Parses input headers to check if the connection is non-persistent.
	 * @param headers The HTTP headers
	 * @return true if the header "Connection: close" is found and false otherwise.
	 */
	private boolean isNonPersistent(String headers) {
		String connectionHeader = findPattern(headers, "(Connection): ");
		if (connectionHeader.equals("Connection")) {
			String connectionType = findPattern(headers, "Connection: (\\S+)\\s\\s");
			if (connectionType.equals("close")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return the requestHeaders
	 */
	public String getRequestHeaders() {
		return requestHeaders;
	}

	/**
	 * @param requestHeaders the requestHeaders to set
	 */
	public void setRequestHeaders(String requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	/**
	 * @return the serverOut
	 */
	public OutputStream getClientOut() {
		return clientOut;
	}

	/**
	 * @param serverOut the serverOut to set
	 */
	public void setClientOut(OutputStream out) {
		this.clientOut = out;
	}

	@Override
	public boolean validateRequest(String headers) {
		// TODO Auto-generated method stub
		return false;
	}
}
