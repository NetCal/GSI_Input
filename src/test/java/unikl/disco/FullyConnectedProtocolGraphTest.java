package unikl.disco;

import de.uni_kl.cs.discodnc.gsi_input.DotGraphParser;
import de.uni_kl.cs.discodnc.gsi_input.FullyConnectedProtocolGraph;
import de.uni_kl.cs.discodnc.gsi_input.FullyConnectedRescaledProtocolGraph;
import de.uni_kl.cs.discodnc.gsi_input.ProtocolGraph;
import de.uni_kl.cs.discodnc.gsi_input.PseudoPeriodicFunction;
import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class FullyConnectedProtocolGraphTest extends TestCase {

    private FullyConnectedProtocolGraph fcGraph;
    private ProtocolGraph graph;

    public void setUp() throws Exception {
        super.setUp();
        graph = new DotGraphParser(DotGraphParser.class.getResourceAsStream("/cryring_fictional.dot")).parse();
        fcGraph = graph.fullyConnected(1);
    }

    public void testRescale() throws Exception {
        FullyConnectedRescaledProtocolGraph rescaledGraph = fcGraph.rescale();

        long time = 0;
        double value = 0;
        while (time <= 40_000_000_000L) {
            assertTrue(fcGraph.maxTraffic(time) <= rescaledGraph.maxTraffic(time));

            time = fcGraph.firstTimeExceeding(value);
            value = fcGraph.maxTraffic(time);
        }
    }

    public void testApproximateMostEfficientLoop() {
        PseudoPeriodicFunction f = fcGraph.approximateMostEfficientLoop();
        f.concaveHull();
        long time = 0;
        double value = 0;
        while (time <= 40_000_000_000L) {
            assertTrue(f.getValue(time) >= value);
            time = graph.firstTimeExceeding(value);
            value = graph.maxTraffic(time);
        }
    }
}