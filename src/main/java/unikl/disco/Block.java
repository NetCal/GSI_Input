package unikl.disco;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Malte Schütze
 */
public class Block implements Iterable<Message> {
    private String label;
    private long period;
    private int totalTraffic;
    private List<Message> messages = new ArrayList<>();
    private Set<Block> previousBlocks = new HashSet<>();
    private Set<Block> nextBlocks = new HashSet<>();

    // For the step function of the flow in this block, note the times where the function "steps", and to what traffc it steps
    private StepFunction maxPrefix = new StepFunction();

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

        // Message at offset n is only counted in interval of length n+1
        maxPrefix.setValueAt(message.getOffset() + 1, maxPrefix.getValue(maxPrefix.getValidUpTo()) + message.getSize());
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

    public long getNextMaxPrefixIncrementTime() {
        int currentMaxPrefix = maxPrefix.maximumValue();
        int remaining = currentMaxPrefix - totalTraffic;

        long earliestIncrement = Long.MAX_VALUE;
        for (Block block: nextBlocks) {
            long increment = block.getEarliestTimeMaxPrefixExceeds(remaining);
            if (increment < earliestIncrement) {
                earliestIncrement = increment;
            }
        }

        return earliestIncrement + period;
    }

    public long getEarliestTimeMaxPrefixExceeds(int value) {
        while (value >= maxPrefix.maximumValue()) {
            precalculateMaxPrefix(getNextMaxPrefixIncrementTime());
        }
        return maxPrefix.firstTimeExceeding(value);
    }

    public long getShortestIntervalWhereMaxTrafficExceeds(int value) {
        long shortestInterval = Long.MAX_VALUE;
        for (Message message: messages) {
            int trafficBefore = message.getOffset() == 0 ? 0 : maxPrefix(message.getOffset() - 1);
            int trafficToReach = value + trafficBefore;
            long intervalToReach = getEarliestTimeMaxPrefixExceeds(trafficToReach);
            long actualInterval = intervalToReach - message.getOffset();
            if (actualInterval < shortestInterval) {
                shortestInterval = actualInterval;
            }
        }

        return shortestInterval;
    }

    public void precalculateMaxPrefix(long time) {
        while (maxPrefix.getValidUpTo() < time) {
            long nextIncrement = getNextMaxPrefixIncrementTime();
            if (nextIncrement <= period) throw new IllegalStateException("Next increment shouldn't lie after validTo and before end of block");

            int traffic = 0;
            // Traverse graph forwards
            long remaining = nextIncrement - period;
            for (Block block : nextBlocks) {
                int nextBlockMaxTraffic = block.maxPrefix(remaining);
                if (nextBlockMaxTraffic > traffic) {
                    traffic = nextBlockMaxTraffic;
                }
            }

            maxPrefix.setValueAt(nextIncrement, traffic + totalTraffic);
        }
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

        precalculateMaxPrefix(time);

        return maxPrefix.getValue(time);
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
        // Calculate traffic in [a, b)
        // as traffic [0, b) - traffic [0, a)
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
        this.precalculateMaxPrefix(period + time);
        return maxPrefix.maximumInterval(time, period);
    }

    public int totalTrafficInBlock() {
        return totalTraffic;
    }

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    @Override
    public void forEach(Consumer<? super Message> action) {
        messages.forEach(action);
    }

    @Override
    public Spliterator<Message> spliterator() {
        return messages.spliterator();
    }
}
