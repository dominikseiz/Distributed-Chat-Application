package mvn.ds3.chat.app.shared.msg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mvn.ds3.chat.app.server.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConvertMsg {
    private static ObjectMapper objectMapper = new ObjectMapper();

    private final static Map<String, Class<?>> messagesTypes;

    static {
        var objectMap = Map.of(LinkMsg.class.getSimpleName(), LinkMsg.class,
                LinkAcknowledged.class.getSimpleName(), LinkAcknowledged.class,
                ComponentMsg.class.getSimpleName(), ComponentMsg.class,
                HeartBeatMsg.class.getSimpleName(), HeartBeatMsg.class,
                MsgChat.class.getSimpleName(), MsgChat.class,
                GetMsg.class.getSimpleName(), GetMsg.class,
                GetReply.class.getSimpleName(), GetReply.class,
                GetMissingMsg.class.getSimpleName(), GetMissingMsg.class,
                MsgLeaderElection.class.getSimpleName(), MsgLeaderElection.class,
                MsgLeaderElection.class.getSimpleName(), MsgLeaderElection.class
        );
        var sequence = Map.of(MsgSequence.class.getSimpleName(), MsgSequence.class);
        var connected = new HashMap<String, Class<?>>();
        connected.putAll(objectMap);
        connected.putAll(sequence);
        messagesTypes = Collections.unmodifiableMap(connected);
    }

    public static Message deserializeMsg(String subject) {
        try {
            JsonNode jNode = objectMapper.readTree(subject);
            JsonNode jType = jNode.path("type");
            if (jType == null || jType.textValue() == null || jType.textValue().trim().isEmpty()) {
                throw new IllegalArgumentException("The type attribute is needed.");
            }
            if (!messagesTypes.containsKey(jType.textValue())) {
                throw new IllegalArgumentException("Type " + jType.textValue() + " is not recognised. Recorded types are: " + messagesTypes.keySet());
            }
            return (Message) objectMapper.treeToValue(jNode, messagesTypes.get(jType.textValue()));
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException("Failure parsing value: " + subject, jpe);
        }
    }

    public static String serializeMsg(Message message) {
        try {
            return objectMapper.writer().writeValueAsString(message);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException("Failure serializing message: " + message.getMessageType(), jpe);
        }
    }
}
