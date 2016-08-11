package wsserver.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Sha1 {
	
	static MessageDigest messageDigest;
	static {
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Can't run websocket server: could not get sha-1 MessageDigest");
			e.printStackTrace();
		}
	}
	
	public static String digest64B(String input) {
		synchronized (messageDigest) {
			messageDigest.reset();
			messageDigest.update(input.getBytes());
			return DatatypeConverter.printBase64Binary(messageDigest.digest());
		}
	}
}
