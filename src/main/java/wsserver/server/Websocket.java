package wsserver.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import wsserver.util.Consumer;
import wsserver.util.IgnoreError;
import wsserver.util.Runner;


public class Websocket {

	private final int wsid;
	private final Socket socket;
	private Consumer<String> messageHandler;
	private Runner closeListener;
	private Consumer<Throwable> errorHandler;

	public Websocket(int request_id, Socket socket) {
		wsid = request_id;
		this.socket = socket;
	}

	public int getId() {
		return wsid;
	}
	
	public void onMessage(Consumer<String> handler) {
		this.messageHandler = handler;
	}

	public void onClose(Runner listener) {
		this.closeListener = listener;
	}
	
	public void onError(Consumer<Throwable> errorHandler) {
		this.errorHandler = errorHandler;
	}
	
	public void run() {
		try {
			socket.setSoTimeout(0);
			InputStream in = socket.getInputStream();
			
			int byte_in;
			int[] masks = new int[4];
			
			int message_length;
			while ((byte_in = in.read()) != -1) {
				int opcode = byte_in & 0x0f;
				if ((byte_in = in.read()) < 0x80) throw new IOException("can't read: illegal unmasked message");
				if (byte_in == 0xff) throw new IOException("won't read: message length exceeds 65535");
				if(byte_in < 0xfe) message_length = byte_in - 128;
				else message_length = ((in.read() << 8) | in.read());
				if(message_length > 4096) throw new IOException("won't read: message length exceeds 4096");

				for (int x=0;x<4;x++) masks[x] = in.read();
				char[] mess = new char[message_length];
				for (int x=0;x<message_length;++x) mess[x] = (char) (masks[x%4] ^ in.read());
				switch(opcode) {
				case 1: // text
					messageHandler.accept(new String(mess)); break;
				case 8: //close
					close(); return;
				case 9: //ping
					if(message_length > 124) throw new IOException("won't read: ping message to long: " + message_length);
					socket.getOutputStream().write((byte) 0x8a);
					socket.getOutputStream().write((byte) message_length);
					socket.getOutputStream().write(new String(mess).getBytes());
					socket.getOutputStream().flush();
					break;
				default:
					throw new IOException("won't read: ws opcode not implemented: " +  opcode);
				}
				
				
			}
			close();
		} catch(Exception e) {
			close(e);
		}
	}

	public void close() {
		close(null);
	}
	
	private synchronized void close(Throwable err) {
		if(err != null && !"Socket closed".equals(err.getMessage())) IgnoreError.accept(errorHandler, err);
		else IgnoreError.run(closeListener);
		IgnoreError.close(socket);
	}

	public void send(String message) {
		
		try {
			byte[] bytes = message.getBytes();
			int header_length;
			byte[] to_write;
			if(bytes.length > 4096) {
				throw new Exception("won't write: message length exceeds 4096");
			}
			if (bytes.length > 0x7d) {
				header_length = 4;
				to_write = new byte[header_length + bytes.length];
				to_write[1] = (byte) 0x7e;
				to_write[2] = (byte) (bytes.length >>> 8);
				to_write[3] = (byte) (bytes.length & 0xff);
			} else {
				header_length = 2;
				to_write = new byte[header_length + bytes.length];
				to_write[1] = (byte) bytes.length;
			}
			to_write[0] = (byte) 0x81;
			System.arraycopy(bytes, 0, to_write, header_length, bytes.length);

			socket.getOutputStream().write(to_write);
			socket.getOutputStream().flush();
		} catch (Exception e) {
			close(e);
		}
	}
}
