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
}
