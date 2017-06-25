package unikl.disco;

import java.util.*;

/**
 * @author Malte Schütze
 */
public class Block {
    private String label;
    private long period;
    private List<Message> messages = new ArrayList<>();
    private Set<Block> nextBlocks = new HashSet<>();

    public Block(String label, long period) {
        this.label = label;
        this.period = period;
    }

    public String getLabel() { return label; }

    public void addMessage(Message message) {
        this.messages.add(message);
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

    public void addNext(Block block) {
        this.nextBlocks.add(block);
    }

    public Set<Block> getNextBlocks() {
        return Collections.unmodifiableSet(nextBlocks);
    }

    public static class Message {
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
}
