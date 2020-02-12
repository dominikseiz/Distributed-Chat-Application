package mvn.ds3.chat.app.server.cluster;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.network.Connection;
import mvn.ds3.chat.app.shared.network.ServerTCP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class UcHandler implements ServerTCP.MsgHandler {
    private static final Logger log = LoggerFactory.getLogger(UcHandler.class);

    private final Map<Class<?>, ServerTCP.MsgHandler> handlers;

    public UcHandler(Map<Class<?>, ServerTCP.MsgHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public String handleUc(Connection connection, Message message) {
    	ServerTCP.MsgHandler handler = handlers.get(message.getClass());
        if (null == handler) {
            throw new IllegalArgumentException(String.format("There isn't any TCP message handler registered for message type %s.", message.getMessageType()));
        }
        return handler.handleUc(connection, message);
    }

    @Override
    public List<Class<? extends Message>> getCertainTypesOfMsgUnicast() {
        throw new UnsupportedOperationException("This procedure is not supported by the root multicast message handler.");
    }

}