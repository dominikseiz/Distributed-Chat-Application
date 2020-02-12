package mvn.ds3.chat.app.server;

import mvn.ds3.chat.app.shared.msg.Message;

public class HeartBeatMsg extends Message {

    public HeartBeatMsg() {
        super(HeartBeatMsg.class.getSimpleName());
    }

    @Override
    public String toString() {
        return "HeartBeatMsg{}";
    }
}
