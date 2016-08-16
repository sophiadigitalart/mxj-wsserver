package wsserver;

import java.io.File;

import com.cycling74.max.Atom;
import com.cycling74.max.MaxObject;

import wsserver.server.WsServer;
import wsserver.util.Consumer;

public class Server extends MaxObject {
	
	private AtomArrayWebsocket wc = new AtomArrayWebsocket(this); 

	private WsServer server = new WsServer()
			.onWebsocket(wc)
			.onOpen(new Runnable() {
				public void run() {
					outlet(0, "start");
					outlet(getInfoIdx(), "started", Atom.newAtom("on port " + server.getPort() + " with webroot " + server.getFileResolver().getRootPath()));
				}
			}).onClose(new Runnable() {
				public void run() {
					outlet(0, "stop");
					outlet(getInfoIdx(), "stopped");
				}
			}).onError(new Consumer<Throwable>() {
				public void accept(Throwable t) {
					outlet(0, "stop");
					outlet(getInfoIdx(), "error", t.getMessage());
					showException("ApiServer error", t);
				}
			});

	
	public Server(Atom[] args) {
		declareAttribute("port", "getPort", "setPort");
		declareAttribute("webroot", "getWebroot", "setWebroot");
		declareIO(1,1);
		createInfoOutlet(true);
	}

	public void start() {
		server.restart();
	}

	public void stop() {
		server.stop();
	}
	
	protected void notifyDeleted() {
		stop();
		super.notifyDeleted();
	}

	protected void list(Atom[] atomArray) {
		wc.send(atomArray);
	}
	
	protected int getPort() {
		return server.getPort();
	}
	
	protected void setPort(int port) {
		if(port < 80 || port > 0xffff) {
			error("ApiServer -> port not in range [80,65535]");
			return;
		}
		outlet(getInfoIdx(), "port", port);
		server.setPort(port);
	}

	protected String getWebroot() {
		return server.getFileResolver().getRootPath();
	}

	protected void setWebroot(String webroot) {
		File f = new File(webroot);
		if(!f.exists() && webroot.contains(":")) f = new File(webroot.substring(webroot.indexOf(":") + 1)); //get rid of drive name from opendialog on osx
		if(!f.isDirectory()) {
			error("ApiServer -> not a directory: " + webroot);
		}
		else {
			outlet(getInfoIdx(), "webroot", webroot);
			server.getFileResolver().setRoot(f);
		}
	}
}
