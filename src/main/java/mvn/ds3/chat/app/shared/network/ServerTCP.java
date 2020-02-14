package mvn.ds3.chat.app.shared.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.ConvertMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerTCP implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerTCP.class);
    private final int port;
    private final ExecutorService exec;
    private final MsgHandler handler;

    public ServerTCP(int port, MsgHandler handler) {
        this.port = port;
        this.exec = Executors.newFixedThreadPool(20);
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            log.info("Starting TCP server on port {}", port);
            ServerSocket server = new ServerSocket(port);
            while (true) {
                Socket socket = server.accept();
                exec.execute(new ConnectionHandler(socket, handler));
            }
        } catch (IOException io) {
            log.error("TCP server error occurred.", io);
            throw new RuntimeException(io);
        }
    }

    public interface MsgHandler {
        String handleUc(Connection connection, Message message);

        List<Class<? extends Message>> getCertainTypesOfMsgUnicast();
    }


    private static class ConnectionHandler implements Runnable {
        private Socket socket;
        private MsgHandler handler;

        private ConnectionHandler(Socket socket, MsgHandler handler) {
            this.socket = socket;
            this.handler = handler;
        }

        @Override
        public void run() {
            try (var socket = this.socket; var outStream = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                 var buffReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String receivedMsg = buffReader.readLine();
                String reply = handler.handleUc(new Connection(socket.getInetAddress(), socket.getPort()),
                        ConvertMsg.deserializeMsg(receivedMsg));
                outStream.write(reply);
                outStream.flush();
            } catch (Exception ex) {
                log.error("Error occurred during TCP request processing.", ex);
            }
        }
    }

}
