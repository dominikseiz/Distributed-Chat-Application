package mvn.ds3.chat.app.server.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Component {

    private String id;
    private String ip;
    private int tcpPort;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Component(@JsonProperty("id") String id, @JsonProperty("ip") String ip, @JsonProperty("tcpPort") int tcpPort) {
        this.id = id;
        this.ip = ip;
        this.tcpPort = tcpPort;
    }

    public String getId() {
        return id;
    }

    public String getIP() {
        return ip;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    @Override
    public String toString() {
        return String.format("id=%s | host=%s:%s", id, ip, tcpPort);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Component component = (Component) object;
        return id.equals(component.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
