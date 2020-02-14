package mvn.ds3.chat.app.shared.network;

import java.net.InetAddress;

public class Connection {

    private final InetAddress ip;
    private final int port;

    public Connection(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }


    public InetAddress getIP() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Chat message from IP=" + ip;
    }
}
