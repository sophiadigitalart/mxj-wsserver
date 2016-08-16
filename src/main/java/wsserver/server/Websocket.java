package wsserver.server;

import wsserver.util.Consumer;

public interface Websocket {

	public void onMessage(Consumer<String> messageHandler);
	public void send(String message);
	public void onClose(Runnable closeListener);
	public void onError(Consumer<Throwable> errorHandler);
}
