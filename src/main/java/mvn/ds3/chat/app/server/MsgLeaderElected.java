package mvn.ds3.chat.app.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.shared.msg.Message;

public class MsgLeaderElected extends Message {
    private final String leaderId;

    public MsgLeaderElected(@JsonProperty("leaderId") String leaderId) {
        super(MsgLeaderElected.class.getSimpleName());
        this.leaderId = leaderId;
    }
    
    public String getElectedLeaderId() {
        return leaderId;
    }
}
