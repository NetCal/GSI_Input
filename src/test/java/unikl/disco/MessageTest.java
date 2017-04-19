package unikl.disco;

import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class MessageTest extends TestCase {

    ProtocolSpec spec;
    Message first;
    Message second;
    Message last;

    @Override
    protected void setUp() throws Exception {
        /*
        Configure a spec like this:

                    5 - x - 1 - x - x
                  /                    \
        x - 4 - 1                        1 - x
                  \                    /
                    x - 1 - x - x - 10
         */

        spec = new ProtocolSpec(10);
        first = new Message(4, 1, spec);
        second = new Message(1, 2, spec);
        Message left1 = new Message(5, 3, spec);
        Message left2 = new Message(1, 5, spec);
        Message right1 = new Message(1, 4, spec);
        Message right2 = new Message(10, 7, spec);
        last = new Message(1, 8, spec);

        first.addNext(second);
        second.addNext(left1);
        second.addNext(right1);
        left1.addNext(left2);
        right1.addNext(right2);
        left2.addNext(last);
        right2.addNext(last);

        spec.add(first);
        spec.add(second);
        spec.add(left1);
        spec.add(left2);
        spec.add(right1);
        spec.add(right2);
        spec.add(last);
    }

    public void testAddAndRemove() throws Exception {
        assertEquals(17, first.maxTraffic(10));

        Message msg = new Message(3, 9, spec);
        last.addNext(msg);
        assertEquals(20, first.maxTraffic(10));

        last.removeNext(msg);
        assertEquals(17, first.maxTraffic(10));
    }

    public void testGetNumOptions() throws Exception {
        assertEquals(1, first.getNumOptions());
        assertEquals(2, second.getNumOptions());
        assertEquals(0, last.getNumOptions());
    }

    public void testMaxTraffic() throws Exception {
        assertEquals("0-length interval is always empty",0, first.maxTraffic(0));
        assertEquals("toTime is exclusive", 0, first.maxTraffic(1));
        assertEquals("to(2) includes only message at t=1", 4, first.maxTraffic(2));
        assertEquals("to(3) includes messages at t=1 and 2", 5, first.maxTraffic(3));
        assertEquals("to(4) picks path left", 10, first.maxTraffic(4));
        assertEquals("to(5) picks path left and ignores message at right", 10, first.maxTraffic(5));
        assertEquals("to(6) picks path left and ignores message at right", 11, first.maxTraffic(6));
        assertEquals("to(7) picks path left and ignores message at right", 11, first.maxTraffic(7));
        assertEquals("to(8) switches to right path", 16, first.maxTraffic(8));
        assertEquals("to(9) includes last via right", 17, first.maxTraffic(9));
        assertEquals("to(10) includes last via right", 17, first.maxTraffic(10));
        assertEquals("to(11) has wrapover but does not include first", 17, first.maxTraffic(11));
        assertEquals("to(12) has wrapover and includes first", 21, first.maxTraffic(12));
    }

}