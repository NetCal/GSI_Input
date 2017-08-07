package unikl.disco;

import java.util.*;

/**
 * @author Malte Schütze
 */
public class Block {
    private String label;
    private long period;
    private int totalTraffic;
    private List<Message> messages = new ArrayList<>();
    private Set<Block> previousBlocks = new HashSet<>();
    private Set<Block> nextBlocks = new HashSet<>();

    // For the stepwise function of the flow in this block, note the times where the function "steps"
    private List<Long> trafficIncrementTimes = new ArrayList<>();
    // And to what traffic it steps
    private List<Integer> trafficAtTime = new ArrayList<>();

    public Block(String label, long period) {
        this.label = label;
        this.period = period;
    }

    public String getLabel() {
        return label;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        this.totalTraffic += message.getSize();

        if (trafficIncrementTimes.isEmpty()) {
            // Initialize stepwise list
            trafficIncrementTimes.add(message.getOffset());
            trafficAtTime.add(message.getSize());
        } else if (trafficIncrementTimes.get(trafficIncrementTimes.size() - 1) == message.getOffset()) {
            // Increment last element in stepwise list
            int trafficSoFar = trafficAtTime.get(trafficAtTime.size() - 1);
            trafficAtTime.set(trafficAtTime.size() - 1, trafficSoFar + message.getSize());
        } else {
            // Add a new point to stepwise list
            int trafficSoFar = trafficAtTime.get(trafficAtTime.size() - 1);
            trafficIncrementTimes.add(message.getOffset());
            trafficAtTime.add(trafficSoFar + message.getSize());
        }
    }

    public long getPeriod() {
        return period;
    }

    public int getNumMessages() {
        return messages.size();
    }

    public Message getMessage(int idx) {
        return messages.get(idx);
    }

    private void addPrevious(Block block) {
        this.previousBlocks.add(block);
    }

    public void addNext(Block block) {
        this.nextBlocks.add(block);
        block.addPrevious(this);
    }

    public Set<Block> getNextBlocks() {
        return Collections.unmodifiableSet(nextBlocks);
    }

    public Set<Block> getPreviousBlocks() {
        return Collections.unmodifiableSet(previousBlocks);
    }

    /**
     * Calculates the max traffic generated in an interval of length <code>time</code> that ends on the end of this block.
     * If the interval is longer than block itself, the interval is extended to blocks feeding into this block
     * @param time the length of the interval
     * @return an upper bound on the traffic generated in a suffix of this block
     */
    public int maxSuffix(long time) {
        if (time == 0) {
            return 0;
        }
        if (time == period) {
            return totalTraffic;
        }

        if (time > period) {
            int traffic = 0;
            // Traverse graph backwards
            for (Block block : previousBlocks) {
                int previousBlockMaxTraffic = block.maxSuffix(time - period);
                if (previousBlockMaxTraffic > traffic) {
                    traffic = previousBlockMaxTraffic;
                }
            }

            return traffic + totalTraffic;
        }

        // traffic produced in suffix of length k is equal to total traffic minus traffic produced in prefix of length n - k
        return totalTraffic - maxPrefix(period - time);
    }

    /**
     * Calculates the max traffic generated in an interval of length <code>time</code> that begins with the start of this block.
     * If the interval is longer than block itself, the interval is extended to blocks following this block
     * @param time the length of the interval
     * @return an upper bound on the traffic generated in a prefix of this block
     */
    public int maxPrefix(long time) {
        if (time == 0) {
            return 0;
        }
        if (time == period) {
            return totalTraffic;
        }

        if (time > period) {
            int traffic = 0;
            // Traverse graph forwards
            for (Block block : nextBlocks) {
                int nextBlockMaxTraffic = block.maxPrefix(time - period);
                if (nextBlockMaxTraffic > traffic) {
                    traffic = nextBlockMaxTraffic;
                }
            }

            return traffic + totalTraffic;
        }

        int idx = Collections.binarySearch(trafficIncrementTimes, time);
        if (idx > 0) {
            // Traffic increases at this time step
            // Since our interval is right-open (-> interval length 0 always empty), we use the traffic value from before
            return trafficAtTime.get(idx - 1);
        } else if (idx < -1) {
            // Traffic increased before this time step
            // actualIdx = -(idx + 1) = -idx - 1, so the increase happened at -idx - 2
            return trafficAtTime.get(-idx - 2);
        }

        // Interval ends before first message
        return 0;
    }

    /**
     * Calculates the max traffic generated in an interval of length <code>time</code> that begins "just before" the
     * message with index <code>fromMessage</code> in this block. Note that if there are messages with an index
     * lower than <code>fromMessage</code> that are sent at the same offset at the start message, their size is included
     * in the interval calculations.
     * If the interval is longer than the remaining length of block, the interval is extended to all blocks following
     * this block.
     * @param fromMessage the index of the message from which the interval should start
     * @param time the length of the interval
     * @return an upper bound on the traffic generated in an interval of length <code>time</code> beginning at <code>fromMessage</code>
     */
    public int maxTraffic(int fromMessage, long time) {
        long offset = messages.get(fromMessage).getOffset();
        long extInterval = offset + time;
        int trafficInExtInterval = maxPrefix(extInterval);
        int trafficInPrefix = maxPrefix(offset);

        return trafficInExtInterval - trafficInPrefix;
    }

    /**
     * Calculate the max traffic generated in any interval of length <code>time</code> that begins inside this block.
     * @param time The length of the interval
     * @return an upper bound on the traffic generated in an interval of length <code>time</code> beginning somewhere
     * in this block
     */
    public int maxTraffic(long time) {
        int max = 0;
        for (int i = 0; i < trafficIncrementTimes.size(); i++) {
            long offset = trafficIncrementTimes.get(i);
            int trafficSoFar = 0;
            if (i > 0) trafficSoFar = trafficAtTime.get(i - 1);

            int trafficGenerated = maxPrefix(offset + time)- trafficSoFar;
            if (trafficGenerated > max) {
                max = trafficGenerated;
            }
        }

        return max;
    }

    public int totalTrafficInBlock() {
        return totalTraffic;
    }
}
