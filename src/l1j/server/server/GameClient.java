package l1j.server.server;

import java.io.IOException;
import java.net.Socket;

/**
 * Minimal GameClient shim for L1J_TW_3.80c cores that reference GameClient.
 * Many code paths in newer patches expect a GameClient type; this class simply
 * extends the existing ClientThread so those references compile without changes.
 *
 * If your core already has additional logic in ClientThread (packet loops,
 * login/auth flow, character loading), you do NOT need to duplicate it here—
 * inheriting from ClientThread is sufficient.
 */
public class GameClient extends ClientThread {

    /**
     * Construct a GameClient using the base ClientThread connection handling.
     * @param socket the accepted client socket
     * @throws IOException if the underlying stream/socket init fails
     */
    public GameClient(Socket socket) throws IOException {
        super(socket);
    }

    // Optional convenience accessors; uncomment if your code expects them.
    // public l1j.server.server.model.Instance.L1PcInstance getActiveChar() {
    //     return super.getActiveChar();
    // }
}
