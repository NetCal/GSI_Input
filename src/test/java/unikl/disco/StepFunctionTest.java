package unikl.disco;

import de.uni_kl.cs.discodnc.gsi_input.StepFunction;
import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class StepFunctionTest extends TestCase {

    StepFunction func;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        func = new StepFunction();
        func.setValueAt(0, 0);
        func.setValueAt(1, 1);
        func.setValueAt(2, 2);
        func.setValueAt(4, 3);
        func.setValueAt(8, 4);
        func.setValueAt(9, 4);
        func.setValueAt(16, 5);
        func.setValueAt(32, 6);
        func.setValueAt(64, 7);
        func.setValueAt(1000000000, 7);
    }

    public void testAddValue() {
        StepFunction func = new StepFunction();
        assertEquals(0, func.getValidUpTo());
        func.setValueAt(1, 10);
        assertEquals(1, func.getValidUpTo());
        func.setValueAt(2, 10);
        assertEquals(2, func.getValidUpTo());
        func.setValueAt(2, 12);
        assertEquals(2, func.getValidUpTo());
        func.setValueAt(12, 1002);
        assertEquals(12, func.getValidUpTo());
    }


    public void testGetValue() throws Exception {
        assertEquals("Time before function returns 0", 0., func.getValue(-1));
        assertEquals("Exact match returns exact value", 0., func.getValue(0));
        assertEquals("Exact match returns exact value", 1., func.getValue(1));
        assertEquals("Exact match returns exact value", 7., func.getValue(64));
        assertEquals("Inexact match returns previous value", 2., func.getValue(3));
        assertEquals("Inexact match returns previous value", 4., func.getValue(12));
        assertEquals("Inexact match returns previous value", 7., func.getValue(99));
        assertEquals("Inexact match returns previous value", 7., func.getValue(103));
    }

    public void testMaxInterval() {
        assertEquals(0., func.maximumInterval(0, 32));
        assertEquals(1., func.maximumInterval(1, 32));
        assertEquals(2., func.maximumInterval(2, 32));
        assertEquals(2., func.maximumInterval(3, 32));
        assertEquals(3., func.maximumInterval(4, 32));
        assertEquals(3., func.maximumInterval(5, 32));
        assertEquals(3., func.maximumInterval(6, 32));
        assertEquals(3., func.maximumInterval(7, 32));
        assertEquals(4., func.maximumInterval(8, 32));
    }

    public void testMaxValue() {
        assertEquals(7., func.maximumValue());
    }

    public void testLastStepTime() {
        assertEquals(64, func.lastStepTime());
    }

    public void testFirstStepAfter() {
        assertEquals(0, func.nextIncrementTimeAfter(-1));
        assertEquals(1, func.nextIncrementTimeAfter(0));
        assertEquals(2, func.nextIncrementTimeAfter(1));
        assertEquals(4, func.nextIncrementTimeAfter(2));
        assertEquals(4, func.nextIncrementTimeAfter(3));
        assertEquals(8, func.nextIncrementTimeAfter(4));

        try {
            func.nextIncrementTimeAfter(64);
            fail();
        } catch (IllegalArgumentException ex) {
            // pass
        }
    }

    public void testFirstStepExceeding() {
        assertEquals(0, func.firstTimeExceeding(-1));
        assertEquals(1, func.firstTimeExceeding(0));
        assertEquals(2, func.firstTimeExceeding(1));
        assertEquals(4, func.firstTimeExceeding(2));
        assertEquals(8, func.firstTimeExceeding(3));

        try {
            func.firstTimeExceeding(7);
            fail();
        } catch (IllegalArgumentException ex) {
            // pass
        }
    }
}