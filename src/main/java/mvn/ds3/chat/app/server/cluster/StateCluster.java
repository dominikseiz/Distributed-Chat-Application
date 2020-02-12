package mvn.ds3.chat.app.server.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.shared.msg.MsgChat;

import java.util.Map;
import java.util.TreeSet;

public class StateCluster {
    private final Map<Long, MsgChat> msgs;
    private final Long sequence;

    public StateCluster(@JsonProperty("messages") Map<Long, MsgChat> msgs, @JsonProperty("sequence") Long sequence) {
        this.msgs = msgs;
        this.sequence = sequence;
    }

    public Map<Long, MsgChat> getMessages() {
        return msgs;
    }

    public Long getSequence() {
        return sequence;
    }

    @JsonIgnore
    public MsgChat getLastMessage() {
        if (msgs.isEmpty()) {
            return null;
        }
        Long lastMessage = new TreeSet<>(msgs.keySet()).last();
        return msgs.get(lastMessage);
    }
}
