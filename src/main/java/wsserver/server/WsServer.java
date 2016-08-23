package wsserver.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import wsserver.util.Consumer;
import wsserver.util.IgnoreError;

/*
 * 
 * The Server listens at the given port, creating a new Request for every new Socket.
 * It manages its lifecycle, calling open/close/error-Listeners and running Requests on its ExceutorService.
 *  
 * The webroot can be changed while the server is running.
 * A port change while trigger a restart on a running server.
 * 
 */

public class WsServer {

	enum State { starting, running, stopping, stopped, error }
	
	private State state = State.stopped;
	private ExecutorService executorService;
	private Consumer<Websocket> websocketHandler;
	private Runnable openListener;
	private Runnable closeListener;
	private Consumer<Throwable> errorHandler;
	private final FileResolver fileResolver = new FileResolver();
	private int port = 7475;
	private ServerSocket serverSocket;

	class Loop implements Runnable {
		public void run() {
			try {
				while(true) {
					Socket s = serverSocket.accept();
					executorService.submit(new Request(fileResolver, websocketHandler, s));
				}
			} catch (IOException e) {
				if(state != State.stopping) setError(e);
			}		
		}
	}

	public WsServer onOpen(Runnable openListener) {
		this.openListener = openListener;
		return this;
	}
	
	public WsServer onClose(Runnable closeListener) {
		this.closeListener = closeListener;
		return this;
	}
	
	public WsServer onError(Consumer<Throwable> errorHandler) {
		this.errorHandler = errorHandler;
		return this;
	}
	
	public WsServer onWebsocket(Consumer<Websocket> websocketHandler) {
		this.websocketHandler = websocketHandler;
		return this;
	}

	public FileResolver getFileResolver() {
		return fileResolver;
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		if(port < 80 || port > 0xffff) throw new IllegalArgumentException("port not in range [80,65535]");
		this.port = port;
		if(state == State.starting || state == State.running) restart();
	}

	public synchronized void restart() {
		stop();
		try {
			state = State.starting;
			serverSocket = new ServerSocket(port);
			executorService = Executors.newCachedThreadPool();
			executorService.submit(new WsServer.Loop());
			state = State.running;
			IgnoreError.run(openListener);
		} catch (Exception e) {
			setError(e);
		}
	}
	
	public synchronized void stop() {
		if(state == State.stopping || state == State.stopped) return;
		boolean error = state == State.error;
		state = State.stopping;
		try {
			serverSocket.close();
		} catch (Exception e) {}
		try {
			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.MILLISECONDS);
		} catch (Exception e) {}
		if(!error) IgnoreError.run(closeListener);
		state = State.stopped;
	}
	
	private synchronized void setError(Throwable t) {
		if(state != State.error) IgnoreError.accept(errorHandler, t);
		state = State.error;
		stop();
	}
}
