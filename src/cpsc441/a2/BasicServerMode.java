/** 
 * Course: CPSC 441 - Computer Networks
 * Semester: Winter 2019
 * Instructor: Majid Ghaderi
 * Assignment: 2
 * 
 * BasicServerMode class
 * 
 * @author Abdullah Al-Wadeiah
 */

package cpsc441.a2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BasicServerMode {
	private String requestHeaders;
	private OutputStream clientOut;
	
	public BasicServerMode(OutputStream out, String headers) {
		setClientOut(out);
		setRequestHeaders(headers);
	}
	
	/**
	 * Processes the client request.
	 * @return true if the request was processed successfully, and false otherwise.
	 */
	public abstract boolean processRequest();
	
	/**
	 * Sends an HTTP error response.
	 * @param os The output stream to send error response to.
	 * @param type The specific type of error; either, "400 Bad Request" or "404 Not Found".
	 */
	public void sendErrorResponse(OutputStream os, String type) {
		String response = String.format("HTTP/1.1 %s\r\nDate: %s\r\nServer: Abe's-Cool-Server\r\nConnection: close\r\n\r\n", type, Utils.getCurrentDate());
		try {
			os.write(response.getBytes("US-ASCII"));
			os.flush();
		} catch (IOException e) {
			System.out.println("Error sending " + type + " response: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * General method for searching a string for a specified pattern.
	 * @param rawString The raw string to be searched
	 * @param pattern The specified regular expression. Must contain exactly one group.
	 * @return first instance of pattern. Empty string otherwise
	 */
	public String findPattern(String rawString, String pattern) {
		Pattern pat = Pattern.compile(pattern);
		Matcher mat = pat.matcher(rawString);
		if (mat.find()) {
			return mat.group(1);
		}
		return "";
	}

	/**
	 * @return the clientOut
	 */
	public OutputStream getClientOut() {
		return clientOut;
	}

	/**
	 * @param clientOut the clientOut to set
	 */
	public void setClientOut(OutputStream clientOut) {
		this.clientOut = clientOut;
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
}
