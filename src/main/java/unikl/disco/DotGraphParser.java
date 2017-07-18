package unikl.disco;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.model.MutableNodePoint;
import guru.nidi.graphviz.parse.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Malte Schütze
 */
public class DotGraphParser {
    private final InputStream source;

    public DotGraphParser(InputStream source) {
        this.source = source;
    }

    private boolean isAttrPresent(MutableNode node, String targetAttr) {
        for (Map.Entry<String, Object> attr : node.attrs()) {
            if (attr.getKey().equals(targetAttr)) {
                return true;
            }
        }

        return false;
    }

    private String getAttrString(MutableNode node, String targetAttr) {
        for (Map.Entry<String, Object> attr : node.attrs()) {
            if (attr.getKey().equals(targetAttr)) {
                return (String) attr.getValue();
            }
        }

        throw new AttributeNotFoundException(node.label().toString(), targetAttr);
    }

    private long getAttrLong(MutableNode node, String targetAttr) {
        for (Map.Entry<String, Object> attr : node.attrs()) {
            if (attr.getKey().equals(targetAttr)) {
                return Long.parseLong(attr.getValue().toString());
            }
        }

        throw new AttributeNotFoundException(node.label().toString(), targetAttr);
    }

    private int inDegreeOf(MutableGraph graph, MutableNode node) {
        int acc = 0;

        for (MutableNode other : graph.nodes()) {
            if (other.equals(node)) {
                continue; // Ignore block-to-same-block links (interpreted as block-to-first-message links w.l.o.g)
            }

            for (Link link : other.links()) {
                if (((MutableNodePoint) link.to()).node().equals(node)) {
                    if (getAttrString(other, "type").equals("Block")) {
                        throw new IllegalStateException("Block to block link: " + other.label() + " -> " + node.label());
                    }

                    acc++;
                }
            }
        }

        return acc;
    }

    private int outDegreeOf(MutableNode node) {
        return node.links().size();
    }

    private MutableNode nextNode(MutableNode node) {
        if (outDegreeOf(node) != 1) {
            throw new IllegalStateException("No single next node found for " + node.label());
        }

        return ((MutableNodePoint) node.links().iterator().next().to()).node();
    }

    public ProtocolGraph parse() throws IOException {
        MutableGraph rawGraph = Parser.read(source);

        // Transform the graph into a nicer format
        ProtocolGraph graph = new ProtocolGraph();
        // input block -> output block
        Map<MutableNode, Block> blockMap = new HashMap<>();
        // input block -> output messages belonging to that block
        Map<MutableNode, List<Message>> messagesInBlock = new HashMap<>();
        // input message -> output message
        Map<MutableNode, Message> messageMap = new HashMap<>();


        for (MutableNode vertex : rawGraph.nodes()) {
            if (getAttrString(vertex, "type").equals("Block")) {
                // Verify that block node was parsed correctly
                if (inDegreeOf(rawGraph, vertex) != 1) {
                    throw new IllegalStateException("Invalid incoming edge count for block type vertex " + vertex.label() + " (" + inDegreeOf(rawGraph, vertex) + ")");
                }

                if (!blockMap.containsKey(vertex)) {
                    blockMap.put(vertex, new Block(vertex.label().toString(), getAttrLong(vertex, "tPeriod")));
                    messagesInBlock.put(vertex, new ArrayList<>());
                }
            } else if (getAttrString(vertex, "type").equals("TMsg")) {
                // Find the block following this message
                MutableNode current = vertex;
                int idxBack = -1;
                while (!getAttrString(current, "type").equals("Block")) {
                    if (outDegreeOf(current) != 1) {
                        throw new IllegalStateException("Invalid outgoing edge count for msg type vertex " + current.label() + " (" + outDegreeOf(current) + ")");
                    }

                    idxBack++;
                    current = nextNode(current);
                }

                // Current now points to a block
                if (!blockMap.containsKey(current)) {
                    blockMap.put(current, new Block(current.label().toString(), getAttrLong(current, "tPeriod")));
                    messagesInBlock.put(current, new ArrayList<>());
                }

                // For efficiency reasons, we store messages backwards (i.e. later messages come first)
                // so we can extend the list easily if we find an earlier message later
                // We fill node slots between the node and already added nodes with null and replace them
                // when we find them
                List<Message> list = messagesInBlock.get(current);
                while (idxBack > list.size() - 1) {
                    list.add(null);
                }

                int size = 1;
                if (isAttrPresent(vertex, "size")) {
                    size = (int) getAttrLong(vertex, "size");
                }

                Message msg = new Message(vertex.label().toString(), blockMap.get(current), getAttrLong(vertex, "tOffs"), size);
                list.set(idxBack, msg);
                messageMap.put(vertex, msg);
            } else {
                throw new IllegalStateException("Unsupported node type '" + getAttrString(vertex, "type"));
            }
        }

        // First add messages
        for (Map.Entry<MutableNode, Block> entry : blockMap.entrySet()) {
            MutableNode inputBlock = entry.getKey();
            Block block = entry.getValue();

            graph.addBlock(block);

            // We stored the messages backwards, turn them back around
            List<Message> messages = messagesInBlock.get(inputBlock);
            for (int i = messages.size() - 1; i >= 0; i--) {
                block.addMessage(messages.get(i));
            }
        }

        // Then link blocks between each other, because we rely on all messages
        // being added for sanity checks
        for (Map.Entry<MutableNode, Block> entry : blockMap.entrySet()) {
            MutableNode inputBlock = entry.getKey();
            Block block = entry.getValue();

            // Connect blocks
            for (Link link : inputBlock.links()) {
                MutableNode to = ((MutableNodePoint) link.to()).node();
                if (to.equals(inputBlock)) {
                    // Direct cycle to self
                    block.addNext(block);
                } else {
                    // Link to other block
                    Message targetMessage = messageMap.get(to);
                    Block targetBlock = targetMessage.getBlock();

                    // Verify we are pointing to the first message in a block
                    if (!targetBlock.getMessage(0).equals(messageMap.get(to))) {
                        throw new IllegalStateException("Block '" + inputBlock.label() + "' points into middle of block '" + targetBlock.getLabel() + "' (-> '" + to.label() + "')");
                    }

                    block.addNext(targetBlock);
                }
            }
        }

        return graph;
    }

    private class AttributeNotFoundException extends RuntimeException {
        public AttributeNotFoundException(String node, String attribute) {
            super("Attribute named '" + attribute + "' not found in node '" + node + "'");
        }
    }
}
