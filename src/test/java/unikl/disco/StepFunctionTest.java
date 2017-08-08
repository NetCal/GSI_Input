package unikl.disco;

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
        func.setValueAt(16, 5);
        func.setValueAt(32, 6);
        func.setValueAt(64, 7);
    }


    public void testGetValue() throws Exception {
        assertEquals("Time before function returns 0", 0, func.getValue(-1));
        assertEquals("Exact match returns exact value", 0, func.getValue(0));
        assertEquals("Exact match returns exact value", 1, func.getValue(1));
        assertEquals("Exact match returns exact value", 7, func.getValue(64));
        assertEquals("Inexact match returns previous value", 2, func.getValue(3));
        assertEquals("Inexact match returns previous value", 4, func.getValue(12));
        assertEquals("Inexact match returns previous value", 7, func.getValue(99));
        assertEquals("Inexact match returns previous value", 7, func.getValue(103));
    }

}