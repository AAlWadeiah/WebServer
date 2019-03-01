/** 
 * Course: CPSC 441 - Computer Networks
 * Semester: Winter 2019
 * Instructor: Majid Ghaderi
 * Assignment: 2
 * 
 * WebServerMode class
 * 
 * @author Abdullah Al-Wadeiah
 */

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

	private File requestedObject;
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
		if (method.equals("HEAD")) {
			sendHEADResponse(getClientOut(), getRequestedObject());
		} else {
			sendGETResponse(getClientOut(), getRequestedObject(), getOffset(), getLength(), getIsRange());
			sendFileRange(getClientOut(), getRequestedObject(), getOffset(), getLength(), getIsRange());
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
	private void sendFileRange(OutputStream out, File obj, Long pos, Integer len, boolean isRange) {
		if (!isRange) {
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
	private boolean checkRange(String headers) {
		String range = findPattern(headers, "Range: bytes=(\\d+-\\d+)");
		if (range.equals("")) {
			return false;
		} else {
			String[] r = range.split("-");
			setOffset(Long.valueOf(r[0]));
			setLength(Integer.valueOf(r[1]));
			setIsRange(true);
			if ((getOffset() > getLength()) || (getOffset() < 0) || (getLength() < 0))
				return false;
			return true;
		}
	}

	/**
	 * Attempts to find the requested object in the server's working directory. If the file is found, the file is created and set.
	 * @param headers The request headers.
	 * @return true if the object was found, and false otherwise
	 */
	private boolean findObject(String headers) {
		String objectPath = findPattern(headers, "^\\w+ /(\\S+) HTTP/1.1\r\n");
		Path path = Paths.get(objectPath);
		if (Files.isRegularFile(path)) {
			setRequestedObject(new File(objectPath));
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
		for (int i = 1; i < splitHeaders.length; i++) {
			boolean match = splitHeaders[i].matches("\\w+-?\\w*: \\S+");
			if (!match)
				return false;
		}
		
		if (headers.contains("\r\nRange:")) {
			boolean goodRange = checkRange(headers);
			if (!goodRange) 
				return false;
		}
		return true;
	}
	
	/**
	 * @return the length
	 */
	public Integer getLength() {
		return length;
	}
	
	/**
	 * @param length the length to set
	 */
	public void setLength(Integer length) {
		this.length = length;
	}
	
	/**
	 * @return the offset
	 */
	public Long getOffset() {
		return offset;
	}
	
	/**
	 * @param offset the offset to set
	 */
	public void setOffset(Long offset) {
		this.offset = offset;
	}
	
	/**
	 * @return the isRange
	 */
	public boolean getIsRange() {
		return isRange;
	}
	
	/**
	 * @param isRange the isRange to set
	 */
	public void setIsRange(boolean isRange) {
		this.isRange = isRange;
	}

	/**
	 * @return the requestedObject
	 */
	public File getRequestedObject() {
		return requestedObject;
	}

	/**
	 * @param requestedObject the requestedObject to set
	 */
	public void setRequestedObject(File requestedObject) {
		this.requestedObject = requestedObject;
	}

}
