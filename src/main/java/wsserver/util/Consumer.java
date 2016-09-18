package wsserver.util;

/*
 * Interface Consumer is replaced with class so it can be dynamically instantiated without creating a new class.
 * See comment in wsserver.server.WsServer.
 * 
 */

public class Consumer<T> {
	
	public void accept(T t) {
		throw new UnsupportedOperationException("Not implemented");
	}

}
