package mvn.ds3.chat.app.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.shared.msg.Message;

public class MsgSequence extends Message {

    private final Long sequence;

    public MsgSequence(@JsonProperty("sequence") Long sequence) {
        super(MsgSequence.class.getSimpleName());
        this.sequence = sequence;
    }

    public Long getSequence() {
        return sequence;
    }
}
