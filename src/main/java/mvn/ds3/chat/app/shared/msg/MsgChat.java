package mvn.ds3.chat.app.shared.msg;

import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.shared.clock.VectorClock;

public class MsgChat extends Message {
    private final Long sequence;
    private final String text;
    private final String name;
    private final VectorClock vClock;

    public MsgChat(@JsonProperty("sequence") Long sequence, @JsonProperty("text") String text, @JsonProperty("name") String name,
                       @JsonProperty("vClock") VectorClock vClock) {
        super(MsgChat.class.getSimpleName());
        this.text = text;
        this.name = name;
        this.sequence = sequence;
        this.vClock = vClock;
    }

    public static MsgChat of(String text, String name) {
        return new MsgChat(0L, text, name, new VectorClock());
    }

    public MsgChat withSequence(Long sequence) {
        return new MsgChat(sequence, this.text, this.name, this.vClock);
    }

    public MsgChat withVectorClock(VectorClock vClock) {
        return new MsgChat(this.sequence, this.text, this.name, vClock);
    }

    public Long getSequence() {
        return sequence;
    }

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }

    public VectorClock getVectorClock() {
        return vClock;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "sequence=" + sequence +
                ", text='" + text + '\'' +
                ", name='" + name + '\'' +
                "} ";
    }
}
