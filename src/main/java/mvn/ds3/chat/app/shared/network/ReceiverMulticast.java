package mvn.ds3.chat.app.shared.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.ConvertMsg;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

public class ReceiverMulticast implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ReceiverMulticast.class.getName());

    private InetAddress interfaceNetwork;
    private MsgHandler msgHandler;
    private int mcPort;
    private InetAddress addressGroup;

    public ReceiverMulticast(int mcPort, InetAddress addressGroup, InetAddress interfaceNetwork, MsgHandler msgHandler) {
        this.mcPort = mcPort;
        this.addressGroup = addressGroup;
        this.interfaceNetwork = interfaceNetwork;
        this.msgHandler = msgHandler;
    }


    public interface MsgHandler {
        void handleMessage(Connection connection, Message message);

        List<Class<? extends Message>> getCertainTypesOfMessage();
    }

    public void run() {
        try {
            MulticastSocket mcSocket = new MulticastSocket(mcPort);
            mcSocket.setLoopbackMode(false);
            mcSocket.setBroadcast(true);
            //TODO add network interface ip address
            mcSocket.setInterface(interfaceNetwork);
            //set -Djava.net.preferIPv4Stack=true
            mcSocket.setTimeToLive(1);
            //   InetSocketAddress inetSocketAddress = new InetSocketAddress(group, multicastPort);
            //socket.joinGroup(inetSocketAddress, NetworkInterface.getByName("192.168.0.192"));
            mcSocket.joinGroup(addressGroup);
            log.info("Listening for multicast messages on port {} and in group {}.", mcPort, addressGroup.getHostAddress());
            while (true) {
                byte[] buffered = new byte[1024];
                DatagramPacket dataPacket = new DatagramPacket(buffered, buffered.length);
                mcSocket.receive(dataPacket);
                String msgReceived = new String(dataPacket.getData(), 0, dataPacket.getLength());
                msgHandler.handleMessage(new Connection(dataPacket.getAddress(), dataPacket.getPort()), ConvertMsg.deserializeMsg(msgReceived));
            }
        } catch (Exception re) {
            throw new RuntimeException(re);
        }
    }

}
