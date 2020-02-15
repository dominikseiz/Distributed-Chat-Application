package mvn.ds3.chat.app.shared.msg;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetReply extends Message {

    private int serverTCP;

    public GetReply(@JsonProperty("serverPort") int serverTCP) {
        super(GetReply.class.getSimpleName());
        this.serverTCP = serverTCP;
    }

    public int getServerPort() {
        return serverTCP;
    }
}
