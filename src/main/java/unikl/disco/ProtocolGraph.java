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

    public PseudoPeriodicFunction approximateSubadditive(long k) {
        PseudoPeriodicFunction result = new PseudoPeriodicFunction(0, k, maxTraffic(k));
        for (long i = 0; i < k; i++) {
            if (i % (k/100) == 0) {
                System.out.printf("%.3d%", 100*i/k);
            }

            result.setValueAt(i, maxTraffic(i));
        }

        return result;
    }
}
