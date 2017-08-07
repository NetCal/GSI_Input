package unikl.disco;

import junit.framework.TestCase;
import unikl.disco.curves.ArrivalCurve;
import unikl.disco.curves.LinearSegment;
import unikl.disco.numbers.NumFactory;

/**
 * @author Malte Schütze
 */
public class PseudoPeriodicFunctionTest extends TestCase {

    PseudoPeriodicFunction func;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        func = new PseudoPeriodicFunction(100, 5, 2);
        func.setValueAt(0, 0);
        func.setValueAt(1, 1);
        func.setValueAt(2, 2);
        func.setValueAt(4, 3);
        func.setValueAt(8, 4);
        func.setValueAt(16, 5);
        func.setValueAt(32, 6);
        func.setValueAt(64, 7);
        func.setValueAt(102, 8);
    }

    public void testGetValue() throws Exception {
        assertEquals("Time before function returns 0", 0, func.getValue(-1));
        assertEquals("Exact match returns exact value", 0, func.getValue(0));
        assertEquals("Exact match returns exact value", 1, func.getValue(1));
        assertEquals("Exact match returns exact value", 7, func.getValue(64));
        assertEquals("Exact match returns exact value", 8, func.getValue(102));
        assertEquals("Inexact match returns previous value", 2, func.getValue(3));
        assertEquals("Inexact match returns previous value", 4, func.getValue(12));
        assertEquals("Inexact match returns previous value", 7, func.getValue(99));
        assertEquals("Inexact match returns previous value", 8, func.getValue(103));
        assertEquals("Increment used correctly", 9, func.getValue(105));
        assertEquals("Increment used correctly", 11, func.getValue(110));
        assertEquals("Increment used correctly", 13, func.getValue(115));
        assertEquals("Function interpolated correctly", 9, func.getValue(106));
        assertEquals("Function interpolated correctly", 10, func.getValue(107));
        assertEquals("Function interpolated correctly", 11, func.getValue(111));
        assertEquals("Function interpolated correctly", 12, func.getValue(112));
    }

    public void testIncrementValueAtPeriodBorder() throws Exception {
        func.setValueAt(105, 9);

        assertEquals("Time before function returns 0", 0, func.getValue(-1));
        assertEquals("Exact match returns exact value", 0, func.getValue(0));
        assertEquals("Exact match returns exact value", 1, func.getValue(1));
        assertEquals("Exact match returns exact value", 7, func.getValue(64));
        assertEquals("Exact match returns exact value", 8, func.getValue(102));
        assertEquals("Inexact match returns previous value", 2, func.getValue(3));
        assertEquals("Inexact match returns previous value", 4, func.getValue(12));
        assertEquals("Inexact match returns previous value", 7, func.getValue(99));
        assertEquals("Inexact match returns previous value", 8, func.getValue(103));
        assertEquals("Increment used correctly", 9, func.getValue(105));
        assertEquals("Increment used correctly", 11, func.getValue(110));
        assertEquals("Increment used correctly", 13, func.getValue(115));
        assertEquals("Function interpolated correctly", 9, func.getValue(106));
        assertEquals("Function interpolated correctly", 10, func.getValue(107));
        assertEquals("Function interpolated correctly", 11, func.getValue(111));
        assertEquals("Function interpolated correctly", 12, func.getValue(112));
    }

    public void testConcaveHull() {
        ArrivalCurve curve = func.concaveHull();

        for (int i = 2; i < curve.getSegmentCount(); i++) {
            LinearSegment segment = curve.getSegment(i);
            LinearSegment previous = curve.getSegment(i-1);

            assertTrue("Decreasing gradient in linear segments", segment.getGrad().leq(previous.getGrad()));
        }

        assertEquals(2, curve.getSegmentCount());
        assertEquals(NumFactory.createZero(), curve.getSegment(1).getX());
        assertEquals(NumFactory.create(1, 1), curve.getSegment(1).getY());
        assertEquals(NumFactory.create(1, 1), curve.getSegment(1).getGrad());
    }
}