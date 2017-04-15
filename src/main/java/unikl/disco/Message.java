package unikl.disco;

import java.util.*;

/**
 * @author Malte Schütze
 */
public class Message {
    private Set<Message> next = new HashSet<>();
    private Map<Integer, Integer> messageSequenceSizeCache = new HashMap<>();

    final int transmissionTime;
    private final int size;
    private final ProtocolSpec spec;

    public Message(int size, int transmissionTime, ProtocolSpec spec) {
        this.size = size;
        this.transmissionTime = transmissionTime;
        this.spec = spec;
    }

    public void addNext(Message item) {
        next.add(item);
        messageSequenceSizeCache = new HashMap<>();
    }

    public void removeNext(Message item) {
        next.remove(item);
        messageSequenceSizeCache = new HashMap<>();
    }

    public int getNumOptions() {
        return next.size();
    }

    public int maxTraffic(int toTime) {
        if (toTime >= transmissionTime) {
            return 0;
        }

        if (next.isEmpty()) {
            return size + spec.maxTraffic(toTime - spec.cycleLength);
        } else {
            return size + next.stream().mapToInt(msg -> msg.maxTraffic(toTime)).max().getAsInt();
        }
    }
}
