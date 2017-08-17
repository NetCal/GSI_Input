package unikl.disco;


import java.util.*;
import java.util.stream.Collectors;

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
     *
     * @param intervalLength The length of the interval to check
     * @return The maximum traffic generated
     */
    public double maxTraffic(long intervalLength) {
        return blocks.values().stream()
                .mapToDouble(b -> b.maxTraffic(intervalLength))
                .max()
                .orElse(0); // Max returns none if there are no blocks, therefore no traffic
    }

    long longestBlockLength() {
        return blocks.values().stream()
                .mapToLong(Block::getPeriod)
                .max()
                .orElse(0); // If no blocks, 0 length of longest block
    }

    long shortestBlockLength() {
        return blocks.values().stream()
                .mapToLong(Block::getPeriod)
                .min()
                .orElse(0);
    }

    int highestBlockTraffic() {
        return blocks.values().stream()
                .mapToInt(Block::totalTrafficInBlock)
                .max()
                .orElse(0);
    }

    double highestAverageBlockTraffic() {
        return blocks.values().stream()
                .mapToDouble(b -> b.totalTrafficInBlock() / (double) b.getPeriod())
                .max()
                .orElse(0);
    }

    public long firstTimeExceeding(double value) {
        return blocks.values().stream()
                .mapToLong(b -> b.getShortestIntervalWhereMaxTrafficExceeds(value))
                .min()
                .getAsLong();
    }

    public long firstTimeExceedingInPrefix(double value) {
        return blocks.values().stream()
                .mapToLong(b -> b.getEarliestTimeMaxPrefixExceeds(value))
                .min()
                .getAsLong();
    }

    public double maxPrefix(long time) {
        return blocks.values().stream()
                .mapToDouble(b -> b.maxPrefix(time))
                .max()
                .orElse(0);
    }

    public double maxSuffix(long time) {
        return blocks.values().stream()
                .mapToDouble(b -> b.maxSuffix(time))
                .max()
                .orElse(0);
    }

    public PseudoPeriodicFunction approximateSubadditive(long k) {
        PseudoPeriodicFunction result = new PseudoPeriodicFunction(0, k, maxTraffic(k));
        if (maxTraffic(0) != 0) {
            throw new IllegalStateException("Interval 0 should always return maxtraffic 0");
        }

        result.setValueAt(0, 0);
        long nextStep = firstTimeExceeding(0);
        while (nextStep <= k) {
            double nextValue = maxTraffic(nextStep);
            result.setValueAt(nextStep, nextValue);
            nextStep = firstTimeExceeding(nextValue);
        }

        result.setValueAt(k, maxTraffic(k));

        return result;
    }

    public FullyConnectedProtocolGraph fullyConnected(int numSuccessiveBlocks) {
        Set<Block> superBlocks = getSuccessiveBlocks(numSuccessiveBlocks).stream()
                .map(this::blocksToSuperBlock)
                .collect(Collectors.toSet());

        for (Block a : superBlocks) {
            for (Block b : superBlocks) {
                a.addNext(b);
            }
        }

        FullyConnectedProtocolGraph result = new FullyConnectedProtocolGraph();
        superBlocks.forEach(result::addBlock);

        return result;
    }

    public Set<List<Block>> getSuccessiveBlocks(int n) {
        Set<List<Block>> result = new HashSet<>();
        for (Block block : blocks.values()) {
            result.addAll(getSuccessiveBlocks(block, n));
        }

        return result;
    }

    public Set<List<Block>> getSuccessiveBlocks(Block block, int n) {
        Set<List<Block>> result = new HashSet<>();
        if (n == 1 || block.getNextBlocks().isEmpty()) {
            List<Block> list = new ArrayList<>();
            list.add(block);
            result.add(list);
            return result;
        }

        for (Block next : block.getNextBlocks()) {
            for (List<Block> successors : getSuccessiveBlocks(next, n - 1)) {
                successors.add(0, block);
                result.add(successors);
            }
        }

        return result;
    }

    public Block blocksToSuperBlock(List<Block> blocks) {
        long duration = blocks.stream().mapToLong(Block::getPeriod).sum();
        String label = blocks.stream().map(Block::getLabel).collect(Collectors.joining("--"));
        Block superBlock = new Block(label, duration);

        long globalOffset = 0;
        for (Block block : blocks) {
            for (Message msg : block) {
                Message cloned = new Message(msg.getLabel(), superBlock, globalOffset + msg.getOffset(), msg.getSize());
                superBlock.addMessage(cloned);
            }

            globalOffset += block.getPeriod();
        }

        return superBlock;
    }
}
