package unikl.disco;

import unikl.disco.curves.ArrivalCurve;
import unikl.disco.numbers.NumFactory;

import java.util.stream.Collectors;

/**
 * @author Malte Schütze
 */
public class Utils {
    public static void dumpPseudoperiodicFunctionMatplotlib(PseudoPeriodicFunction f) {
        String xs = f.incrementTimeSteps.stream().map(Object::toString).collect(Collectors.joining(", "));
        String ys = f.incrementValues.stream().map(Object::toString).collect(Collectors.joining(", "));

        System.out.println("x = [" + xs + "]");
        System.out.println("y = [" + ys + "]");
        System.out.println("plt.step(x, y, where='post')");
        System.out.println("plt.axvline(" + f.periodLength + ")");
        System.out.println("plt.axhline(" + f.periodIncrement + ")");
        System.out.println();
    }

    public static void dumpArrivalCurveMatplotlib(ArrivalCurve curve, long finalSegmentEnd) {
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
                xs.append(curve.getSegment(i+1).getX().doubleValue());
                ys.append(curve.getSegment(i).f(curve.getSegment(i+1).getX()).doubleValue());
            } else {
                xs.append(finalSegmentEnd);
                ys.append(curve.getSegment(i).f(NumFactory.create(finalSegmentEnd)).doubleValue());
            }
        }

        System.out.println("x = [" + xs + "]");
        System.out.println("y = [" + ys + "]");
        System.out.println("plt.plot(x, y)");
        System.out.println();
    }
}
