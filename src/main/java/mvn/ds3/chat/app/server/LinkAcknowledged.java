package mvn.ds3.chat.app.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.server.cluster.StateCluster;
import mvn.ds3.chat.app.shared.msg.Message;

public class LinkAcknowledged extends Message {
    private final StateCluster cluster;

    public LinkAcknowledged(@JsonProperty("cluster") StateCluster cluster) {
        super(LinkAcknowledged.class.getSimpleName());
        this.cluster = cluster;
    }

    public StateCluster getStateOfCluster() {
        return cluster;
    }

    @Override
    public String toString() {
        return "LinkAcknowledged{}";
    }
}