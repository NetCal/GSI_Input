package unikl.disco;

import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class ProtocolGraphTest extends TestCase {

    private ProtocolGraph graph;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        graph = new DotGraphParser(DotGraphParser.class.getResourceAsStream("/cryring_fictional.dot")).parse();
    }

    public void testMaxTraffic() throws Exception {
        assertEquals(0, graph.maxTraffic(0));
        assertEquals(4, graph.maxTraffic(1));
        assertEquals(203, graph.maxTraffic(100000000));
    }

}