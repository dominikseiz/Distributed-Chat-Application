package mvn.ds3.chat.app.shared.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.ConvertMsg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class PublisherMulticast {

    private static final Logger log = LoggerFactory.getLogger(PublisherMulticast.class);
    private InetAddress ip;
    private final int mcPort;

    private DatagramSocket socket;

    public PublisherMulticast(InetAddress ip, int mcPort) {
        this.ip = ip;
        this.mcPort = mcPort;
    }

    public void broadcast(Message message) {
        try {
            if (socket == null) {
                socket = new DatagramSocket();
            }
            String messageToSend = ConvertMsg.serializeMsg(message);
            byte[] buffered = messageToSend.getBytes(StandardCharsets.UTF_8);
            DatagramPacket dataPacket = new DatagramPacket(buffered, buffered.length, ip, mcPort);
            log.info("Sending a multicast message of type: {}", message.getMessageType());
            log.info("The raw message is {}", messageToSend);
            socket.send(dataPacket);
        } catch (IOException io) {
            throw new RuntimeException("Failure sending multicast message.", io);
        }
    }

    public void closeSocket() {
        if (null != socket && !socket.isClosed()) {
            socket.close();
        }
    }

}