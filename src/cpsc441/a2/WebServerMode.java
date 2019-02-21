package cpsc441.a2;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServerMode {
	// TODO Implement web server mode
	private String requestHeaders;
	private OutputStream clientOut;
	

	public WebServerMode(String headers, OutputStream out) {
		setClientOut(out);
		setRequestHeaders(headers);
	}

	public boolean processRequest() {
		boolean isValidRequest = validateRequest(getRequestHeaders());
		if (!isValidRequest) {
			sendErrorResponse(getClientOut(), "400 Bad Request");
			return false;
		}
		
		boolean objectExists = findObject(getRequestHeaders());
		if (!objectExists) {
			sendErrorResponse(getClientOut(), "404 Not Found");
			return false;
		}
		
		// determine if request is HEAD or GET request
		// if HEAD request, send header lines only
		// else, send header lines and specified bytes from file
		
		return true;
	}
	
	private boolean findObject(String headers) {
		String objectPath = findPattern(headers, "^\\w+ (\\S+) HTTP/1.1");
		File temp = new File(objectPath);
		if (temp.exists() && temp.isDirectory()) {
			return true;
		}
		return false;
	}

	private boolean validateRequest(String headers) {
		String requestLine = findPattern(headers, "^(\\w+) /\\S+ HTTP/1.1\r\n");
		if (requestLine.equals(""))
			return false;
		
		if(!(requestLine.equals("GET") || requestLine.equals("HEAD")))
			return false;
		
		String[] splitHeaders = headers.split("\r\n");
		for (int i = 1; i < splitHeaders.length; i++) {
			boolean match = splitHeaders[i].matches("\\w+-?\\w*: \\S+\r\n");
			if (!match)
				return false;
		}
		return true;
	}

	public void sendErrorResponse(OutputStream os, String type) {
		String response = String.format("HTTP/1.1 %s\r\nDate: %s\r\nServer: Abe's-Cool-Server\r\nConnection: close", type, Utils.getCurrentDate());
		try {
			getClientOut().write(response.getBytes());
			getClientOut().flush();
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
	private String findPattern(String rawString, String pattern) {
		Pattern pat = Pattern.compile(pattern);
		Matcher mat = pat.matcher(rawString);
		if (mat.find()) {
			return mat.group(1);
		}
		return "";
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

}
