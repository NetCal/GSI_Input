package de.uni_kl.cs.discodnc.gsi_input;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_kl.cs.discodnc.Calculator;
import de.uni_kl.cs.discodnc.curves.ArrivalCurve;
import de.uni_kl.cs.discodnc.gsi_input.Block;
import de.uni_kl.cs.discodnc.gsi_input.DotGraphParser;
import de.uni_kl.cs.discodnc.gsi_input.FullyConnectedProtocolGraph;
import de.uni_kl.cs.discodnc.gsi_input.MatplotlibOutputFormatter;
import de.uni_kl.cs.discodnc.gsi_input.ProtocolGraph;
import de.uni_kl.cs.discodnc.gsi_input.PseudoPeriodicFunction;
import de.uni_kl.cs.discodnc.numbers.Num;

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
        assertEquals(0., graph.maxTraffic(0));
        assertEquals(4., graph.maxTraffic(1));
        assertEquals(203., graph.maxTraffic(100000000));
    }

    public void testApproximateSubadditiveIsAboveActualArrivalCurve() throws Exception {
        for (long interval : Arrays.asList(1_000L, 1_000_000L, 1_000_000_000L, 10_000_000_000L)) {
            System.out.println("Testing interval " + interval);
            PseudoPeriodicFunction f = graph.approximateSubadditive(interval);
            ArrivalCurve curve = f.concaveHull();
            for (long time : f.incrementTimeSteps) {
                if (time > 0) {
                    assertTrue(f.getValue(time - 1) >= graph.maxTraffic(time - 1));
                    assertSegmentAboveCurve(f, curve, time - 1);
                }
                assertTrue(f.getValue(time) >= graph.maxTraffic(time));
                assertSegmentAboveCurve(f, curve, time);
                assertTrue(f.getValue(time + 1) >= graph.maxTraffic(time + 1));
                assertSegmentAboveCurve(f, curve, time + 1);

                // In the first approximated period
                assertTrue(f.getValue(f.periodLength + time - 1) >= graph.maxTraffic(f.periodLength + time - 1));
                assertSegmentAboveCurve(f, curve, f.periodLength + time - 1);
                assertTrue(f.getValue(f.periodLength + time) >= graph.maxTraffic(f.periodLength + time));
                assertSegmentAboveCurve(f, curve, f.periodLength + time);
                assertTrue(f.getValue(f.periodLength + time + 1) >= graph.maxTraffic(f.periodLength + time + 1));
                assertSegmentAboveCurve(f, curve, f.periodLength + time + 1);

                // In a couple of approximated period
                assertTrue(f.getValue(40 * f.periodLength + time - 1) >= graph.maxTraffic(40 * f.periodLength + time - 1));
                assertSegmentAboveCurve(f, curve, 40 * f.periodLength + time - 1);
                assertTrue(f.getValue(40 * f.periodLength + time) >= graph.maxTraffic(40 * f.periodLength + time));
                assertSegmentAboveCurve(f, curve, 40 * f.periodLength + time);
                assertTrue(f.getValue(40 * f.periodLength + time + 1) >= graph.maxTraffic(40 * f.periodLength + time + 1));
                assertSegmentAboveCurve(f, curve, 40 * f.periodLength + time + 1);
            }

        }
    }

    Num num_factory = Num.getFactory(Calculator.getInstance().getNumBackend());
    private void assertSegmentAboveCurve(PseudoPeriodicFunction f, ArrivalCurve curve, long time) {
        Num x = num_factory.create(time);
        int segmentId = curve.getSegmentDefining(x);
        assertTrue(curve.getSegment(segmentId).f(x).geq(num_factory.create(f.getValue(time))));
    }

    public void testBlocksToSuperBlock() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(graph.getBlock("B_CRY_0"));
        blocks.add(graph.getBlock("B_CRY_0"));
        blocks.add(graph.getBlock("B_CRY_HALT"));

        Block superBlock = graph.blocksToSuperBlock(blocks);
        assertEquals("B_CRY_0--B_CRY_0--B_CRY_HALT", superBlock.getLabel());
        assertEquals(5500500000L, superBlock.getPeriod());
        assertEquals(21., superBlock.maxPrefix(2750000000L));
        assertEquals(42., superBlock.maxPrefix(5500000000L));
        assertEquals(43., superBlock.maxPrefix(5500500000L));
    }

    public void testGetSuccessiveBlocksOfSpecificBlock() {
        Block block = graph.getBlock("B_CRY_0");
        Set<List<Block>> result = graph.getSuccessiveBlocks(block, 3);
        Set<String> asString = result.stream().map(l -> l.stream().map(Block::getLabel).collect(Collectors.joining(" -> "))).collect(Collectors.toSet());

        assertTrue(asString.contains("B_CRY_0 -> B_CRY_0 -> B_CRY_0"));
        assertTrue(asString.contains("B_CRY_0 -> B_CRY_0 -> B_CRY_1"));
        assertTrue(asString.contains("B_CRY_0 -> B_CRY_0 -> B_CRY_HALT"));
        assertTrue(asString.contains("B_CRY_0 -> B_CRY_1 -> B_CRY_0"));
        assertTrue(asString.contains("B_CRY_0 -> B_CRY_1 -> B_CRY_1"));
        assertTrue(asString.contains("B_CRY_0 -> B_CRY_1 -> B_CRY_HALT"));
        assertTrue(asString.contains("B_CRY_0 -> B_CRY_HALT -> B_CRY_HALT"));
        assertTrue(asString.contains("B_CRY_0 -> B_CRY_HALT -> B_CRY_INIT"));
        assertEquals(8, asString.size());
    }

    public void testGetFullyConnected() {
        FullyConnectedProtocolGraph fcGraph1 = graph.fullyConnected(1);
        FullyConnectedProtocolGraph fcGraph2 = graph.fullyConnected(2);
        FullyConnectedProtocolGraph fcGraph4 = graph.fullyConnected(4);

        long time = 0;
        double value = 0;
        while (time <= 40_000_000_000L) {
            assertTrue(graph.maxTraffic(time) <= fcGraph4.maxTraffic(time));
            assertTrue(fcGraph4.maxTraffic(time) <= fcGraph2.maxTraffic(time));
            assertTrue(fcGraph2.maxTraffic(time) <= fcGraph1.maxTraffic(time));

            time = graph.firstTimeExceeding(value);
            value = graph.maxTraffic(time);
        }
    }

    public void testDumpGraph() {
        PseudoPeriodicFunction f = graph.approximateSubadditive(10_000_000_000L);
        ArrivalCurve curve = f.concaveHull();
        new MatplotlibOutputFormatter().printMaxTraffic(graph, 20_000_000_000L);
        new MatplotlibOutputFormatter().printPseudoperiodicFunction(graph, f, 20_000_000_000L);
        new MatplotlibOutputFormatter().printArrivalCurve(curve, 20_000_000_000L);
    }
}