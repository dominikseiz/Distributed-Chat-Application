package mvn.ds3.chat.app.shared.msg;

public abstract class Message {
    private final String messageType;

    protected Message(String msgType) {
        this.messageType = msgType;
    }

    public String getMessageType() {
        return messageType;
    }

}
