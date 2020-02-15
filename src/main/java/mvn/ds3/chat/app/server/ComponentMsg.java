package mvn.ds3.chat.app.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import mvn.ds3.chat.app.server.cluster.Component;
import mvn.ds3.chat.app.shared.clock.VectorClock;
import mvn.ds3.chat.app.shared.msg.Message;

import java.util.Set;

public class ComponentMsg extends Message {
    private final VectorClock vClock;
    private final Set<Component> components;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ComponentMsg(@JsonProperty("components") Set<Component> component,
                         @JsonProperty("vectorClock") VectorClock vClock) {
        super(ComponentMsg.class.getSimpleName());
        this.components = component;
        this.vClock = vClock;
    }


    public Set<Component> getComponents() {
        return components;
    }

    public VectorClock getVectorClock() {
        return vClock;
    }

    @Override
    public String toString() {
        return "ComponentMsg{" +
                "component=" + components.toString() +
                "}";
    }
}
