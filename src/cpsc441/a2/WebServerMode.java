package cpsc441.a2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.RandomAccess;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServerMode {
	// TODO Implement web server mode
	private String requestHeaders;
	private OutputStream clientOut;
	private String objectPath;
	private boolean isRange = false;
	private Long offset;
	private Integer length;
	

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
		
		String method = findPattern(getRequestHeaders(), "^(\\w+) \\S+ HTTP/1.1\r\n");
		File requestedObject = new File(getObjectPath());
		if (method.equals("HEAD")) {
			sendHEADResponse(getClientOut(), requestedObject);
		} else {
			setRange(getRequestHeaders());
			sendGETResponse(getClientOut(), requestedObject, offset, length, isRange);
			sendFileRange(getClientOut(), requestedObject, offset, length, isRange);
		}
		
		return true;
	}

	private void sendFileRange(OutputStream out, File obj, Long pos, Integer len, boolean isR) {
		if (!isR) {
			pos = (long) 0;
			len = (int) obj.length();
		}
		try {
			RandomAccessFile reader = new RandomAccessFile(obj, "r");
			reader.seek(pos);
			byte[] temp = new byte[len];
			reader.read(temp, 0, len);
			out.write(temp);
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error reading file: " + e.getMessage());
			return;
		} catch (IOException e) {
			System.out.println("Error closing file: " + e.getMessage());
			return;
		}
		
	}
	
	private void sendGETResponse(OutputStream out, File requestedObject, Long off, Integer len, boolean isR) {
		String response, currentDate, lastModified, contentLength, contentType = "", start, end;
		
		try {
			currentDate = Utils.getCurrentDate();
			lastModified = Utils.getLastModified(requestedObject);
			contentLength = Long.toString(requestedObject.length());
			contentType = Utils.getContentType(requestedObject);
			
			if (isR) {
				start = off.toString();
				end = len.toString();
				response = String.format("HTTP/1.1 200 OK\r\nDate: %s\r\nServer: Abe's-Cool-Server\r\nLast-Modified: %s\r\n" +
						"Accept-Ranges: bytes\r\nContent-Length: %s\r\nContent-Type: %s\r\nContent-Ranges: bytes %s-%s/%s\r\n" + 
						"Connection: close\r\n\r\n",
						currentDate, lastModified, contentLength, contentType, start, end, contentLength);
			} else {
				response = String.format("HTTP/1.1 200 OK\r\nDate: %s\r\nServer: Abe's-Cool-Server\r\nLast-Modified: %s\r\nAccept-Ranges: bytes\r\n" + 
						"Content-Length: %s\r\nContent-Type: %s\r\nConnection: close\r\n\r\n", currentDate, lastModified, contentLength, contentType);
			}
			
			out.write(response.getBytes("US-ASCII"));
			out.flush();
		} catch (IOException e) {
			System.out.println("Error sending OK response: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	private void sendHEADResponse(OutputStream out, File requestedObject) {
		String currentDate, lastModified, contentLength, contentType = "";
		try {
			currentDate = Utils.getCurrentDate();
			lastModified = Utils.getLastModified(requestedObject);
			contentLength = Long.toString(requestedObject.length());
			contentType = Utils.getContentType(requestedObject);
			
			String response = String.format("HTTP/1.1 200 OK\r\nDate: %s\r\nServer: Abe's-Cool-Server\r\nLast-Modified: %s\r\nAccept-Ranges: bytes\r\n" + 
					"Content-Length: %s\r\nContent-Type: %s\r\nConnection: close\r\n\r\n", currentDate, lastModified, contentLength, contentType);
			
			out.write(response.getBytes("US-ASCII"));
			out.flush();
		} catch (IOException e) {
			System.out.println("Error sending OK response: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		
	}
	
	private void setRange(String headers) {
		String range = findPattern(headers, "Range: bytes=(\\d+-\\d+)");
		if (!range.equals("")) {
			this.isRange = true;
			String[] r = range.split("-");
			this.offset = Long.valueOf(r[0]);
			this.length = Integer.valueOf(r[1]);
		}
	}

	private boolean findObject(String headers) {
		String objectPath = findPattern(headers, "^\\w+ /(\\S+) HTTP/1.1\r\n");
		Path path = Paths.get(objectPath);
		if (Files.isRegularFile(path)) {
			setObjectPath(objectPath);
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
		for (int i = 1; i < splitHeaders.length-1; i++) {
			boolean match = splitHeaders[i].matches("\\w+-?\\w*: \\S+");
			if (!match)
				return false;
		}
		return true;
	}

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

	/**
	 * @return the objectPath
	 */
	public String getObjectPath() {
		return objectPath;
	}

	/**
	 * @param objectPath the objectPath to set
	 */
	public void setObjectPath(String objectPath) {
		this.objectPath = objectPath;
	}

}
