package mvn.ds3.chat.app.client;


import mvn.ds3.chat.app.shared.Properties;
import mvn.ds3.chat.app.shared.ConstantValues;
import mvn.ds3.chat.app.shared.msg.GetMsg;
import mvn.ds3.chat.app.shared.network.PublisherMulticast;
import mvn.ds3.chat.app.shared.network.ReceiverMulticast;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientApp {

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        InetAddress multicastIP = Properties.getMulticastIP();
        var publisher = new PublisherMulticast(multicastIP, ConstantValues.PORT_MC);
        var controller = new ControllerApp(new PublisherMulticast(multicastIP, ConstantValues.PORT_MC));

        new Thread(new ReceiverMulticast(ConstantValues.PORT_MC, multicastIP, Properties.getIpAddress(), controller)).start();
        var connectUI = new ConnectUI(controller);
        connectUI.setVisible(true);

        Thread.sleep(2000);
        publisher.publish(new GetMsg());
    }

}
