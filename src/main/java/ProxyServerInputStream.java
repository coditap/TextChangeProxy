package main.java;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;

public class ProxyServerInputStream extends BufferedInputStream implements ProxyInputStream {

	public ProxyServerInputStream(ProxyServer server, InputStream a, boolean filter) {
		super(a);
	}

	public int read_f(byte[] b) throws IOException {
		return read(b);
	}
}
