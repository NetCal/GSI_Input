package de.uni_kl.cs.discodnc.gsi_input;

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
    private StepFunction maxSuffix = new StepFunction();

    public Block(String label, long period) {
        this.label = label;
        this.period = period;
    }

    public String getLabel() {
        return label;
    }

    public void addMessage(Message message) {
        if (message.getOffset() < 0 || message.getOffset() >= period) {
            throw new IllegalArgumentException("Message offset invalid (below zero or exceeding period): " + label + "/" + message.getLabel());
        }
        this.messages.add(message);
        this.totalTraffic += message.getSize();

        // Message at offset n is only counted in interval of length n+1
        maxPrefix.setValueAt(message.getOffset() + 1, maxPrefix.getValue(maxPrefix.getValidUpTo()) + message.getSize());
        rebuildMaxSuffix();
    }

    private void rebuildMaxSuffix() {
        StepFunction f = new StepFunction();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            f.setValueAt(period - msg.getOffset(), f.getValue(f.getValidUpTo()) + msg.getSize());
        }

        maxSuffix = f;
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
        double currentMaxPrefix = maxPrefix.maximumValue();
        double remaining = currentMaxPrefix - totalTraffic;

        long earliestIncrement = Long.MAX_VALUE;
        for (Block block : nextBlocks) {
            if (block == this && block.totalTraffic == 0) {
                // Endless recursion if we do not catch this
                // Note that this can also happen with a sequence of empty blocks (what the hell are you doing, tho?)
                if (nextBlocks.size() == 1) {
                    throw new IllegalStateException(label + ": Can't calculate next max prefix increment time! My only connection is to myself and I do not have any traffic");
                } else {
                    continue; // Just skip myself, every other block will provide tighter bound
                }
            }
            long increment = block.getEarliestTimeMaxPrefixExceeds(remaining);
            if (increment < earliestIncrement) {
                earliestIncrement = increment;
            }
        }

        return earliestIncrement + period;
    }

    public long getNextMaxSuffixIncrementTime() {
        double currentMaxSuffix = maxSuffix.maximumValue();
        double remaining = currentMaxSuffix - totalTraffic;

        long earliestIncrement = Long.MAX_VALUE;
        for (Block block : previousBlocks) {
            if (block == this && block.totalTraffic == 0) {
                // Endless recursion if we do not catch this
                // Note that this can also happen with a sequence of empty blocks (what the hell are you doing, tho?)
                if (nextBlocks.size() == 1) {
                    throw new IllegalStateException(label + ": Can't calculate next max prefix increment time! My only connection is to myself and I do not have any traffic");
                } else {
                    continue; // Just skip myself, every other block will provide tighter bound
                }
            }

            long increment = block.getEarliestTimeMaxSuffixExceeds(remaining);
            earliestIncrement = Math.min(increment, earliestIncrement);
        }

        return earliestIncrement + period;
    }

    public long getEarliestTimeMaxPrefixExceeds(double value) {
        while (value >= maxPrefix.maximumValue()) {
            precalculateMaxPrefix(getNextMaxPrefixIncrementTime());
        }
        return maxPrefix.firstTimeExceeding(value);
    }

    public long getEarliestTimeMaxSuffixExceeds(double value) {
        while (value >= maxSuffix.maximumValue()) {
            precalculateMaxSuffix(getNextMaxSuffixIncrementTime());
        }
        return maxSuffix.firstTimeExceeding(value);
    }

    public long getShortestIntervalWhereMaxTrafficExceeds(double value) {
        long shortestInterval = Long.MAX_VALUE;
        for (Message message : messages) {
            double trafficBefore = maxPrefix(message.getOffset()); // Offset is 0-indexed -> offset i = i slots before
            double trafficToReach = value + trafficBefore;
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
            if (nextIncrement <= period)
                throw new IllegalStateException("Next increment shouldn't lie after validTo and before end of block");

            double traffic = 0;
            // Traverse graph forwards
            long remaining = nextIncrement - period;
            for (Block block : nextBlocks) {
                double nextBlockMaxTraffic = block.maxPrefix(remaining);
                if (nextBlockMaxTraffic > traffic) {
                    traffic = nextBlockMaxTraffic;
                }
            }

            maxPrefix.setValueAt(nextIncrement, traffic + totalTraffic);
        }
    }

    public void precalculateMaxSuffix(long time) {
        while (maxSuffix.getValidUpTo() < time) {
            long nextIncrement = getNextMaxSuffixIncrementTime();
            if (nextIncrement <= period)
                throw new IllegalStateException("Next increment in illegal range");

            double traffic = 0;
            // Traverse graph backwards
            long remaining = nextIncrement - period;
            for (Block block : previousBlocks) {
                double nextBlockMaxTraffic = block.maxSuffix(remaining);
                traffic = Math.max(traffic, nextBlockMaxTraffic);
            }

            maxSuffix.setValueAt(nextIncrement, traffic + totalTraffic);
        }
    }

    /**
     * Calculates the max traffic generated in an interval of length <code>time</code> that ends on the end of this block.
     * If the interval is longer than block itself, the interval is extended to blocks feeding into this block
     *
     * @param time the length of the interval
     * @return an upper bound on the traffic generated in a suffix of this block
     */
    public double maxSuffix(long time) {
        if (time == 0) {
            return 0;
        }
        if (time == period) {
            return totalTraffic;
        }

        precalculateMaxSuffix(time);
        return maxSuffix.getValue(time);
    }

    /**
     * Calculates the max traffic generated in an interval of length <code>time</code> that begins with the start of this block.
     * If the interval is longer than block itself, the interval is extended to blocks following this block
     *
     * @param time the length of the interval
     * @return an upper bound on the traffic generated in a prefix of this block
     */
    public double maxPrefix(long time) {
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
     *
     * @param fromMessage the index of the message from which the interval should start
     * @param time        the length of the interval
     * @return an upper bound on the traffic generated in an interval of length <code>time</code> beginning at <code>fromMessage</code>
     */
    public double maxTraffic(int fromMessage, long time) {
        // Calculate traffic in [a, b)
        // as traffic [0, b) - traffic [0, a)
        long offset = messages.get(fromMessage).getOffset();
        long extInterval = offset + time;

        double trafficInExtInterval = maxPrefix(extInterval);
        double trafficInPrefix = maxPrefix(offset);

        return trafficInExtInterval - trafficInPrefix;
    }

    /**
     * Calculate the max traffic generated in any interval of length <code>time</code> that begins inside this block.
     *
     * @param time The length of the interval
     * @return an upper bound on the traffic generated in an interval of length <code>time</code> beginning somewhere
     * in this block
     */
    public double maxTraffic(long time) {
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
