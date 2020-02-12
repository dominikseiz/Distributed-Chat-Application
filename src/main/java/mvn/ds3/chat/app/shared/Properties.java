package mvn.ds3.chat.app.shared;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Properties {

    private static final String PROP_TCP = "TCP_SERVER_PORT";
    private static final String PROP_COMPONENT = "COMP_ID";
    private static final String PROP_IP = "IP";

    public static Integer getTcp() {
        return Integer.parseInt(getPropOfProperties(PROP_TCP));
    }

    public static String getComponentId() {
        return getPropOfProperties(PROP_COMPONENT);
    }

    public static InetAddress getIpAddress() {
        try {
            return InetAddress.getByName(getPropOfProperties(PROP_IP));
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }

    public static String getPropOfProperties(final String nameOfProperty) {
        String propValue = System.getProperty(nameOfProperty);
        if (propValue == null || propValue.isEmpty()) {
            throw new IllegalArgumentException(String.format("The required property %s is ...", nameOfProperty));
        }
        return propValue;
    }


    public static InetAddress getMcIP() {
        try {
            return InetAddress.getByName("225.6.7.8");
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }


}
