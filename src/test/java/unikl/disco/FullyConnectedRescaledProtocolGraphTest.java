package unikl.disco;

import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class FullyConnectedRescaledProtocolGraphTest extends TestCase {

    private ProtocolGraph graph;
    private FullyConnectedRescaledProtocolGraph rescaledGraph;

    public void setUp() throws Exception {
        super.setUp();
        graph = new DotGraphParser(DotGraphParser.class.getResourceAsStream("/cryring_fictional.dot")).parse();
        rescaledGraph = graph.fullyConnected(1).rescale();
    }

    public void testApproximateTightestLoop() throws Exception {
        PseudoPeriodicFunction f = rescaledGraph.approximateTightestLoop();
        f.concaveHull();
        long time = 0;
        int value = 0;
        while (time <= 40_000_000_000L) {
            assertTrue(f.getValue(time) >= value);
            time = graph.firstTimeExceeding(value);
            value = graph.maxTraffic(time);
        }
    }

}