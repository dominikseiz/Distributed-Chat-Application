package mvn.ds3.chat.app.client;

import mvn.ds3.chat.app.client.HoldbackQ;
import mvn.ds3.chat.app.shared.msg.MsgChat;
import mvn.ds3.chat.app.shared.network.PublisherMulticast;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class HoldbackQtest {
    @Test
    public void testPrioQ() throws InterruptedException {
        HoldbackQ queue = new HoldbackQ(Mockito.mock(PublisherMulticast.class),
                Mockito.mock(Consumer.class));

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(queue::deliverMessages, 1, 1, TimeUnit.SECONDS);

        queue.handle(null, new MsgChat(1L, "test queue", "test queue", null));
        queue.handle(null, new MsgChat(4L, "test queue", "test queue", null));

        Thread.sleep(2000);
        queue.handle(null, new MsgChat(2L, "test queue", "test queue", null));
        Thread.sleep(8000);
        queue.handle(null, new MsgChat(3L, "test queue", "test queue", null));

        Thread.sleep(10000);
    }

    @Test
    public void testingGetRangeOfExpectedValidValues() {
        List<Long> rangeValues1 = HoldbackQ.getRange(0, 3);
        assertThat(rangeValues1).containsOnly(1L, 2L);

        List<Long> rangeValues2 = HoldbackQ.getRange(0, 3);
        assertThat(rangeValues2).containsOnly(1L, 2L);

        List<Long> rangeValues3 = HoldbackQ.getRange(123, 128);
        assertThat(rangeValues3).containsOnly(124L, 125L, 126L, 127L);
    }

}