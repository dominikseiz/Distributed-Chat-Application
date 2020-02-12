package mvn.ds3.chat.app.shared.clock;

import org.junit.jupiter.api.Test;

import mvn.ds3.chat.app.shared.clock.VectorClock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorClockTest {

    @Test
    public void testVectorClockCurrState() {
        var vClock1 = new VectorClock();
        vClock1.setReplicaTimestamp("replica1", 2);

        var vClock2 = new VectorClock();
        vClock2.setReplicaTimestamp("replica2", 2);

        int comparisonCurr = VectorClock.compare(vClock1, vClock2);
        assertEquals(0, comparisonCurr);
    }

    @Test
    public void testVectorClockPreState() {
        var vClock1 = new VectorClock();
        vClock1.setReplicaTimestamp("replica1", 2);

        var vClock2 = new VectorClock();
        vClock2.setReplicaTimestamp("replica1", 3);

        int comparisonPre = VectorClock.compare(vClock1, vClock2);
        assertEquals(-1, comparisonPre);
    }


    @Test
    public void testVectorClockPostState() {
        var vClock1 = new VectorClock();
        vClock1.setReplicaTimestamp("replica1", 3);

        var vClock2 = new VectorClock();
        vClock2.setReplicaTimestamp("replica1", 2);

        int comparisonPost = VectorClock.compare(vClock1, vClock2);
        assertEquals(1, comparisonPost);
    }

}