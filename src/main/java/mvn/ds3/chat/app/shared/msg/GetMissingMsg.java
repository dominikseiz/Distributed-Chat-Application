package mvn.ds3.chat.app.shared.msg;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GetMissingMsg extends Message {
    private final List<Long> missingMsgSequence;

    public GetMissingMsg(@JsonProperty("missingMessageSequence") List<Long> missingMsgSequence) {
        super(GetMissingMsg.class.getSimpleName());
        this.missingMsgSequence = missingMsgSequence;
    }

    public List<Long> getMissingMsgSequence() {
        return missingMsgSequence;
    }
}
