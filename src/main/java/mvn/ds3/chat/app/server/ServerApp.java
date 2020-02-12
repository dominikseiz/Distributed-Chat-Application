package mvn.ds3.chat.app.server;


import mvn.ds3.chat.app.server.cluster.*;
import mvn.ds3.chat.app.shared.Properties;
import mvn.ds3.chat.app.shared.ConstantValues;
import mvn.ds3.chat.app.shared.network.PublisherMulticast;
import mvn.ds3.chat.app.shared.network.ReceiverMulticast;
import mvn.ds3.chat.app.shared.network.ServerTCP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerApp {
    private static final Logger log = LoggerFactory.getLogger(ServerApp.class);


    public static void main(String[] args) throws Exception {
        InetAddress localhost = Properties.getIpAddress();
        int tcpPort = Properties.getTcp();
        Component component = new Component(Properties.getComponentId(), localhost.getHostAddress(), tcpPort);
        log.info("Starting chat app with IP address {}", localhost);

        var multicastIP = Properties.getMcIP();
        //InetAddress.getByName("169.254.25.135")
        var pub = new PublisherMulticast(multicastIP, ConstantValues.PORT_MC);

        var server = new ServerMaster();
        var compSup = new ComponentSupport(component, server, pub);

        var leader = new LeaderElection(component, server, compSup::getComponents, pub);
        var service = new AppSupport(component.getIP(), server::isMaster, pub, compSup.getUcSender());

        compSup.setStateOfCluster(service::getStateCluster);
        compSup.setStateOfClusterReceivingHandler(service::setStateOfCluster);

        var handlersMc = toMulticastHandlers(compSup, service, leader);
        var handlersUc = toUnicastHandlers(compSup, service, leader);

        new Thread(new ServerTCP(component.getTcpPort(), new UcHandler(handlersUc))).start();
        new Thread(new ReceiverMulticast(ConstantValues.PORT_MC, multicastIP, localhost,
                new McHandler(component, handlersMc))).start();

        Thread.sleep(2000);
        compSup.sendLinkMsg();
    }


    private static Map<Class<?>, ReceiverMulticast.MsgHandler> toMulticastHandlers(ReceiverMulticast.MsgHandler... handlersMsg) {
                 .map(handler -> handler.getCertainTypeOfMessage()
                        .stream()
                        .collect(Collectors.toMap(h -> h, h -> handler)))
                .collect(DisallowDuplicateKeyHashMap::new, Map::putAll, Map::putAll);
    }

    private static Map<Class<?>, ServerTCP.MsgHandler> toUnicastHandlers(ServerTCP.MsgHandler... handlers) {
        return Stream.of(handlers)
                .map(handler -> handler.getResponsibleUnicastMessageTypes()
                        .stream()
                        .collect(Collectors.toMap(h -> h, h -> handler)))
                .collect(DisallowDuplicateKeyHashMap::new, Map::putAll, Map::putAll);
    }

    private static class DisallowDuplicateKeyHashMap<K, V> extends HashMap<K, V> {

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            List<? extends K> duplicates = m.keySet().stream()
                    .filter(this::containsKey)
                    .collect(Collectors.toList());
            if (duplicates.isEmpty()) {
                m.forEach(this::put);
                return;
            }
            throw new IllegalArgumentException(String.format("Duplicate keys are not allowed. Duplicates are %s",
                    duplicates.toString()));
        }
    }

}
