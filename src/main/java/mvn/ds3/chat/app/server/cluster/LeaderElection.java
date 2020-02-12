package mvn.ds3.chat.app.server.cluster;

import mvn.ds3.chat.app.server.HeartBeatMsg;
import mvn.ds3.chat.app.server.MsgLeaderElected;
import mvn.ds3.chat.app.server.MsgLeaderElection;
import mvn.ds3.chat.app.shared.ConstantValues;
import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LeaderElection implements ServerTCP.MsgHandler, ReceiverMulticast.MsgHandler {

    private static final Logger log = LoggerFactory.getLogger(MsgLeaderElection.class);

    private static final List<Class<? extends Message>> CERTAIN_TYPES_OF_UC_MSG =
            List.of(MsgLeaderElection.class, HeartBeatMsg.class);

    private static final List<Class<? extends Message>> CERTAIN_TYPES_OF_MC_MSG =
            List.of(MsgLeaderElected.class);


    private volatile boolean isElectionRunning = false;
    private final String id;
    private final ServerMaster server;
    private final Supplier<Set<Component>> components;
    private final PublisherMulticast publisher;
    private final FailureDetector failureDetector;

    public LeaderElection(Component thisInstanceComponent,
                                 ServerMaster server,
                                 Supplier<Set<Component>> components,
                                 PublisherMulticast publisher) {
        this.id = thisInstanceComponent.getId();
        this.server = server;
        this.components = components;
        this.publisher = publisher;
        this.failureDetector = new FailureDetector(new FailureDetectionListener());
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(failureDetector, 2000, 100, TimeUnit.MILLISECONDS);
    }


    @Override
    public String handleUc(Connection connection, Message message) {
        if (message instanceof MsgLeaderElection) {
            var electionMessage = (MsgLeaderElection) message;

            int electionIdCompareResult = electionMessage.getId().compareTo(id);
            log.info("The election ID comparision between this component with ID {} and the received ID {} is {}.",
                    id, electionMessage.getId(), electionIdCompareResult);
            // When this instance receives it's own id, the election is finished.
            if (electionIdCompareResult == 0) {
                publisher.broadcast(new MsgLeaderElected(id));
            }
            Set<Component> components = this.components.get();
            // When the received id is bigger then the the id of this instance the received message will be send to
            // the next member.
            if (electionIdCompareResult > 0) {
                sendElectionMessage(components, id, electionMessage);
                return ConstantValues.REPLY_OK;
            }
            // When the received id is smaller, then the id of this instance will be send to the next member.
            if (electionIdCompareResult < 0) {
                sendElectionMessage(components, id, new MsgLeaderElection(id));
                return ConstantValues.REPLY_OK;
            }
        }

        if (message instanceof HeartBeatMsg) {
            failureDetector.receivedHeartBeatMsg();
            log.info("Received HeartBeatMessage responding with OK.");
            return ConstantValues.REPLY_OK;
        }
        return String.format("%s : Message of type %s is not supported by the %s handler. ",
                ConstantValues.REPLY_ERR, message.getMessageType(), this.getClass().getSimpleName());
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfUcMsg() {
        return CERTAIN_TYPES_OF_UC_MSG;
    }

    @Override
    public void handleMessage(Connection connection, Message message) {
        if (message instanceof MsgLeaderElected) {
            isElectionRunning = false;
            var electedMessage = (MsgLeaderElected) message;
            String electedLeaderId = electedMessage.getElectedLeaderId();
            log.info("The component with address {} is elected as the new leader.", electedLeaderId);
            server.setIsMaster(electedLeaderId.equals(this.id));
        }
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMcMsg() {
        return CERTAIN_TYPES_OF_MC_MSG;
    }

    public void startLeaderElection() {
        if (isElectionRunning) {
            return;
        }
        isElectionRunning = true;
        Set<Component> components = this.components.get();
        sendElectionMessage(components, id, new MsgLeaderElection(id));
    }

    public void sendElectionMessage(Set<Component> components, String targetId, MsgLeaderElection electionMessage) {
        Component target = getNextComponent(components, targetId);
        try {
            log.info("Sending election message with ID {} to the next component with address {}.",
                    electionMessage, target.getIP());
            var tcpClient = new ClientTCP(InetAddress.getByName(target.getIP()), target.getTcpPort());
            tcpClient.sendMessage(electionMessage);
        } catch (IOException io) {
            log.info("Component {} was not reachable for the leader election message and will be removed. Leader election message is sent to the next component.", target);
            components.remove(target);
            sendElectionMessage(components, targetId, electionMessage);
        }
    }

    static Component getNextComponent(Set<Component> components, String id) {
        return components.stream()
                .filter(value -> value.getId().compareTo(id) > 0)
                .findFirst()
                .orElse(components.iterator().next());
    }

    private class FailureDetectionListener implements Consumer<Long> {

        @Override
        public void accept(Long accepted) {
            log.info("Nothing heard from the master server for {} seconds. Starting leader election.", accepted);
            startLeaderElection();
        }

		
    }

    private class FailureDetector implements Runnable {

        private long lastTimeHeartBeatMsgReceived = System.currentTimeMillis();
        private Consumer<Long> onFailureDetected;

        public FailureDetector(Consumer<Long> onFailureDetected) {
            this.onFailureDetected = onFailureDetected;
        }

        public void receivedHeartBeatMsg() {
        	lastTimeHeartBeatMsgReceived = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        }

        @Override
        public void run() {
            if (server.isMaster()) {
                return;
            }
            long secDiff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastTimeHeartBeatMsgReceived;
            if (secDiff > 2) {
                onFailureDetected.accept(secDiff);
            }
        }
    }
}
