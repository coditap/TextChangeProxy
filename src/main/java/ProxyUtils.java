package main.java;
import java.util.Random;

public class ProxyUtils {

	static final String CRLF = "\r\n";

	public static String urlDecoder(String encoded) {
		StringBuffer decoded = new StringBuffer();
		int len = encoded.length();
		for (int i = 0; i < len; ++i) {
			if (encoded.charAt(i) == '%' && i + 2 < len) {
				int d1 = Character.digit(encoded.charAt(i + 1), 16);
				int d2 = Character.digit(encoded.charAt(i + 2), 16);
				if (d1 != -1 && d2 != -1)
					decoded.append((char) ((d1 << 4) + d2));
				i += 2;
			} else if (encoded.charAt(i) == '+')
				decoded.append(' ');
			else
				decoded.append(encoded.charAt(i));
		}
		return decoded.toString();
	}

	// Random number generator for the following functions
	static Random rnd = new Random();

	/**
	 * creates a random string with given length
	 * 
	 */
	static String randomStringWithCharset(int length, String code) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(code.charAt(rnd.nextInt(code.length())));
		return sb.toString();
	}

	static String randomString(int length) {
		String availableChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		return randomStringWithCharset(length, availableChars);
	}

	/**
	 * 
	 * @return a random String with 16 characters
	 */
	static String randomString() {
		return randomString(16);
	}

}