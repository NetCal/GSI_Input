package unikl.disco;

import java.util.*;

/**
 * @author Malte Schütze
 */
public class Message {
    private Set<Message> next = new HashSet<>();
    private Map<Integer, Integer> intervalTrafficCache = new HashMap<>();

    final int transmissionTime;
    private final int size;
    private final ProtocolSpec spec;

    public Message(int size, int transmissionTime, ProtocolSpec spec) {
        this.size = size;
        this.transmissionTime = transmissionTime;
        this.spec = spec;
    }

    public void addNext(Message item) {
        if (!intervalTrafficCache.isEmpty()) {
            throw new IllegalStateException("Adding a message after the traffic cache has been populated is not supported");
        }
        next.add(item);
    }

    public void removeNext(Message item) {
        if (!intervalTrafficCache.isEmpty()) {
            throw new IllegalStateException("Adding a message after the traffic cache has been populated is not supported");
        }
        next.remove(item);
    }

    public int getNumOptions() {
        return next.size();
    }

    public int maxTraffic(int toTime) {
        if (toTime <= transmissionTime) {
            return 0;
        }

        if (intervalTrafficCache.containsKey(toTime)) {
            return intervalTrafficCache.get(toTime);
        }

        int traffic;
        if (next.isEmpty()) {
            traffic = size + spec.maxTraffic(toTime - spec.cycleLength);
        } else {
            traffic = size + next.stream().mapToInt(msg -> msg.maxTraffic(toTime)).max().getAsInt();
        }

        intervalTrafficCache.put(toTime, traffic);
        return traffic;
    }
}
