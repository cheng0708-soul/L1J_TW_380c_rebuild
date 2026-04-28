// 覆蓋檔案：src/l1j/server/server/utils/StreamUtil.java
// 目的：完整支援舊專案常見呼叫方式（如：StreamUtil.close(_out, _in);）並降噪 SEVERE
package l1j.server.server.utils;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.Channel;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StreamUtil {
	private static final Logger _log = Logger.getLogger(StreamUtil.class.getName());

	private StreamUtil() {}

	// ---- 單一資源關閉（最精確匹配） ----
	public static void close(Closeable c) {
		if (c == null) return;
		try {
			c.close();
		} catch (SocketException e) {
			_log.log(Level.FINER, "Client closed connection during close(): {0}", e.getMessage());
		} catch (Exception e) {
			_log.log(Level.FINE, "Close stream error", e);
		}
	}
	public static void close(Socket socket) {
		if (socket == null) return;
		try {
			socket.close();
		} catch (SocketException e) {
			_log.log(Level.FINER, "Client closed socket: {0}", e.getMessage());
		} catch (Exception e) {
			_log.log(Level.FINE, "Close socket error", e);
		}
	}
	public static void close(ServerSocket serverSocket) {
		if (serverSocket == null) return;
		try {
			serverSocket.close();
		} catch (Exception e) {
			_log.log(Level.FINE, "Close server socket error", e);
		}
	}
	public static void close(DatagramSocket datagramSocket) {
		if (datagramSocket == null) return;
		try {
			datagramSocket.close();
		} catch (Exception e) {
			_log.log(Level.FINE, "Close datagram socket error", e);
		}
	}
	public static void close(Channel channel) {
		if (channel == null) return;
		try {
			channel.close();
		} catch (Exception e) {
			_log.log(Level.FINE, "Close channel error", e);
		}
	}
	public static void close(InputStream in) { close((Closeable) in); }
	public static void close(OutputStream out) { close((Closeable) out); }
	public static void close(Reader r) { close((Closeable) r); }
	public static void close(Writer w) { close((Closeable) w); }

	// ---- 舊呼叫相容：可同時傳多個參數 ----
	// 例如：StreamUtil.close(_out, _in); 或 StreamUtil.close(_out, _in, _socket);
	public static void close(Object... resources) {
		if (resources == null) return;
		for (Object o : resources) {
			if (o == null) continue;
			try {
				if (o instanceof Socket) {
					close((Socket) o);
				} else if (o instanceof ServerSocket) {
					close((ServerSocket) o);
				} else if (o instanceof DatagramSocket) {
					close((DatagramSocket) o);
				} else if (o instanceof Channel) {
					close((Channel) o);
				} else if (o instanceof Closeable) {
					close((Closeable) o);
				} else {
					// 其他型別略過
				}
			} catch (Exception e) {
				_log.log(Level.FINE, "Close resource error", e);
			}
		}
	}
}
