package unikl.disco;

import junit.framework.TestCase;
import unikl.disco.curves.ArrivalCurve;
import unikl.disco.numbers.Num;
import unikl.disco.numbers.NumFactory;

import java.util.Arrays;

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

    public void testApproximateSubadditiveIsAboveActualArrivalCurve() throws Exception {
        for (long interval: Arrays.asList(1_000L, 1_000_000L, 1_000_000_000L, 10_000_000_000L, 1_000_000_000_000L)) {
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
                assertTrue(f.getValue(10 * f.periodLength + time - 1) >= graph.maxTraffic(10 * f.periodLength + time - 1));
                assertSegmentAboveCurve(f, curve, 10 * f.periodLength + time - 1);
                assertTrue(f.getValue(10 * f.periodLength + time) >= graph.maxTraffic(10 * f.periodLength + time));
                assertSegmentAboveCurve(f, curve, 10 * f.periodLength + time);
                assertTrue(f.getValue(10 * f.periodLength + time + 1) >= graph.maxTraffic(10 * f.periodLength + time + 1));
                assertSegmentAboveCurve(f, curve, 10 * f.periodLength + time + 1);
            }

        }
    }

    private void assertSegmentAboveCurve(PseudoPeriodicFunction f, ArrivalCurve curve, long time) {
        Num x = NumFactory.create(time);
        int segmentId = curve.getSegmentDefining(x);
        assertTrue(curve.getSegment(segmentId).f(x).geq(NumFactory.create(f.getValue(time))));
    }
}