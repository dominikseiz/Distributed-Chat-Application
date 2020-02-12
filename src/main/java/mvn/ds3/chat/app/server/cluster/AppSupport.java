package mvn.ds3.chat.app.server.cluster;

import mvn.ds3.chat.app.server.MsgSequence;
import mvn.ds3.chat.app.shared.ConstantValues;
import mvn.ds3.chat.app.shared.HoldbackQ;
import mvn.ds3.chat.app.shared.clock.VectorClock;
import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.MsgChat;
import mvn.ds3.chat.app.shared.msg.GetMissingMsg;
import mvn.ds3.chat.app.shared.network.Connection;
import mvn.ds3.chat.app.shared.network.PublisherMulticast;
import mvn.ds3.chat.app.shared.network.ReceiverMulticast;
import mvn.ds3.chat.app.shared.network.ServerTCP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class AppSupport implements ReceiverMulticast.MsgHandler, ServerTCP.MsgHandler {
    private static final Logger log = LoggerFactory.getLogger(AppSupport.class);

    private static final List<Class<? extends Message>> CERTAIN_TYPES_OF_UC_MSG = List.of(MsgChat.class, MsgSequence.class);

    private static final List<Class<? extends Message>> CERTAIN_TYPES_OF_MC_MSG =
            List.of(MsgChat.class, GetMissingMsg.class);

    private HoldbackQ queue;

    private final VectorClock vClock;
    private final String replicaId;
    private final PublisherMulticast publisher;
    private final Supplier<Boolean> isMaster;
    private final UcSender sender;

    private AtomicLong sequence = new AtomicLong(0);

    public AppSupport(String replicaId, Supplier<Boolean> isMaster, PublisherMulticast publisher,
                       UcSender sender) {
        this.vClock = new VectorClock();
        this.isMaster = isMaster;
        this.publisher = publisher;
        this.replicaId = replicaId;
        this.vClock.setReplicaOfTimestamp(replicaId, BigDecimal.ZERO.longValue());
        this.sender = sender;
        this.queue = new HoldbackQ (publisher);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(queue::getMissingMessages,
                2000, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleMessage(Connection connection, Message message) {

        if (isMaster()) {
            masterMcMessagesHandler(message);
            return;
        }

        if (message instanceof MsgChat) {
            vClockIncrementing();
            MsgChat chatMessage = ((MsgChat) message);
            vClock.unite(chatMessage.getVectorClock());
            queue.add(chatMessage);
            return;
        }
    }

    private void masterMcMessagesHandler(Message message) {
        if (message instanceof GetMissingMsg) {
            var chatMessages = queue.getChatMessages();
            var retransmissionMessage = (GetMissingMsg) message;
            retransmissionMessage.getMissingMsgSequence().stream()
                    .filter(chatMessages::containsKey)
                    .map(chatMessages::get)
                    .forEach(publisher::publish);
        }
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMessage() {
        return CERTAIN_TYPES_OF_MC_MSG;
    }

    @Override
    public String handleUc(Connection connection, Message message) {
        if (isMaster()) {
            return handleMasterMessages(message);
        }

        if (message instanceof MsgSequence) {
            var sequenceOfMessage = ((MsgSequence) message).getSequence();
            setSquenceWhenGreater(sequenceOfMessage);
        }
        return ConstantValues.REPLY_OK;
    }

    private void setSquenceWhenGreater(final Long sequenceToSet) {
        sequence.updateAndGet(internalSequence -> {
            if (internalSequence > sequenceToSet) {
                log.info("Received sequence {} is smaller then the internal sequence {} will be ignored.", sequenceToSet, internalSequence);
                return internalSequence;
            }
            log.info("Received sequence {} is greate then the internal sequence {} will be set.", sequenceToSet, internalSequence);
            return sequenceToSet;
        });
    }

    public void setClusterState(StateCluster cluster) {
        sequence.set(cluster.getSequence());
        queue.addAllMessages(cluster.getMessages());
    }

    private String handleMasterMessages(Message message) {
        if (message instanceof MsgChat) {
        	vClockIncrementing();
            long nextSequence = sequence.incrementAndGet();
            sender.sendMsgToAllComponents(new MsgSequence(nextSequence));
            var chatMessage = ((MsgChat) message).withSequence(nextSequence);
            vClock.unite(chatMessage.getVectorClock());
            vClockIncrementing();
            MsgChat messageToSend = chatMessage.withVectorClock(vClock.copy());
            queue.add(messageToSend);
            publisher.broadcast(messageToSend);
            return ConstantValues.REPLY_OK;
        }
        log.info("Message of type {} is not handled by the leader.", message.getMessageType());
        return ConstantValues.REPLY_OK;
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMsgUnicast() {
        return CERTAIN_TYPES_OF_UC_MSG;
    }

    private boolean isMaster() {
        return isMaster.get();
    }

    public StateCluster getStateCluster() {
        return new StateCluster(queue.getChatMessages(), sequence.get());
    }

    private synchronized void vClockIncrementing() {
        long sequReplica = this.vClock.getReplicaOfTimestamp(replicaId) + 1;
        this.vClock.setReplicaOfTimestamp(replicaId, sequReplica);
    }

	

}
