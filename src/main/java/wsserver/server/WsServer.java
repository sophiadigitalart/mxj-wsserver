package wsserver.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.cycling74.max.MaxObject;

import wsserver.util.Consumer;
import wsserver.util.IgnoreError;
import wsserver.util.Runner;
import wsserver.util.Sha1;

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

	
	@SuppressWarnings("unused")
	public WsServer() {
		//mention all classes that will be used later
		//without it, ClassNotFoundExceptions, invisible errors (methods literally stopping on some line)
		//and even Segfaults in the frozen Device have been observed.
		//call it static class allocation.
		IgnoreError ign = new IgnoreError();
		Loop loop = new Loop();
		Request request = new Request(null,null,null);
		Websocket ws = new Websocket(0, null);
		Sha1 sha = new Sha1();
		Runner r = new Runner();
	}
	
	enum State { starting, running, stopping, stopped, error }
	
	private State state = State.stopped;
	private ExecutorService executorService;
	private Consumer<Websocket> websocketHandler;
	private Runnable openListener;
	private Runnable closeListener;
	private Consumer<Throwable> errorHandler;
	private final FileResolver fileResolver = new FileResolver();
	private int port = 8080;
	private ServerSocket serverSocket;
	private Set<Socket> openSockets = new HashSet<Socket>();

	class Loop implements Runnable {
		public void run() {
			MaxObject.post("WsServer.Loop: accepting connections on port " + serverSocket.getLocalPort());
			try {
				while(true) {
					Iterator<Socket> os = openSockets.iterator();
					while(os.hasNext()) { if(os.next().isClosed()) os.remove(); }
					Socket s = serverSocket.accept();
					openSockets.add(s);
					MaxObject.post("WsServer.Loop: Accepted connection from " + s.getRemoteSocketAddress()); 
					executorService.submit(new Request(fileResolver, websocketHandler, s));
				}
			} catch(Exception e) {
				if(state == State.stopping && e instanceof IOException) return;
				setError(e);
			} finally {
				MaxObject.post("WsServer.Loop: no longer accepting connections");
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
		if(port < 8080 || port > 0xffff) throw new IllegalArgumentException("port not in range [8080,65535]");
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
		for(Socket s : openSockets) IgnoreError.close(s);
		if(!error) IgnoreError.run(closeListener);
		state = State.stopped;
	}
	
	private synchronized void setError(Throwable t) {
		MaxObject.showException("WsServer.setError", t);
		if(state != State.error) IgnoreError.accept(errorHandler, t);
		state = State.error;
		stop();
	}
}
