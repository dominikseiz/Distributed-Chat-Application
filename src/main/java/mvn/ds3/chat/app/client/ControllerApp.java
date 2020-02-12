package mvn.ds3.chat.app.client;

import mvn.ds3.chat.app.shared.clock.VectorClock;
import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.MsgChat;
import mvn.ds3.chat.app.shared.msg.GetMsg;
import mvn.ds3.chat.app.shared.msg.GetReply;
import mvn.ds3.chat.app.shared.network.Connection;
import mvn.ds3.chat.app.shared.network.PublisherMulticast;
import mvn.ds3.chat.app.shared.network.ReceiverMulticast;
import mvn.ds3.chat.app.shared.network.ClientTCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ControllerApp implements ReceiverMulticast.MsgHandler {
    private static final Logger log = LoggerFactory.getLogger(ControllerApp.class);

    private final HoldbackQ queue;
    private final PublisherMulticast publisher;
    private Consumer<MsgChat> consumer;
    private boolean startedDelivery = false;
    private InetAddress ip;
    private int serverTcp;
    private VectorClock vClock;
    private final String id;

    public ControllerApp(PublisherMulticast publisher) throws UnknownHostException {
        this.publisher = publisher;
        this.id = UUID.randomUUID().toString();
        this.queue = new HoldbackQ(publisher, chatMessage -> {
            if (consumer != null) {
                // vClock.merge(message.getVectorClock());
                consumer.accept(chatMessage);
            }
        });
        this.vClock = new VectorClock();
        vClock.setReplicaOfTimestamp(id, 0);
    }

    public void setMsgHandler(Consumer<MsgChat> consumer) {
        this.consumer = consumer;
        startDelivery();
    }

    private void startDelivery() {
        if (startedDelivery) {
            return;
        }
        startedDelivery = true;
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(queue::msgDelivery,
                200, 200, TimeUnit.MILLISECONDS);
    }

    public void sendChatMsg(MsgChat msgChat) {
        vClockIncrementing();
        boolean isSuccess = false;
        MsgChat toSend = msgChat.withVectorClock(vClock.copy());
        int nrOfRetries = 0;
        while (!isSuccess) {
            isSuccess = isMsgSent(toSend);
            nrOfRetries = nrOfRetries + 1;
            if (nrOfRetries == 3) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
    }

    public boolean isMsgSent(MsgChat msgChat) {
        if (ip == null) {
            throw new IllegalArgumentException("Server address must not be null.");
        }
        try {
            var client = new ClientTCP(ip, serverTcp);
            client.sendMessage(msgChat);
            return true;
        } catch (Exception ex) {
            log.info("Failed to send chat message to master due to {}. Going to discover new server.", ex.getMessage());
            publisher.broadcast(new GetMsg());
            return false;
        }
    }


    private synchronized void vClockIncrementing() {
        long replicaSequence = this.vClock.getReplicaOfTimestamp(id) + 1;
        this.vClock.setReplicaOfTimestamp(id, replicaSequence);
    }

    @Override
    public void handleMessage(Connection connection, Message message) {
        if (message instanceof MsgChat) {
        	vClockIncrementing();
            vClock.unite(((MsgChat) message).getVectorClock());
            queue.handle(connection, message);
        }
        if (message instanceof GetReply) {
            log.info("Setting server address to {}", connection.getIP());
            var masterReply = (GetReply) message;
            ip = connection.getIP();
            serverTcp = masterReply.getServerPort();
        }
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMessage() {
        return null;
    }

	

}
