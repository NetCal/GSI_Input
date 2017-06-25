package unikl.disco;


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
}
