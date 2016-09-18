package wsserver.server;

import wsserver.util.Consumer;
import wsserver.util.Runner;

public interface Websocket {

	public void onMessage(Consumer<String> messageHandler);
	public void send(String message);
	public void onClose(Runner closeListener);
	public void onError(Consumer<Throwable> errorHandler);
}
