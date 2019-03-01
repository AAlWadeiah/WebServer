package cpsc441.a2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebServerMode extends BasicServerMode{

	private String objectPath;
	private boolean isRange = false;
	private Long offset;
	private Integer length;
	
	public WebServerMode(OutputStream out, String headers) {
		super(out, headers);
	}

	@Override
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

	/**
	 * Sends the requested bytes of the file to the client.
	 * @param out The client's output stream.
	 * @param obj The file of the requested object.
	 * @param pos The position to start reading from in the file.
	 * @param len The number of bytes to read from the file.
	 * @param isR Boolean indicating if the request is a Range request.
	 */
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
	
	/**
	 * Sends a response to a GET request.
	 * @param out The output stream to send the response to.
	 * @param requestedObject The file of the requested object.
	 * @param off The starting position of reading the file.
	 * @param len The number of bytes to be read from the file.
	 * @param isR Boolean indicating if the request is a Range request.
	 */
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

	/**
	 * Sends a response to a HEAD request.
	 * @param out The output stream to send the response to.
	 * @param requestedObject The file of the requested object.
	 */
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
	
	/**
	 * Looks for the Range header. If Range header is found, it is parsed to set the offset and length for reading from the file.
	 * @param headers The request headers.
	 */
	private void setRange(String headers) {
		String range = findPattern(headers, "Range: bytes=(\\d+-\\d+)");
		if (!range.equals("")) {
			this.isRange = true;
			String[] r = range.split("-");
			this.offset = Long.valueOf(r[0]);
			this.length = Integer.valueOf(r[1]);
		}
	}

	/**
	 * Attempts to find the requested object in the server's working directory. If the file is found, the path is set.
	 * @param headers The request headers.
	 * @return true if the object was found, and false otherwise
	 */
	private boolean findObject(String headers) {
		String objectPath = findPattern(headers, "^\\w+ /(\\S+) HTTP/1.1\r\n");
		Path path = Paths.get(objectPath);
		if (Files.isRegularFile(path)) {
			setObjectPath(objectPath);
			return true;
		}
		return false;
	}

	/**
	 * Checks if headers request headers are well-formed.
	 * @param headers The request headers.
	 * @return true if headers have correct form and if method is GET or HEAD. False otherwise.
	 */
	public boolean validateRequest(String headers) {
		String requestLine = findPattern(headers, "^(\\w+) /\\S+ HTTP/1.[10]\r\n");
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
