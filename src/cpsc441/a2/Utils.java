/**
 * Utils Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author 	Majid Ghaderi
 *
 */

package cpsc441.a2;

import java.net.*;
import java.util.*;
import java.text.*;
import java.io.*;
import java.nio.file.*;

public class Utils {
	
	private final static String dateFormat = "EEE, dd MMM yyyy hh:mm:ss zzz";
	private static Set<InetAddress> localAddresses = enumLocalAddresses();

	
	/**
	 * Checks if host name specified by the parameter host refers to the local host
	 * 
	 * @param host	The host name to compare against the local host
	 * @return boolean Returns true, if host refers to the local host; false otherwise
	 * @throws UnknownException On input error
	 * 
	 */
	public static boolean isLocalHost(String host) throws UnknownHostException {
		InetAddress ip = InetAddress.getByName(host);
		return localAddresses.contains(ip);
	}

	
	/**
	 * Returns the current date of the system
	 * 
	 * @return String The current date formatted as string
	 * 
	 */
	public static String getCurrentDate() {
		return dateLongToString(System.currentTimeMillis());
	}

	
	/**
	 * Returns the content type of the object
	 * 
	 * @param object The File object to be probed for its type
	 * @return String Type of the object
	 * 
	 */
	public static String getContentType(File object) throws IOException { 
		return Files.probeContentType(object.toPath());	
	}


	/**
	 * Returns the last modified date of the object
	 * 
	 * @param object The File object to be probed for its last modified date
	 * @return String Last modified date of the object
	 * 
	 */
	public static String getLastModified(File object) {
		return dateLongToString(object.lastModified());
	}

	
	// Coverts a date from long (in milli seconds) format to a string format
	private static String dateLongToString(long longDate) {
		SimpleDateFormat simple = new SimpleDateFormat(dateFormat);
		return simple.format(new Date(longDate));
	}

	
	// Enumerates all IP addresses of the local host
	private static HashSet<InetAddress> enumLocalAddresses() {
		HashSet<InetAddress> addresses = new HashSet<InetAddress>();

		try {
			Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
			
			for (NetworkInterface nic : Collections.list(netInterfaces)) {
			    Enumeration<InetAddress> inetAddresses = nic.getInetAddresses();
			    addresses.addAll(Collections.list(inetAddresses));
			}
		} catch (SocketException e) {
			throw new ExceptionInInitializerError(e);
		}
		
		return addresses;
	}
	
}
