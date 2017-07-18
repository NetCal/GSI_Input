package unikl.disco;

import junit.framework.TestCase;

import java.util.Set;

/**
 * @author Malte Schütze
 */
public class DotGraphParserTest extends TestCase {

    private DotGraphParser parser;
    private ProtocolGraph graph;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        parser = new DotGraphParser(DotGraphParser.class.getResourceAsStream("/cryring_fictional.dot"));
        graph = parser.parse();
    }

    public void testNumBlocks() throws Exception {
        assertEquals(4, graph.getBlockCount());
    }

    public void testBlockByName() throws Exception {
        assertNotNull(graph.getBlock("B_CRY_INIT"));
        assertNotNull(graph.getBlock("B_CRY_0"));
        assertNotNull(graph.getBlock("B_CRY_1"));
        assertNotNull(graph.getBlock("B_CRY_HALT"));

        try {
            graph.getBlock("This block does not exist");
            fail("Trying to retrieve a block that does not exist should throw an exception");
        } catch (IllegalArgumentException ex) {
            // Ok
        }
    }

    public void testBlockMessageCount() {
        assertEquals(2, graph.getBlock("B_CRY_INIT").getNumMessages());
        assertEquals(21, graph.getBlock("B_CRY_0").getNumMessages());
        assertEquals(19, graph.getBlock("B_CRY_1").getNumMessages());
        assertEquals(1, graph.getBlock("B_CRY_HALT").getNumMessages());
    }

    public void testBlockMessageContents() {
        // Single message, correctly parsed
        Message message = graph.getBlock("B_CRY_HALT").getMessage(0);
        assertEquals("CRY_HALT", message.getLabel());
        assertEquals(1, message.getSize());
        assertEquals(0, message.getOffset());
        assertEquals(graph.getBlock("B_CRY_HALT"), message.getBlock());

        // Message ordering retained for message with same offset
        message = graph.getBlock("B_CRY_0").getMessage(0);
        assertEquals("CRY_0_SEQ_START_0", message.getLabel());
        message = graph.getBlock("B_CRY_0").getMessage(1);
        assertEquals("CRY_0_SEQ_START_1", message.getLabel());
        message = graph.getBlock("B_CRY_0").getMessage(2);
        assertEquals("CRY_0_SEQ_START_2", message.getLabel());
        message = graph.getBlock("B_CRY_0").getMessage(3);
        assertEquals("CRY_0_SEQ_START_3", message.getLabel());
    }

    public void testBlockLinks() {
        assertEquals(1, graph.getBlock("B_CRY_INIT").getNextBlocks().size());
        assertEquals("B_CRY_0", graph.getBlock("B_CRY_INIT").getNextBlocks().iterator().next().getLabel());

        Set<Block> bCry0NextBlocks = graph.getBlock("B_CRY_0").getNextBlocks();
        assertEquals(3, bCry0NextBlocks.size());
        assertTrue(bCry0NextBlocks.contains(graph.getBlock("B_CRY_0")));
        assertTrue(bCry0NextBlocks.contains(graph.getBlock("B_CRY_1")));
        assertTrue(bCry0NextBlocks.contains(graph.getBlock("B_CRY_HALT")));

        Set<Block> bCryHaltNextBlocks = graph.getBlock("B_CRY_HALT").getNextBlocks();
        assertEquals(2, bCryHaltNextBlocks.size());
        assertTrue(bCryHaltNextBlocks.contains(graph.getBlock("B_CRY_HALT")));
        assertTrue(bCryHaltNextBlocks.contains(graph.getBlock("B_CRY_INIT")));


        Set<Block> bCry1PreviousBlocks = graph.getBlock("B_CRY_1").getPreviousBlocks();
        assertEquals(2, bCry1PreviousBlocks.size());
        assertTrue(bCry1PreviousBlocks.contains(graph.getBlock("B_CRY_0")));
        assertTrue(bCry1PreviousBlocks.contains(graph.getBlock("B_CRY_1")));
    }

    public void testBlockPeriod() {
        assertEquals(500000, graph.getBlock("B_CRY_HALT").getPeriod());
        assertEquals(2750000000L, graph.getBlock("B_CRY_1").getPeriod());
    }
}