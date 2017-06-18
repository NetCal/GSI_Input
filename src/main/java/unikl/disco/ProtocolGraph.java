package unikl.disco;


import java.util.HashSet;
import java.util.Set;

/**
 * @author Malte Schütze
 */
public class ProtocolGraph {

    private Set<Block> blocks = new HashSet<>();

    public void addBlock(Block block) {
        blocks.add(block);
    }

}
