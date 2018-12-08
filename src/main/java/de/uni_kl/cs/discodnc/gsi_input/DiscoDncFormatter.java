package de.uni_kl.cs.discodnc.gsi_input;

import de.uni_kl.cs.discodnc.gsi_input.curves.ArrivalCurve;

/**
 * @author Malte Schütze
 */
public class DiscoDncFormatter implements OutputFormatter {
    @Override
    public void printPseudoperiodicFunction(ProtocolGraph graph, PseudoPeriodicFunction f, long time) {
        // pseudoperiodic function omitted
    }

    @Override
    public void printArrivalCurve(ArrivalCurve curve, long time) {
        System.out.println(curve.toString());
    }

    @Override
    public void printMaxTraffic(ProtocolGraph graph, long time) {
        // max traffic omitted
    }

    public String toString() {
        return "discodnc";
    }
}
