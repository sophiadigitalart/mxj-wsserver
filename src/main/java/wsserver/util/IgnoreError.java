package wsserver.util;

import java.io.Closeable;
import java.net.Socket;

public class IgnoreError {

	public static void run(Runnable r) {
		try {
			r.run();
		} catch(Exception e) {}
	}
	
	public static void run(Runner r) {
		try {
			r.run();
		} catch(Exception e) {}
	}
	
	public static <T extends Object> void accept(Consumer<T> consumer, T object) {
		try {
			consumer.accept(object);
		} catch(Exception e) {}
	}
	
	public static void close(Socket socket) {
		try {
			socket.close();
		} catch(Exception e) {};
	}
	
	public static void close(Closeable c) {
		try {
			c.close();
		} catch(Exception e) {};
	}
}
