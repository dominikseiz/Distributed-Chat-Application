package mvn.ds3.chat.app.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.shared.msg.Message;

public class MsgLeaderElection extends Message {

    private final String id;

    public MsgLeaderElection(@JsonProperty("id") String id) {
        super(MsgLeaderElection.class.getSimpleName());
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
