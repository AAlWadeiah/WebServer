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

	public void run() {
		System.out.println("Inside runnable");
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
		try {
			boolean local = isLocal(rawData);
			System.out.println("Local check: " + local);
			if (!local) {
				// create proxyServerMode
				System.out.println("Proxy mode");
				ProxyServerMode proxyRequest = new ProxyServerMode(rawData.toString(), out);
				success = proxyRequest.processRequest();
			} else {
				System.out.println("Web server mode");
				// create WebServerMode
				WebServerMode webRequest = new WebServerMode(rawData.toString(), out);
				success = webRequest.processRequest();
			}
		} catch (UnknownHostException e) {
			System.out.println("Host name error: " + e.getMessage());
			e.printStackTrace();
			close();
			return;
		}
		
		if (success) {
			System.out.println("Successfully processed client request.");
		} else {
			System.out.println("Failed to process client request.");
		}
		
		close();
	}
	
	private boolean isLocal(ByteArrayOutputStream rawData) throws UnknownHostException {
		String rawHeaders = rawData.toString();
		if (rawHeaders.contains("Host: ")) {
			String hostName = findPattern(rawHeaders, "Host: (\\D+):\\d+\\s\\s");
			if(Utils.isLocalHost(hostName)) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
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

	//==============================================================//
	//<<<<<<<<<<<<<<<<<<<<<<< PROBLEM METHOD >>>>>>>>>>>>>>>>>>>>>>>//
	/**
	 * Reads input coming from the InputStream and stores it in a raw format as a ByteArrayOutputStream.
	 * @param is The InputStream to be read from
	 * @return the raw data from the InputStream
	 */
	private ByteArrayOutputStream readRequest(InputStream is) {
		System.out.println("Reading request");
		ByteArrayOutputStream rawData = new ByteArrayOutputStream();
        byte[] chunk = new byte[2048];
        int bytesRead = 0;
		try {
			// NEVER BREAKS OUT OF THIS LOOP //
			while ((bytesRead = is.read(chunk)) > -1) {
				rawData.write(chunk, 0, bytesRead);
				System.out.println("Bytes read: " + bytesRead);
			}
		} catch (IOException e) {
			System.out.println("Error reading client request:" + e.getMessage());
			e.printStackTrace();
			return null;
		}
		System.out.println("Done reading");
		return rawData;
	}
	//<<<<<<<<<<<<<<<<<<<<<<< PROBLEM METHOD >>>>>>>>>>>>>>>>>>>>>>>//
	//==============================================================//

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
