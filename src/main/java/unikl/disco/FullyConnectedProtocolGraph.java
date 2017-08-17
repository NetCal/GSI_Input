package unikl.disco;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Malte Schütze
 */
public class FullyConnectedProtocolGraph extends ProtocolGraph {

    public FullyConnectedRescaledProtocolGraph rescale() {
        long blockLength = shortestBlockLength();
        Set<Block> rescaledBlocks = this.getBlocks().stream()
                .map(block -> rescaleBlock(block, blockLength))
                .collect(Collectors.toSet());

        for (Block a : rescaledBlocks) {
            for (Block b : rescaledBlocks) {
                a.addNext(b);
            }
        }

        FullyConnectedRescaledProtocolGraph graph = new FullyConnectedRescaledProtocolGraph();
        rescaledBlocks.forEach(graph::addBlock);
        return graph;
    }

    public Block rescaleBlock(Block block, long length) {
        Block result = new Block(block.getLabel(), length);
        double scalingFactor = length / (double) block.getPeriod();
        for (Message msg : block) {
            long offset = Math.round(scalingFactor * msg.getOffset());
            Message clone = new Message(msg.getLabel(), result, offset, msg.getSize());
            result.addMessage(clone);
        }

        return result;
    }

    public PseudoPeriodicFunction approximateMostEfficientLoop() {
        double highestAverageTraffic = highestAverageBlockTraffic();
        PseudoPeriodicFunction function = new PseudoPeriodicFunction(2 * longestBlockLength(), 1, highestAverageTraffic);

        long time = 0;
        double value = 0;
        while (time < 2 * longestBlockLength()) {
            function.setValueAt(time, value);
            time = firstTimeExceeding(value);
            value = maxTraffic(time);
        }

        function.setValueAt(2 * longestBlockLength(), splitTrafficBetweenPrefixAndSuffix());
        return function;
    }

    public double splitTrafficBetweenPrefixAndSuffix() {
        long timeInSuffix = 0;
        double maxTraffic = 0;
        while (timeInSuffix < longestBlockLength()) {
            double trafficInSuffix = maxSuffix(timeInSuffix);
            maxTraffic = Math.max(maxTraffic, trafficInSuffix + splitTrafficBetweenLoopAndPrefix(2 * longestBlockLength() - timeInSuffix));
            timeInSuffix = firstTimeExceedingInSuffix(trafficInSuffix);
        }

        return maxTraffic;
    }

    private double splitTrafficBetweenLoopAndPrefix(long time) {
        double highestAverageTraffic = highestAverageBlockTraffic();

        long timeInPrefix = 0;
        double maxTraffic = 0;
        while (timeInPrefix < time) {
            double trafficInPrefix = maxPrefix(timeInPrefix);
            maxTraffic = Math.max(maxTraffic, (time - timeInPrefix) * highestAverageTraffic + trafficInPrefix);
            timeInPrefix = firstTimeExceedingInPrefix(trafficInPrefix);
        }

        return maxTraffic;
    }
}
