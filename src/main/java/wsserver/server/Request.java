package wsserver.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import com.cycling74.max.MaxObject;

import wsserver.util.Consumer;
import wsserver.util.IgnoreError;
import wsserver.util.Sha1;


/*
 * 
 * A Request handles a single socket connection from the Server.
 * 
 * For HTTP GET Requests, it tries to find a File from the FileResolver and respond with that file. 
 * For Websocket Requests, it calls the Consumer<Websocket> provided in onWebsocket(websocketConsumer).
 * All other requests are ignored.
 * 
 */

public class Request implements Runnable {
	static String wsIdent = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	private final Consumer<Websocket> websocketHandler;
	private final FileResolver fileResolver;
	private final Socket socket;
	private String path;
	private Map<String,String> headers = new HashMap<String,String>();

	public Request(FileResolver fileResolver, Consumer<Websocket> websocketHandler, Socket socket) {
		this.websocketHandler = websocketHandler;
		this.fileResolver = fileResolver;
		this.socket = socket;
	}

	public void run() {
		try {
			socket.setSoTimeout(1000);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = in.readLine();
			String[] firstLine = line.split(" ");
			String method = firstLine[0];
			path = firstLine[1];

			if(!"GET".equals(method)) throw new Exception("not a GET request");
			while((line = in.readLine()) != null) {
				if(line.isEmpty()) break;
				String[] sp = line.split(":", 2);
				if(sp.length != 2) throw new Exception("Can't parse Header line: " + line);
				headers.put(sp[0].trim(), sp[1].trim());
			}
			MaxObject.post("Request: handle request from " + socket.getRemoteSocketAddress() + " with path " + path);
			if("websocket".equals(headers.get("Upgrade"))) {
				MaxObject.post("Request: Upgrade to websocket");
				upgrade();
				Websocket ws = new Websocket(socket);
				websocketHandler.accept(ws);
				ws.run();
			}
			else respond(path);
		} catch(Exception e) {
			if(e instanceof SocketTimeoutException) {
				MaxObject.post("Request: Socket read timed out");
				return;
			}
			MaxObject.showException("Request: Unexpected Exception", e);
		} finally {
			IgnoreError.close(socket);
		}
	}

	public void upgrade() throws IOException {
		String digestheader = Sha1.digest64B(headers.get("Sec-WebSocket-Key") + wsIdent);
		OutputStream out = socket.getOutputStream();
		out.write("HTTP/1.1 101 Web Socket Protocol Handshake\r\n".getBytes());
		out.write("Upgrade: WebSocket\r\nConnection: Upgrade\r\n".getBytes());
		out.write("Sec-WebSocket-Accept: ".getBytes());
		out.write(digestheader.getBytes()); out.write("\r\n".getBytes());
		out.write(headers.get("Host").getBytes()); out.write("\r\n".getBytes());
		out.write("\r\n".getBytes());
	}
	
	public void respond(final String path) throws IOException {
		File file = fileResolver.get(path);
		OutputStream os = socket.getOutputStream();
		if(file == null) {
			MaxObject.post("Request: Respond with 404 for request path " + path);
			os.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
			return;
		}
		MaxObject.post("Request: Respond with file " + file.getAbsolutePath() + " for request path " + path);

		String name = file.getName();
		String ext = name.substring(name.lastIndexOf('.') + 1);
		if("jpg".equals(ext)) ext = "jpeg";

		os.write("HTTP/1.1 200 OK\r\n".getBytes());
		os.write(("Host: " + headers.get("Host") + "\r\n").getBytes());
		if("jpg".equals(ext) || "gif".equals(ext) || "png".equals(ext)) {
			os.write("Accept-Ranges: bytes\r\n".getBytes());
			os.write(("Content-Length: " + file.length() + "\r\n").getBytes());
			os.write(("Content-Type: image/" + ext + "\r\n").getBytes()); 
		}
		else if("css".equals(ext)) os.write("Content-Type: text/css\r\n".getBytes());
		else if("js".equals(ext)) os.write("Content-Type: application/javascript\r\n".getBytes());
		else if("html".equals(ext)) os.write("Content-Type: text/html\r\n".getBytes()); 
		os.write("\r\n".getBytes());
		writeFile(file, socket.getOutputStream());
		os.flush();
	}

	private static void writeFile(File file, OutputStream os) throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			byte[] buffer = new byte[4096];
			int length = 0;
			while ((length = in.read(buffer)) != -1) {
				os.write(buffer, 0, length);
			}
		} finally {
			IgnoreError.close(in);
		}
	}
}
