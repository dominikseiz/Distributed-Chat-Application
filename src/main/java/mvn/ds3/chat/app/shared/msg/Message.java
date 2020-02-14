package mvn.ds3.chat.app.shared.msg;

public abstract class Message {
    private final String msgType;

    protected Message(String msgType) {
        this.msgType = msgType;
    }

    public String getMessageType() {
        return msgType;
    }

}
