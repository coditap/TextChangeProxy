package main.java;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class ProxyClientInputStream extends BufferedInputStream {

	private static final Logger LOG = Logger.getLogger(ProxyClientInputStream.class);
	final int UNSUPPORTED = -1;
	final int GET_OR_HEAD = 0;
	final int POST_OR_PUT = 1;
	final int CONNECT = 2;

	/**
	 * Buffer
	 */
	private String buf;
	/**
	 * How many Bytes read?
	 */
	private int nRead = 0;

	/**
	 * one line
	 */
	private String line;
	/**
	 * The length of the header (with body, if one)
	 */
	private int headerLength = 0;
	/**
	 * The length of the (optional) body of the actual request
	 */
	private int contentLength = 0;
	/**
	 * This is set to true with requests with bodies, like "POST"
	 */
	private boolean body = false;

	private static ProxyServer server;

	/**
	 * Connection variables
	 */
	private ProxyHTTPSession connection;
	private InetAddress remoteHostAddress;
	private String remoteHostName;
	private boolean ssl = false;

	private String errordescription;
	private int statuscode;

	public String url;
	public String method;
	public int HTTPversion;
	public int remotePort = 0;
	public int post_data_len = 0;

	public int getHeaderLength() {
		return headerLength;
	}

	public InetAddress getRemoteHost() {
		return remoteHostAddress;
	}

	public String getRemoteHostName() {
		return remoteHostName;
	}

	public ProxyClientInputStream(ProxyServer server, ProxyHTTPSession connection, InputStream a) {
		super(a);
		ProxyClientInputStream.server = server;
		this.connection = connection;
	}

	/**
	 * Handler for the actual HTTP request
	 * 
	 * @exception IOException
	 */
	public int read(byte[] a) throws IOException {
		statuscode = ProxyHTTPSession.SC_OK;

		if (ssl) {
			// jesli to SSL, to po prosty wywolujemy read z klasy buffered input stream i nic nie parsujemy, zawartosc jest szyfrowana
			return super.read(a);
		}
		if (server == null) {
			throw new IOException("Stream closed"); // jesli z jakiegos powodu server jest nullem, to rzucamy wyjatek i aplikacja pada
		}

		boolean start_line = true; // zmienna do oznaczania, ze czytamy pierwsza linie zapytania
		int nChars; // bedziemy tu trzymali ilosc wczytanych znakow

		String rq = "";
		headerLength = 0; // zmienna do przechowywania dlugosci naglowkow
		post_data_len = 0; // zmienna do przechowywania dlugosci danych lecactych postem
		contentLength = 0; // zmienna do przechowywania dlugosci contentu

		nChars = getLine(); // wczytujemy z pliku pierwsza linie
		buf = line; // i przypisujemy ja do bufora

		while (nChars != -1 && nChars > 2) { // dopoki ilosc ostatnio wczytanych bajtow jest wieksza od 2
			if (start_line) { // jesli czytalismy pierwsza linie
				start_line = false; // oznaczamy ja jako przerobiona
				int methodID = server.getHttpMethod(buf); // pobieramy z zapytania metode (get, post, put, connect)
				switch (methodID) { // sprawdzamy
				case UNSUPPORTED:
					statuscode = ProxyHTTPSession.SC_NOT_SUPPORTED; // ustawiamy kod statusu jako nieobslugiwany
					break; // koniec switcha, nastepna linia
				case CONNECT:
					ssl = true; // oznaczamy, ze korzystamy z SSL
				default: // get, head, post, put
					InetAddress host = parseRequest(buf, methodID); // pobieramy hosta do ktorego bylo wykonywane zapytanie
					if (statuscode != ProxyHTTPSession.SC_OK) // jesli status jest nie ok
						break; // blad, kolejna linia

					if (!ssl) {
						/* tworzy nowy request bez nazwy hosta */
						buf = method + " " + url + " " + server.getHttpVersion() + "\r\n";
						nRead = buf.length(); // ilosc wczytanych bajtow ustalamy jako dlugosc requestu
					}
					if (!host.equals(remoteHostAddress)) { // jesli host nie jest hostem zdalnym
						statuscode = ProxyHTTPSession.SC_CONNECTING_TO_HOST; // ustawiamy status jako laczenia
						remoteHostAddress = host; // przypisujemy do hosta zdalnetgo aktualny
					}
				} // end switch
			}// end if(startline)
			else {
				/*-----------------------------------------------
				 * Content-Length parsing
				 *-----------------------------------------------*/
				if (server.startsWith(buf.toUpperCase(), "CONTENT-LENGTH")) {
					String clen = buf.substring(16);
					if (clen.indexOf("\r") != -1)
						clen = clen.substring(0, clen.indexOf("\r"));
					else if (clen.indexOf("\n") != -1)
						clen = clen.substring(0, clen.indexOf("\n"));
					try {
						contentLength = Integer.parseInt(clen);
					} catch (NumberFormatException e) {
						statuscode = ProxyHTTPSession.SC_CLIENT_ERROR;
					}

					if (!ssl)
						body = true; // Note: in HTTP/1.1 any method can have a
					// body, not only "POST"
				} else if (server.startsWith(buf, "Proxy-Connection:")) {

					buf = null;

				} else if (server.startsWith(buf, "Accept-Encoding:")) { //zmiana encodowania, wylaczenie kompresji
					buf = "Accept-Encoding: identity";
					buf += '\n';
					nRead = buf.length();
				}

			}

			if (buf != null) {
				rq += buf;

				headerLength += nRead;
			}
			nChars = getLine(); //wczytujemy nowa linie
			buf = line; //przypisujemuy ja do bufora
		}

		if (nChars != -1) {
			// adds last line (should be an empty line) to the header
			// String
			if (nChars > 0) {
				rq += buf;
				headerLength += nRead;
			}

			if (headerLength == 0) {
				statuscode = ProxyHTTPSession.SC_CONNECTION_CLOSED;
			}

			for (int i = 0; i < headerLength; i++)
				a[i] = (byte) rq.charAt(i);

			if (body) {// read the body, if "Content-Length" given
				post_data_len = 0;
				while (post_data_len < contentLength) {
					a[headerLength + post_data_len] = (byte) read(); // writes data
					// into the
					// array
					post_data_len++;
				}
				headerLength += contentLength; // add the body-length to the
				// header-length
				body = false;
			}
		}

		// return -1 with an error
		return (statuscode == ProxyHTTPSession.SC_OK) ? headerLength : -1;

	}

	/**
	 * metoda wczytujaca liniei i zwracajaca ilosc wczytanych znakow
	 */
	public int getLine() throws IOException {
		int c = 0;
		line = "";
		nRead = 0;
		while (c != '\n') {
			c = read();
			if (c != -1) {
				line += (char) c;
				nRead++;
			} else
				break;
		}
		return nRead;
	}

	/**
	 * Metoda do parsowania pierwszej lini z requestu, wyciaga URL, typ metody i nazwe zdalnego hosta
	 */
	public InetAddress parseRequest(String a, int method_index) {

		int pos;

		String f = "";
		String r_host_name = "";
		String r_port = "";

		url = "";

		if (ssl) {
			// remove CONNECT
			f = a.substring(8);
		} else {
			method = a.substring(0, a.indexOf(" ")); // first word in the line
			pos = a.indexOf(":"); // locate first ":"
			// Proxy request
			f = a.substring(pos + 3); // removes "http://"
		}
		// Strip white spaces
		f = f.replace("\r", "").replace("\n", "");

		int versionp = f.indexOf("HTTP/");
		String HTTPversionRaw;

		// length of "HTTP/x.x": 8 chars
		if (versionp == (f.length() - 8)) {
			// Detect the HTTP version
			HTTPversionRaw = f.substring(versionp + 5);
			if (HTTPversionRaw.equals("1.1"))
				HTTPversion = 1;
			else if (HTTPversionRaw.equals("1.0"))
				HTTPversion = 0;

			// remove " HTTP/x.x"
			f = f.substring(0, versionp - 1);

		} else {
			// bad request: no "HTTP/xxx" at the end of the line
			HTTPversionRaw = "";
		}

		pos = f.indexOf("/"); // locate the first slash
		if (pos != -1) {
			url = f.substring(pos); // saves path without host name
			r_host_name = f.substring(0, pos); // reduce string to the host name
		} else {
			url = "/";
			r_host_name = f;
		}

		// Port number: parse String and convert to integer
		if (r_port != null && !r_port.equals("")) {
			try {
				remotePort = Integer.parseInt(r_port);
			} catch (NumberFormatException e_get_host) {

				remotePort = 80;
			}
		} else {
			remotePort = 80;
		}

		remoteHostName = r_host_name;
		InetAddress address = null;

		LOG.info("Zapytanie do serwera: " + connection.getLocalSocket().getInetAddress().getHostAddress() + " metoda: " + method);

		// Resolve host name
		try {
			address = InetAddress.getByName(remoteHostName);

		} catch (UnknownHostException e_u_host) {
			statuscode = ProxyHTTPSession.SC_HOST_NOT_FOUND;

		}

		if (method_index > 0) {
			statuscode = ProxyHTTPSession.SC_INTERNAL_SERVER_ERROR;
			errordescription = "GET no supported";
		}

		return address;
	}

	/**
	 * @return boolean whether the current connection was established with the CONNECT method.
	 */
	public boolean isTunnel() {
		return ssl;
	}

	/**
	 * @return the full qualified URL of the actual request.
	 */
	public String getFullUrl() {
		return "http" + (ssl ? "s" : "") + "://" + getRemoteHostName() + (remotePort != 80 ? (":" + remotePort) : "") + url;
	}

	/**
	 * @return status-code for the current request
	 */
	public int getStatusCode() {
		return statuscode;
	}

	/**
	 * @return the (optional) error description for this request
	 */
	public String getErrorDescription() {
		return errordescription;
	}
}
