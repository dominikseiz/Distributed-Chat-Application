package mvn.ds3.chat.app;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import mvn.ds3.chat.app.shared.msg.Message;
import mvn.ds3.chat.app.shared.msg.ConvertMsg;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ConvertMsgTest {

    @Test
    void testMsgValidation() throws JSONException {
        String subject = "{" +
                "\"type\": \"ComponentMessage\"," +
                "\"components\": [{" +
                "\"ipAddress\": \"127.0.0.1\"," +
                "\"port\": 10" +
                "}]" +
                "}";
        Message componentMsg = ConvertMsg.deserialize(subject);
        assertEquals("ComponentMessage", componentMsg.getMessageType());

        String serializedComponentMessage = ConvertMsg.serialize(componentMsg);
        JSONAssert.assertEquals(subject, serializedComponentMessage, JSONCompareMode.LENIENT);
    }
}
