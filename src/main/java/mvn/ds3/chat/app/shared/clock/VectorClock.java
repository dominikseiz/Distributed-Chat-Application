package mvn.ds3.chat.app.shared.clock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class VectorClock {
    private Map<String, Long> timestampReplicas = new ConcurrentHashMap<>();

    public VectorClock() {
    }

    public VectorClock(@JsonProperty("timestampReplicas") Map<String, Long> timestampReplicas) {
        this.timestampReplicas = new ConcurrentHashMap<>(timestampReplicas);
    }

    public Map<String, Long> getTimestampReplicas() {
        return timestampReplicas;
    }


    public Long getReplicaOfTimestamp(String replicaId) {
        return timestampReplicas.get(replicaId);
    }

    public void setReplicaOfTimestamp(String replicaId, long timestamp) {
        timestampReplicas.put(replicaId, timestamp);
    }

    public VectorClock copy() {
        return new VectorClock(Map.copyOf(timestampReplicas));
    }

    public void unite(VectorClock vClock) {
        for (Entry<String, Long> entry : vClock.timestampReplicas.entrySet()) {
            final String replicaId = entry.getKey();
            final long unitingTimestamp = entry.getValue();
            final long localTimestamp = timestampReplicas.getOrDefault(replicaId, Long.MIN_VALUE);
            timestampReplicas.put(replicaId, Math.max(localTimestamp, unitingTimestamp));
        }
    }

    public boolean afterwards(VectorClock vClock) {
        return compare(this, vClock) == 1;
    }


    public static int compare(VectorClock leftClock, VectorClock rightClock) {
        Map<String, Long> leftClocks = leftClock.timestampReplicas;
        Map<String, Long> rightClocks = rightClock.timestampReplicas;
        Set<String> unity = new HashSet<>(leftClocks.keySet());
        unity.addAll(rightClocks.keySet());

        int greaterLeft = 0;
        int greaterRight = 0;
        for (String key : unity) {
            long leftValue = leftClocks.getOrDefault(key, 0L);
            long rightValue = rightClocks.getOrDefault(key, 0L);

            if (leftValue > rightValue) {
                greaterLeft++;
            } else if (leftValue < rightValue) {
                greaterRight++;
            }
        }
        //before -1
        //after 1
        // concurrent 0
        return Long.compare(greaterLeft, greaterRight);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        VectorClock clck = (VectorClock) object;

        return timestampReplicas.equals(clck.timestampReplicas);
    }

    @Override
    public int hashCode() {
        return timestampReplicas.hashCode();
    }

    @Override
    public String toString() {
        return timestampReplicas.toString();
    }

}