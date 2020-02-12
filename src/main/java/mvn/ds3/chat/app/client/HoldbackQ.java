package mvn.ds3.chat.app.client;

import mvn.ds3.chat.app.shared.clock.VectorClock;
import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.MsgChat;
import mvn.ds3.chat.app.shared.msg.GetMissingMsg;
import mvn.ds3.chat.app.shared.network.Connection;
import mvn.ds3.chat.app.shared.network.PublisherMulticast;
import mvn.ds3.chat.app.shared.network.ReceiverMulticast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class HoldbackQ implements ReceiverMulticast.MsgHandler {

    private static final Logger log = LoggerFactory.getLogger(HoldbackQ.class);

    private final Map<Long, MsgChat> history = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<MsgChat> prioQ = new PriorityBlockingQueue<>(10,
            new ChatMessageComparator());

    private final PriorityBlockingQueue<MsgChat> orderedDeliveryQueue = new PriorityBlockingQueue<>(10,
            (o1, o2) -> VectorClock.compare(o1.getVectorClock(), o2.getVectorClock()));


    private final Object LOCK = new Object();

    private AtomicLong lastDeliveredMessage = new AtomicLong();
    private PublisherMulticast publisher;
    private Consumer<MsgChat> messageDeliveryHandler;

    public HoldbackQ(PublisherMulticast publisher, Consumer<MsgChat> messageDeliveryHandler) {
        this.publisher = publisher;
        this.messageDeliveryHandler = messageDeliveryHandler;
    }

    @Override
    public void handleMessage(Connection connection, Message message) {
        if (message instanceof MsgChat) {
            MsgChat chatMessage = (MsgChat) message;
            synchronized (LOCK) {
                if (history.containsKey(chatMessage.getSequence())) {
                    return;
                }
                history.put(chatMessage.getSequence(), chatMessage);
                prioQ.put(chatMessage);
                orderedDeliveryQueue.put(chatMessage);
            }
        }
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMessage() {
        return List.of(MsgChat.class);
    }

    public void msgDelivery() {
        if (prioQ.size() == 0) {
            return;
        }
        Long queueHeadSequence = prioQ.peek().getSequence();
        long diff = queueHeadSequence - lastDeliveredMessage.get();
        if (diff == 1) {
            MsgChat messageToDeliver = prioQ.poll();
            Long sequence = messageToDeliver.getSequence();
            lastDeliveredMessage.set(sequence);

            MsgChat messageOrdered = orderedDeliveryQueue.poll();
            log.info("Delivering message with sequence {}. Ordered message sequence is {}", sequence, messageOrdered.getSequence());
            log.info("Vector clock of chat message is {}", messageOrdered.getVectorClock().getReplicaOfTimestamp());
            messageDeliveryHandler.accept(messageOrdered);
        } else {
            List<Long> missingMessageSequences = getRange(lastDeliveredMessage.get(), queueHeadSequence);
            if (missingMessageSequences.isEmpty()) {
                return;
            }
            log.info("There are missing messages for sequences {}. Going to request them.",
                    missingMessageSequences.toString());
            publisher.broadcast(new GetMissingMsg(missingMessageSequences));
        }
    }

    static List<Long> getRange(long startExclusive, long endExclusive) {
        return LongStream.range(startExclusive + 1, endExclusive).boxed().collect(Collectors.toList());
    }

    private static class ChatMessageComparator implements Comparator<MsgChat> {

        @Override
        public int compare(MsgChat m1, MsgChat m2) {
            return m1.getSequence().compareTo(m2.getSequence());
        }
    }

	

}
