package unikl.disco;


import java.util.Collection;
import java.util.HashMap;

/**
 * @author Malte Schütze
 */
public class ProtocolGraph {

    private HashMap<String, Block> blocks = new HashMap<>();

    public void addBlock(Block block) {
        blocks.put(block.getLabel(), block);
    }

    public Block getBlock(String label) {
        Block block = blocks.get(label);
        if (block == null) throw new IllegalArgumentException("No block labeled " + label);
        return block;
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public Collection<Block> getBlocks() {
        return blocks.values();
    }

    /**
     * Calculate the maximum traffic generated over all possible intervals of length <code>intervalLength</code>
     * @param intervalLength The length of the interval to check
     * @return The maximum traffic generated
     */
    public int maxTraffic(long intervalLength) {
        return blocks.values().stream()
                .mapToInt(b -> b.maxTraffic(intervalLength))
                .max()
                .orElse(0); // Max returns none if there are no blocks, therefore no traffic
    }

    private long longestBlockLength() {
        return blocks.values().stream()
                .mapToLong(Block::getPeriod)
                .max()
                .orElse(0); // If no blocks, 0 length of longest block
    }

    private long firstTimeExceeding(int value) {
        return blocks.values().stream()
                .mapToLong(b -> b.getShortestIntervalWhereMaxTrafficExceeds(value))
                .min()
                .getAsLong();
    }

    public PseudoPeriodicFunction approximateSubadditive(long k) {
        PseudoPeriodicFunction result = new PseudoPeriodicFunction(0, k, maxTraffic(k));
        if (maxTraffic(0) != 0) {
            throw new IllegalStateException("Interval 0 should always return maxtraffic 0");
        }

        result.setValueAt(0, 0);
        long nextStep = firstTimeExceeding(0);
        while (nextStep <= k) {
            int nextValue = maxTraffic(nextStep);
            result.setValueAt(nextStep, nextValue);
            nextStep = firstTimeExceeding(nextValue);
        }

        result.setValueAt(k, maxTraffic(k));

        return result;
    }
}
