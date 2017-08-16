package unikl.disco;

import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class FullyConnectedProtocolGraphTest extends TestCase {

    private FullyConnectedProtocolGraph graph;

    public void setUp() throws Exception {
        super.setUp();
        graph = new DotGraphParser(DotGraphParser.class.getResourceAsStream("/cryring_fictional.dot")).parse().fullyConnected(1);
    }

    public void testRescale() throws Exception {
        FullyConnectedRescaledProtocolGraph rescaledGraph = graph.rescale();

        long time = 0;
        int value = 0;
        while (time <= 40_000_000_000L) {
            assertTrue(graph.maxTraffic(time) <= rescaledGraph.maxTraffic(time));

            time = graph.firstTimeExceeding(value);
            value = graph.maxTraffic(time);
        }
    }

}