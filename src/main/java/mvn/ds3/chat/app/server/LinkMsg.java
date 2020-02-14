package mvn.ds3.chat.app.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.shared.msg.Message;

public class LinkMsg extends Message {

    private final String id;
    private final int tcpPort;

    public LinkMsg(@JsonProperty("id") String id, @JsonProperty("tcpPort") int tcpPort) {
        super(LinkMsg.class.getSimpleName());
        this.id = id;
        this.tcpPort = tcpPort;
    }

    public String getId() {
        return id;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    @Override
    public String toString() {
        return "LinkMsg{}";
    }
}
