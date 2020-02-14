package mvn.ds3.chat.app.shared;

import mvn.ds3.chat.app.shared.msg.MsgChat;
import mvn.ds3.chat.app.shared.msg.GetMissingMsg;
import mvn.ds3.chat.app.shared.network.PublisherMulticast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class HoldbackQ {

    private static final Logger log = LoggerFactory.getLogger(HoldbackQ.class);

    private final Map<Long, MsgChat> chatLog = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<MsgChat> prioQ = new PriorityBlockingQueue<>(10,
            new ChatMessageComparator());


    private final Object keyLock = new Object();

    private AtomicLong lastMsgDelivered = new AtomicLong();
    private PublisherMulticast publisher;

    public HoldbackQ(PublisherMulticast publisher) {
        this.publisher = publisher;
    }

    public void add(MsgChat chatMessage) {
        synchronized (keyLock) {
        	chatLog.put(chatMessage.getSequence(), chatMessage);
        	prioQ.put(chatMessage);
            log.info("{} added.", chatMessage.toString());
        }
    }

    public void addAllMessages(Map<Long, MsgChat> chatMessages) {
        if (chatMessages.isEmpty()) {
            return;
        }
        Long lastMessage = new TreeSet<>(chatMessages.keySet()).last();
        log.info("Adding {} chat messages, and setting last delivered message sequence to {}.", chatMessages.size(), lastMessage);
        lastMsgDelivered.set(lastMessage);
        chatLog.putAll(chatMessages);
    }

    public Map<Long, MsgChat> getChatMessages() {
        return chatLog;
    }

    public void getMissingMessages() {
        if (prioQ.size() == 0) {
            return;
        }
        Long queueSequ = prioQ.peek().getSequence();
        long diff = queueSequ - lastMsgDelivered.get();
        if (diff == 1) {
            MsgChat deliverMsg = prioQ.poll();
            Long msgSequ = deliverMsg.getSequence();
            lastMsgDelivered.set(msgSequ);
        } else {
            List<Long> missingSequ = getRange(lastMsgDelivered.get(), queueSequ);
            if (missingSequ.isEmpty()) {
                return;
            }
            log.info("There are missing messages for sequences {}. Going to request them.",
            		missingSequ.toString());
            publisher.broadcast(new GetMissingMsg(missingSequ));
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
