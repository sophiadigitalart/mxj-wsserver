package wsserver;

import java.util.HashMap;
import java.util.Map;

import com.cycling74.max.Atom;
import com.cycling74.max.MaxObject;

import wsserver.server.Websocket;
import wsserver.util.Consumer;
import wsserver.util.Runner;

/*
 * This class is used to bridge communication from Websocket to max.
 * Each new websocket gets a unique id and the atom[] [start <id>] is sent to max.
 * On error or close, the atom[] [stop <id>] is sent to max.
 * On ws message, [<id> args...] is sent to max, where args is the output of Atom.parse(message)
 * On max message [<id> args...], Atom.toOneString(args) is sent to the websocket with id <id>.
 * In other words, max atoms translate to websocket messages as text separated by spaces.
 * 
 */


public class AtomArrayWebsocket extends Consumer<Websocket> {
	final static Atom msg = Atom.newAtom("msg");

	private MaxObject maxObject;
	private Map<Integer, Websocket> sockets = new HashMap<Integer, Websocket>();

	public AtomArrayWebsocket(MaxObject maxObject) {
		this.maxObject = maxObject;
	}

	public void send(Atom[] input) {
		int wsid = input[0].getInt();
		Websocket websocket = sockets.get(wsid);
		if(websocket == null) {
			MaxObject.error("Websocket #" + wsid + " not found");
			return;
		}
		websocket.send(Atom.toOneString(Atom.removeFirst(input)));
	}

	@Override
	public void accept(final Websocket websocket) {
		
		final Atom wsid_a = Atom.newAtom(websocket.getId());
		sockets.put(websocket.getId(), websocket);
		maxObject.outlet(0, "start", wsid_a);
		websocket.onMessage(new Consumer<String>() {
			public void accept(String message) {
				Atom[] mess = Atom.parse(message);
				Atom[] output = new Atom[mess.length + 1];
				output[0] = wsid_a;
				System.arraycopy(mess, 0, output, 1, mess.length);
				maxObject.outlet(0, output);
			}
		});

		websocket.onClose(new Runner() {
			public void run() {
				maxObject.outlet(0, "stop", wsid_a);
				sockets.remove(websocket.getId());
			}
		});

		websocket.onError(new Consumer<Throwable>() {
			public void accept(Throwable t) {
				maxObject.outlet(0, "stop", wsid_a);
				sockets.remove(websocket.getId());
				if(!"Socket closed".equals(t.getMessage())) MaxObject.showException("Websocket #" + websocket.getId() + " onError", t);
			}
		});
	}
}
