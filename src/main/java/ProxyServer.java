package main.java;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class ProxyServer implements Runnable {

	private final String HTTP_VERSION = "HTTP/1.1";

	private String httpUserAgent = "Mozilla/4.0 (compatible; MSIE 5.0; WindowsNT 5.1)";
	private ServerSocket listen;

	public final int DEFAULT_SERVER_PORT = 9997;

	public InetAddress proxy;
	public int proxy_port = 0;

	public boolean fatalError;
	private String errorMessage;
	private boolean serverRunning = false;

	void init() {

		System.out.println(" proxy server startup...");

		// create now server socket
		try {
			listen = new ServerSocket(DEFAULT_SERVER_PORT);
		} catch (BindException e_bind_socket) {
			fatalError = true;
			System.out.println("The socket " + DEFAULT_SERVER_PORT + " is already in use " + e_bind_socket.getMessage());
		} catch (IOException e_io_socket) {
			fatalError = true;
			System.out.println("IO Exception occured while creating server socket on port " + DEFAULT_SERVER_PORT + ". " + e_io_socket.getMessage());
		}

		if (fatalError) {
			System.out.println(errorMessage);
			return;
		}

	}

	public ProxyServer() {
		init();
	}

	void serve() {

		serverRunning = true;
		System.out.println("Server running on port " + DEFAULT_SERVER_PORT);
		try {
			while (serverRunning) {
				Socket client = listen.accept();
				new ProxyHTTPSession(this, client);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		serve();
	}

	/**
	 * Sprawdza, jakie typu metoda zosta�a uzyta do polaczenia
	 * 
	 * @return -1 if the server doesn't support the method
	 */
	public int getHttpMethod(String d) {
		if (startsWith(d, "GET") || startsWith(d, "HEAD")) // metody get lub head
			return 0;
		if (startsWith(d, "POST") || startsWith(d, "PUT")) // metody post lub put
			return 1;
		if (startsWith(d, "CONNECT")) // metoda connect - SSL
			return 2;
		return -1;
	}

	/**
	 * metoda do sprawdzania, czy dany string zaczyna sie na podany
	 */
	public boolean startsWith(String a, String what) {
		int l = what.length();
		int l2 = a.length();
		return l2 >= l ? a.substring(0, l).equals(what) : false;
	}

	/**
	 * @return the HTTP version
	 */
	public String getHttpVersion() {
		return HTTP_VERSION;
	}

	/**
	 * the User-Agent header field
	 * 
	 * @return User-Agent String
	 */
	public String getUserAgent() {
		return httpUserAgent;
	}

	public void setUserAgent(String ua) {
		httpUserAgent = ua;
	}

	public String getGMTString() {
		return new Date().toString();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}