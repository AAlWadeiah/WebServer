/** 
 * Course: CPSC 441 - Computer Networks
 * Semester: Winter 2019
 * Instructor: Majid Ghaderi
 * Assignment: 2
 * 
 * ProxyServerMode class
 * 
 * @author Abdullah Al-Wadeiah
 */

package cpsc441.a2;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class ProxyServerMode extends BasicServerMode{

	public ProxyServerMode(OutputStream out, String headers) {
		super(out, headers);
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
}
