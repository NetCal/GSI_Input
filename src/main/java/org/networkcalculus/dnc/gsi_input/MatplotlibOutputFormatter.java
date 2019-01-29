package org.networkcalculus.dnc.gsi_input;

import java.util.List;
import java.util.stream.Collectors;

import org.networkcalculus.dnc.Calculator;
import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.numbers.Num;

/**
 * @author Malte Schütze
 */
public class MatplotlibOutputFormatter implements OutputFormatter {
    public void printPseudoperiodicFunction(ProtocolGraph graph, PseudoPeriodicFunction f, long finalTime) {
        // Run full approximation just to find the time steps
        boolean verbose = graph.args.verbose;
        graph.args.verbose = false;
        List<Long> steps = graph.approximateSubadditive(finalTime).incrementTimeSteps;
        graph.args.verbose = true;

        String xs = steps.stream().map(Object::toString).collect(Collectors.joining(", "));
        String ys = steps.stream().map(t -> Double.toString(f.getValue(t))).collect(Collectors.joining(", "));

        System.out.println("# Pseudo-periodic function up to " + finalTime);
        System.out.println("plt.axvline(" + f.periodBegin + ")");
        System.out.println("plt.axvline(" + (f.periodBegin + f.periodLength) + ")");
        System.out.println("plt.axhline(" + f.getValue(f.periodBegin) + ")");
        System.out.println("plt.axhline(" + f.getValue(f.periodBegin + f.periodLength) + ")");
        System.out.println("x = [" + xs + "]");
        System.out.println("y = [" + ys + "]");
        System.out.println("legend_entry_pp, = plt.step(x, y, where='post', label='Pseudoperiodic Approximation')");
        System.out.println();
    }

    public void printArrivalCurve(ArrivalCurve curve, long finalSegmentEnd) {
        StringBuilder xs = new StringBuilder();
        StringBuilder ys = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < curve.getSegmentCount(); i++) {
            if (!first) {
                xs.append(", ");
                ys.append(", ");
            } else {
                first = false;
            }

            xs.append(curve.getSegment(i).getX().doubleValue());
            ys.append(curve.getSegment(i).getY().doubleValue());
            xs.append(", ");
            ys.append(", ");
            if (i != curve.getSegmentCount() - 1) {
                xs.append(curve.getSegment(i + 1).getX().doubleValue());
                ys.append(curve.getSegment(i).f(curve.getSegment(i + 1).getX()).doubleValue());
            } else {
                xs.append(finalSegmentEnd);
                ys.append(curve.getSegment(i).f(
                		Num.getFactory(Calculator.getInstance().getNumBackend())
                		.create(finalSegmentEnd)).doubleValue());
            }
        }

        System.out.println("# Concave hull up to " + finalSegmentEnd);
        System.out.println("x = [" + xs + "]");
        System.out.println("y = [" + ys + "]");
        System.out.println("legend_entry_ch, = plt.plot(x, y, label='Concave Hull')");
        System.out.println();
    }

    public void printMaxTraffic(ProtocolGraph graph, long maxTime) {
        boolean verbose = graph.args.verbose;
        graph.args.verbose = false;
        PseudoPeriodicFunction f = graph.approximateSubadditive(maxTime);
        graph.args.verbose = verbose;

        String xs = f.incrementTimeSteps.stream().map(Object::toString).collect(Collectors.joining(", "));
        String ys = f.incrementValues.stream().map(Object::toString).collect(Collectors.joining(", "));
        xs += ", " + maxTime;
        ys += ", " + f.getValue(maxTime);

        System.out.println("# Actual traffic up to " + maxTime);
        System.out.println("x = [" + xs + "]");
        System.out.println("y = [" + ys + "]");
        System.out.println("legend_entry_mt, = plt.step(x, y, where='post', label='Max Traffic')");
        System.out.println();
    }

    @Override
    public String toString() {
        return "matplotlib";
    }
}
