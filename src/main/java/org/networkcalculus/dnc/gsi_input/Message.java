package org.networkcalculus.dnc.gsi_input;

/**
 * @author Malte Schütze
 */
public class Message {
    private final String label;
    private final long offset;
    private final int size;
    private final Block block;

    public Message(String label, Block block, long offset, int size) {
        this.label = label;
        this.offset = offset;
        this.size = size;
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public String getLabel() {
        return label;
    }

    public int getSize() {
        return size;
    }

    public long getOffset() {
        return offset;
    }
}
