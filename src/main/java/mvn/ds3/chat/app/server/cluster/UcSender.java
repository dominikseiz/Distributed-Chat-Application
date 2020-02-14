package mvn.ds3.chat.app.server.cluster;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.network.ClientTCP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UcSender {

    private static final Logger log = LoggerFactory.getLogger(UcSender.class);

    private Supplier<Set<Component>> components;
    private Consumer<Component> failureOfTransferHandler;
    private ExecutorService execute;

    public UcSender(Supplier<Set<Component>> components, Consumer<Component> failureOfTransferHandler) {
        this.components = components;
        this.failureOfTransferHandler = failureOfTransferHandler;
        this.execute = Executors.newFixedThreadPool(10);
    }

    public void sendMsgToAllComponents(Message message) {
    	components.get().forEach(component -> execute.submit(() -> sendMsg(component, message)));
    }

    private void sendMsg(Component component, Message message) {
        try {
            var clientTcp = new ClientTCP(InetAddress.getByName(component.getIP()), component.getTcpPort());
            clientTcp.sendMessage(message);
        } catch (Exception e) {
            log.info("The component with ID {} and IP {}:{} was not reachable. This happened for this particular reason {}.", component.getId(), component.getIP(), component.getTcpPort(), e.getMessage());
            failureOfTransferHandler.accept(component);
        }
    }


}
