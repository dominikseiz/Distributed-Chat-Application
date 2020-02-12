package mvn.ds3.chat.app.server.cluster;

import mvn.ds3.chat.app.server.HeartBeatMsg;
import mvn.ds3.chat.app.server.LinkAcknowledged;
import mvn.ds3.chat.app.server.LinkMsg;
import mvn.ds3.chat.app.server.ComponentMsg;
import mvn.ds3.chat.app.shared.ConstantValues;
import mvn.ds3.chat.app.shared.clock.VectorClock;
import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.MsgChat;
import mvn.ds3.chat.app.shared.msg.GetMsg;
import mvn.ds3.chat.app.shared.msg.GetReply;
import mvn.ds3.chat.app.shared.network.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class ComponentSupport implements ReceiverMulticast.MsgHandler, ServerTCP.MsgHandler {
    private static final Logger log = LoggerFactory.getLogger(ComponentSupport.class);

    private static final List<Class<? extends Message>> CERTAIN_TYPES_OF_UC_MSG = List.of(ComponentMsg.class, LinkAcknowledged.class);
    private final List<Class<? extends Message>> CERTAIN_TYPES_OF_MC_MSG = List.of(
            LinkMsg.class, GetMsg.class);

    private final Map<String, Component> components = new ConcurrentHashMap<>();
    private final ServerMaster server;
    private final VectorClock vClock;
    private final String id;
    private final Component component;
    private final PublisherMulticast publisher;
    private final UcSender ucSender;
    private Supplier<StateCluster> state;
    private Consumer<StateCluster> receivingStateOfCluster;

    public ComponentSupport(Component component, ServerMaster server, PublisherMulticast publisher) {
        this.vClock = new VectorClock();
        this.server = server;
        this.publisher = publisher;
        this.id = component.getId();
        this.component = component;
        this.vClock.setReplicaOfTimestamp(id, BigDecimal.ZERO.longValue());
        this.ucSender = new UcSender(this::getReplicas, new FailureOfTransferHandler());
        this.components.put(id, component);
        detectFailure();
    }

    public UcSender getUcSender() {
        return ucSender;
    }

    private void detectFailure() {
        Runnable failureDetector = () -> {
            if (server.isMaster()) {
                ucSender.sendMsgToAllComponents(new HeartBeatMsg());
            }
        };
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(failureDetector, 2000, 1000, TimeUnit.MILLISECONDS);
    }

    public Set<Component> getReplicas() {
        var replicas = new HashMap<>(components);
        replicas.remove(id);
        return new HashSet<>(new TreeMap<>(replicas).values());
    }

    public Set<Component> getComponents() {
        return new HashSet<>(new TreeMap<>(components).values());
    }

    private synchronized void vClockIncrementing() {
        long sequenceReplica = this.vClock.getReplicaOfTimestamp(id) + 1;
        this.vClock.setReplicaOfTimestamp(id, sequenceReplica);
    }

    private synchronized void addComponents(ComponentMsg msgComponent) {
        if (vClock.afterwards(msgComponent.getVectorClock())) {
            log.info("Components message is set after the current clock and will be ignored. Component in message is {}.",
            		msgComponent.toString());
            return;
        }
        vClock.unite(msgComponent.getVectorClock());
        log.info("Current clock state is {}", vClock);
        this.components.clear();
        msgComponent.getComponents().forEach(component -> components.put(component.getId(), component));
        log.info("Members are: {}", getComponentIds());
    }

    private Set<String> getComponentIds() {
        return getComponents().stream().map(Component::toString).collect(Collectors.toSet());
    }

    @Override
    public void handleMessage(Connection connection, Message message) {
        if (server.isMaster()) {
            masterMsgHandler(connection, message);
            //  return;
        }
        //throw new UnsupportedOperationException("Message of type " + message.getType() + " is not handled.");
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMcMsg() {
        return CERTAIN_TYPES_OF_MC_MSG;
    }

    private void masterMsgHandler(Connection connection, Message message) {
        if (message instanceof LinkMsg) {
            var connectMsg = (LinkMsg) message;
            if (connectMsg.getId().equals(id)) {
                log.info("LinkMsg has same id as this instance and will be ignored.");
                return;
            }
            // 1. Create the member
            Component component = new Component(connectMsg.getId(), connection.getIP().getHostAddress(), connectMsg.getTcpPort());
            var client = new ClientTCP(connection.getIP(), connectMsg.getTcpPort());
            try {
                //2. Check if member is healthy
            	client.sendMessage(new LinkAcknowledged(state.get()));
            } catch (IOException io) {
                log.info("Component with ID {} and address {}:{} was not reachable and will be removed.", component.getId(), component.getIP(), component.getTcpPort());
            }
            //3. Add member to the members list
            components.put(component.getId(), component);
            //4. Multicast the new members list
            vClockIncrementing();
            ucSender.sendMsgToAllComponents(new ComponentMsg(getComponents(), vClock.copy()));
            log.info("Components are: {}", getComponentIds());
            return;
        }

        if (message instanceof GetMsg) {
            log.info("Sending GetMasterMsgReply over multicast.");
            publisher.broadcast(new GetReply(component.getTcpPort()));
            MsgChat last = state.get().getLastMessage();
            if (last != null) {
                publisher.broadcast(last);
            }
            return;
        }
        throw new UnsupportedOperationException("Type of message " + message.getMessageType() + " is not handled.");
    }

    public void setStateOfCluster(Supplier<StateCluster> state) {
        this.state = state;
    }

    public void setStateOfClusterReceivingHandler(Consumer<StateCluster> receivingStateOfCluster) {
        this.receivingStateOfCluster = receivingStateOfCluster;
    }

    @Override
    public String handleUc(Connection connection, Message message) {
        if (message instanceof LinkAcknowledged) {
            // When a acknowledgement for a Join message is received
            // a master is already available. Therefore this instance is going to be marked as replica.
            log.info("Master server already available. This is going to be a replica.");
            server.setIsMaster(false);
            LinkAcknowledged ackMessage = (LinkAcknowledged) message;
            receivingStateOfCluster.accept(ackMessage.getStateOfCluster());
            return ConstantValues.REPLY_OK;
        }

        if (server.isMaster()) {
            return ConstantValues.REPLY_OK;
        }
        if (message instanceof ComponentMsg) {
        	vClockIncrementing();
            addComponents((ComponentMsg) message);
        }
        return ConstantValues.REPLY_ERR;
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesofUCMsg() {
        return CERTAIN_TYPES_OF_UC_MSG;
    }

    public void sendLinkMsg() {
        publisher.broadcast(new LinkMsg(component.getId(), component.getTcpPort()));
    }

    private class FailureOfTransferHandler implements Consumer<Component> {

        @Override
        public void accept(Component component) {
            log.info("Component with ID {} and address {}:{} was not reachable and will be removed.", component.getId(), component.getIP(), component.getTcpPort());
            components.remove(component.getId());
            ucSender.sendMsgToAllComponents(new ComponentMsg(getComponents(), vClock));
        }
    }

}
