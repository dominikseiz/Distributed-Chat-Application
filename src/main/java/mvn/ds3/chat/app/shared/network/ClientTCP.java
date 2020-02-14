package mvn.ds3.chat.app.shared.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.ConvertMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientTCP {
    private static Logger log = LoggerFactory.getLogger(ClientTCP.class);
    private final InetAddress ip;
    private final int port;

    private static final int MAXIMUM_RETRY = 3;
    private static final int WAIT_MILLIS_RETRY = 150;

    public ClientTCP(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String sendMessage(Message message) throws IOException {
        boolean retry = true;
        int countingRetry = 1;
        while (retry) {
            try (var socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip.getHostAddress(), port), 100);
                socket.setSoTimeout(100);
                try (var outStreamWriter = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                     var bufReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                	log.info("Sending message {} to {}:{}", message, ip.getHostAddress(), port);
                    retry = false;
                    outStreamWriter.write(ConvertMsg.serializeMsg(message) + "\n");
                    outStreamWriter.flush();

                    return bufReader.readLine();
                }
            } catch (IOException ioe) {
                if (countingRetry >= MAXIMUM_RETRY) {
                	log.info("Failure connecting after {} retries.", countingRetry);
                    throw ioe;
                }
                log.info("Failure to connect. Number of retry attempts: {} Retrying to connect...", countingRetry);
                countingRetry = countingRetry + 1;
                try {
                    Thread.sleep(WAIT_MILLIS_RETRY);
                } catch (InterruptedException ie) {
                	log.info("Sleeping thread failed", ie);
                }
            }
        }
        throw new ConnectException("The attempted connection could not be established after several retries.");
    }
}