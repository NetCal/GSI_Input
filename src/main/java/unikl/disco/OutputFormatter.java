package unikl.disco;

import unikl.disco.curves.ArrivalCurve;

/**
 * @author Malte Schütze
 */
public interface OutputFormatter {
    void printPseudoperiodicFunction(ProtocolGraph graph, PseudoPeriodicFunction f, long time);
    void printArrivalCurve(ArrivalCurve curve, long time);
    void printMaxTraffic(ProtocolGraph graph, long time);
}
