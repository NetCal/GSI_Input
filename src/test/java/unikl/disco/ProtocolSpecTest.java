package unikl.disco;

import junit.framework.TestCase;
import unikl.disco.curves.ArrivalCurve;
import unikl.disco.curves.LinearSegment;
import unikl.disco.numbers.Num;
import unikl.disco.numbers.NumFactory;

/**
 * @author Malte Schütze
 */
public class ProtocolSpecTest extends TestCase {

    ProtocolSpec spec;
    Message first;
    Message second;
    Message secondLast;
    Message last;

    public void setUp() throws Exception {
        /*
        Configure a spec like this:

                    6 - x - 1 - x - x
                  /                    \
        x - 4 - 6                        1 - 8
                  \                    /
                    x - 1 - x - x - 10
         */

        spec = new ProtocolSpec(10);
        first = new Message(4, 1, spec);
        second = new Message(6, 2, spec);
        Message left1 = new Message(6, 3, spec);
        Message left2 = new Message(1, 5, spec);
        Message right1 = new Message(1, 4, spec);
        Message right2 = new Message(10, 7, spec);
        secondLast = new Message(1, 8, spec);
        last = new Message(8, 9, spec);

        first.addNext(second);
        second.addNext(left1);
        second.addNext(right1);
        left1.addNext(left2);
        right1.addNext(right2);
        left2.addNext(secondLast);
        right2.addNext(secondLast);
        secondLast.addNext(last);

        spec.add(first);
        spec.add(second);
        spec.add(left1);
        spec.add(left2);
        spec.add(right1);
        spec.add(right2);
        spec.add(secondLast);
        spec.add(last);
    }

    public void testCreateArrivalCurve() throws Exception {
        ArrivalCurve curve = spec.createArrivalCurve();
        for (int i = 1; i < curve.getSegmentCount(); i++) {
            LinearSegment segment = curve.getSegment(i);
            LinearSegment previous = curve.getSegment(i - 1);
            if (!previous.isLeftopen() && previous.getX().eqZero() && previous.getGrad().eqZero() && segment.getX().eqZero()) {
                // "zero" segment at begin of hull
                continue;
            }

            assertTrue("Increasing gradient in concave hull: " + previous + " -> " + segment, segment.getGrad().leq(previous.getGrad()));
        }


        for (int i = 0; i < spec.cycleLength * 4; i++) {
            LinearSegment segment = curve.getSegment(curve.getSegmentDefining(NumFactory.create(i)));
            Num y = segment.f(NumFactory.create(i));
            assertTrue("Concave hull below strict arrival curve",y.doubleValue() >= spec.maxTrafficInInterval(i));
        }
    }

    public void testAdd() throws Exception {
        Message veryLast = new Message(20, 9, spec);
        this.secondLast.addNext(veryLast);
        spec.add(veryLast);

        assertEquals(20, spec.maxTrafficInInterval(1));
        assertEquals(31, spec.maxTrafficInInterval(3));

        try {
            spec.add(new Message(0, 10, spec));
            fail("Should not be able to add message with transmission time >= cycle time");
        } catch (IllegalArgumentException ex) {
            // Ok
        }

        try {
            spec.add(new Message(0, -1, spec));
            fail("Should not be able to add message with transmission time < 0");
        } catch (IllegalArgumentException ex) {
            // Ok
        }
    }

    public void testMaxTraffic() throws Exception {
        for (int i = 0; i < 2 * spec.cycleLength; i++) {
            assertEquals("spec.maxTraffic() should delegate to first message", first.maxTraffic(i), spec.maxTraffic(i));
        }
    }

    public void testMaxTrafficInInterval() throws Exception {

        /*
        Configure a spec like this:

                    6 - x - 1 - x - x
                  /                    \
        x - 4 - 6                        1 - 8
                  \                    /
                    x - 1 - x - x - 10
         */
        assertEquals("Max traffic in 0-length interval should be 0", 0, spec.maxTrafficInInterval(0));
        assertEquals("inInterval should pick largest-size message for interval 1", 10, spec.maxTrafficInInterval(1));
        assertEquals(12, spec.maxTrafficInInterval(2));
        assertEquals(19, spec.maxTrafficInInterval(3));
        assertEquals(19, spec.maxTrafficInInterval(4));
        assertEquals(24, spec.maxTrafficInInterval(5));
        assertEquals(29, spec.maxTrafficInInterval(6));
        assertEquals(35, spec.maxTrafficInInterval(7));
        assertEquals(35, spec.maxTrafficInInterval(8));
        assertEquals(36, spec.maxTrafficInInterval(9));
        assertEquals(36, spec.maxTrafficInInterval(10));

        assertEquals(40, spec.maxTrafficInInterval(11));
        assertEquals(42, spec.maxTrafficInInterval(12));
        assertEquals(49, spec.maxTrafficInInterval(13));
        assertEquals(49, spec.maxTrafficInInterval(14));
        assertEquals(54, spec.maxTrafficInInterval(15));
        assertEquals(59, spec.maxTrafficInInterval(16));
        assertEquals(65, spec.maxTrafficInInterval(17));
        assertEquals(65, spec.maxTrafficInInterval(18));
        assertEquals(66, spec.maxTrafficInInterval(19));
        assertEquals(66, spec.maxTrafficInInterval(20));

        assertEquals(70, spec.maxTrafficInInterval(21));
        assertEquals(72, spec.maxTrafficInInterval(22));
        assertEquals(79, spec.maxTrafficInInterval(23));
        assertEquals(79, spec.maxTrafficInInterval(24));
        assertEquals(84, spec.maxTrafficInInterval(25));
        assertEquals(89, spec.maxTrafficInInterval(26));
        assertEquals(95, spec.maxTrafficInInterval(27));
        assertEquals(95, spec.maxTrafficInInterval(28));
        assertEquals(96, spec.maxTrafficInInterval(29));
        assertEquals(96, spec.maxTrafficInInterval(30));
    }

}