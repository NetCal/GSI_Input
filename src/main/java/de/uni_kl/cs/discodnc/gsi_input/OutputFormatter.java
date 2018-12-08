package de.uni_kl.cs.discodnc.gsi_input;

import de.uni_kl.cs.discodnc.gsi_input.curves.ArrivalCurve;

/**
 * @author Malte Schütze
 */
public interface OutputFormatter {
    void printPseudoperiodicFunction(ProtocolGraph graph, PseudoPeriodicFunction f, long time);
    void printArrivalCurve(ArrivalCurve curve, long time);
    void printMaxTraffic(ProtocolGraph graph, long time);
}
