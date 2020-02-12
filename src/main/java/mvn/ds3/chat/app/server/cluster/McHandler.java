package mvn.ds3.chat.app.server.cluster;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.network.Connection;
import mvn.ds3.chat.app.shared.network.ReceiverMulticast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class McHandler implements ReceiverMulticast.MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(McHandler.class.getName());

    private Map<Class<?>, ReceiverMulticast.MessageHandler> mcHandlers;
    private Component component;

    public McHandler(Component component, Map<Class<?>, ReceiverMulticast.MessageHandler> mcHandlers) {
        this.mcHandlers = mcHandlers;
        this.component = component;
    }

    @Override
    public void handle(Connection connection, Message message) {
        log.info("Multicast received with type {}", message.getMessageType());
     /* if (member.getAddress().equals(details.getFrom().getHostAddress())) {
            log.info("Multicast message is from self and will ignored.");
            return;
        }*/
        ReceiverMulticast.MessageHandler mHandler = mcHandlers.get(message.getClass());
        if (null == mHandler) {
            log.info("There isn't any multicast handler registered for the following type of message {}.", message.getMessageType());
            return;
        }
        try {
        	mHandler.handle(connection, message);
        } catch (Exception e) {
            log.error("A failure occured to handle this message with type {}", message.getMessageType(), e);
        }
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMessage() {
        throw new UnsupportedOperationException("This procedure is not supported by the root multicast message handler.");
    }

}