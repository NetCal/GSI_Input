package unikl.disco;

import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class BlockTest extends TestCase {

    private DotGraphParser parser;
    private ProtocolGraph graph;
    private Block block;

    public void setUp() throws Exception {
        super.setUp();
        parser = new DotGraphParser(DotGraphParser.class.getResourceAsStream("/cryring_fictional.dot"));
        graph = parser.parse();
        block = graph.getBlock("B_CRY_0");
    }

    public void testMaxPrefix() throws Exception {
        assertEquals("Zero length interval should be 0", 0., block.maxPrefix(0));
        assertEquals(4., block.maxPrefix(1));
        assertEquals(16., block.maxPrefix(500000000));
        assertEquals(21., block.maxPrefix(2750000000L));
        assertEquals("Wraparound to start", 25., block.maxPrefix(2750000001L));
        assertEquals("B_CRY_0 -> B_CRY_HALT -> B_CRY_HALT -> B_CRY_HALT -> B_CRY_INIT -> B_CRY_0", 30., block.maxPrefix(2750000001L + 5 * 500000));
        assertEquals("Staying in B_CRY_HALT for 17 cycles then starting again", 45., block.maxPrefix(2750000001L + 20 * 500000));
    }

    public void testMaxSuffix() throws Exception {
        assertEquals("Zero length interval should be 0", 0., block.maxSuffix(0));
        assertEquals(0., block.maxSuffix(10000000 - 1));
        assertEquals(1., block.maxSuffix(10000000));
        assertEquals(17., block.maxSuffix(2750000000L - 1));
        assertEquals(21., block.maxSuffix(2750000000L));
        assertEquals(23., block.maxSuffix(2750000000L + 1000000));
        assertEquals(25., block.maxSuffix(2750000000L + 1000000 + 2 * 500000));
        assertEquals(30., block.maxSuffix(2750000000L + 1000000 + 7 * 500000));
    }

    public void testMaxTrafficFromMessage() throws Exception {
        assertEquals(4., block.maxTraffic(0, 1));
        assertEquals(4., block.maxTraffic(1, 1));
        assertEquals(4., block.maxTraffic(2, 1));
        assertEquals(4., block.maxTraffic(3, 1));

        assertEquals(1., block.maxTraffic(4, 1));
        assertEquals(1., block.maxTraffic(5, 1));
        assertEquals(1., block.maxTraffic(6, 1));

        assertEquals(2., block.maxTraffic(9, 1));
        assertEquals(2., block.maxTraffic(10, 1));

        assertEquals(184., block.maxTraffic(20, 100000000));
    }

    public void testMaxTraffic() throws Exception {
        assertEquals(0., block.maxTraffic(0));
        assertEquals(4., block.maxTraffic(1));
        assertEquals(7., block.maxTraffic(750000));
        assertEquals(184., block.maxTraffic(100000000));
    }

    public void testEarliestTimeMaxTrafficExceeds() throws Exception {
        assertEquals(1, block.getShortestIntervalWhereMaxTrafficExceeds(3));
        assertEquals(253394, block.getShortestIntervalWhereMaxTrafficExceeds(4));
        assertEquals(503394, block.getShortestIntervalWhereMaxTrafficExceeds(6));
    }

}