package org.networkcalculus.dnc.gsi_input;

import org.networkcalculus.dnc.curves.ArrivalCurve;

/**
 * @author Malte Schütze
 */
public interface OutputFormatter {
    void printPseudoperiodicFunction(ProtocolGraph graph, PseudoPeriodicFunction f, long time);
    void printArrivalCurve(ArrivalCurve curve, long time);
    void printMaxTraffic(ProtocolGraph graph, long time);
}
