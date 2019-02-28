package cpsc441.a2;

import java.io.*;
import java.net.*;
import java.util.regex.*;

public class ServerRunnable implements Runnable{
	private Socket sock;
	private OutputStream out;
	private InputStream in;

	public ServerRunnable(Socket s) {
		sock = s;
		out = null;
		in = null;
	}

	@Override
	public void run() {
		try {
			in = new DataInputStream(sock.getInputStream());
			out = new DataOutputStream(sock.getOutputStream());
		}
		catch (UnknownHostException e) {
			System.out.println ("Unknown host error: " + e.getMessage());
			close();
			return;
		}
		catch (IOException e) {
			System.out.println ("Could not create input and output streams: " + e.getMessage());
			close();
			return;
		}

		ByteArrayOutputStream rawData = readRequest(in);
		boolean success;
		boolean local = isLocal(rawData);
		if (!local) {
			// create proxyServerMode
			ProxyServerMode proxyRequest = new ProxyServerMode(rawData.toString(), out);
			success = proxyRequest.processRequest();
		} else {
			// create WebServerMode
			WebServerMode webRequest = new WebServerMode(rawData.toString(), out);
			success = webRequest.processRequest();
		}

		if (success) {
			System.out.println("Successfully processed client request.\n");
		} else {
			System.out.println("Failed to process client request.\n");
		}

		close();
	}

	/**
	 * Checks if request is for the local server. 
	 * @param rawData
	 * @return true if no Host header is found or if Host header is found and the host name is local; false otherwise.
	 */
	private boolean isLocal(ByteArrayOutputStream rawData){
		String rawHeaders = rawData.toString();
		if (rawHeaders.contains("Host: ")) {
			String hostName = findPattern(rawHeaders, "Host: (\\S+):?");
			try {
				if(Utils.isLocalHost(hostName)) {
					return true;
				} else {
					return false;
				}
			} catch (UnknownHostException e) {
				System.out.println("Non-local host");
			}
		} else {
			return true;
		}
		return false;
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
	 * Reads input coming from the InputStream and stores it in a raw format as a ByteArrayOutputStream.
	 * @param is The InputStream to be read from
	 * @return the raw data from the InputStream
	 */
	private ByteArrayOutputStream readRequest(InputStream is) {
		ByteArrayOutputStream rawData = new ByteArrayOutputStream();
		byte[] chunk = new byte[2048];
		int bytesRead = 0;
		try {
			while ((bytesRead = is.read(chunk)) > -1) {
				rawData.write(chunk, 0, bytesRead);
				String temp = rawData.toString();
				if (temp.contains("\r\n\r\n"))
					break;

			}
		} catch (IOException e) {
			System.out.println("Error reading client request:" + e.getMessage());
			e.printStackTrace();
			return null;
		}
		return rawData;
	}

	/**
	 * Shuts down the socket connection
	 */
	public void close() {
		try {
			sock.close();
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		} 
		catch (IOException e) {
			System.out.println(e);
			return;
		}	
	}

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return sock;
	}

}
