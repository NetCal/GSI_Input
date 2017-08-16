package unikl.disco;

import unikl.disco.curves.ArrivalCurve;
import unikl.disco.numbers.NumFactory;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author Malte Schütze
 */
public class Utils {
    public static void dumpPseudoperiodicFunctionMatplotlib(PseudoPeriodicFunction f, long finalTime) {
        String xs = f.incrementTimeSteps.stream().map(Object::toString).collect(Collectors.joining(", "));
        String ys = f.incrementValues.stream().map(Object::toString).collect(Collectors.joining(", "));
        int timeIdx = Collections.binarySearch(f.incrementTimeSteps, f.periodBegin);
        if (timeIdx < 0) {
            timeIdx = -(timeIdx + 1);
        }
        if (timeIdx == f.incrementTimeSteps.size()) {
            xs += ", " + finalTime;
            ys += ", " + f.incrementValues.get(f.incrementValues.size() - 1);
        } else {
            long time = f.periodLength + f.incrementTimeSteps.get(timeIdx);
            StringBuilder xsBuilder = new StringBuilder(xs);
            StringBuilder ysBuilder = new StringBuilder(ys);
            int timeIdxOffset = 0;
            while (time <= finalTime) {
                xsBuilder.append(", ").append(time);
                ysBuilder.append(", ").append(f.getValue(time));

                timeIdxOffset += 1;
                timeIdxOffset %= (f.incrementTimeSteps.size() - timeIdx);
                long t = f.incrementTimeSteps.get(timeIdx + timeIdxOffset);
                long deltaT = (timeIdx + timeIdxOffset == 0) ? t : t - f.incrementTimeSteps.get(timeIdx + timeIdxOffset - 1);
                time += deltaT;
            }

            ys = ysBuilder.toString();
            xs = xsBuilder.toString();
        }

        System.out.println("# Pseudo-periodic function up to " + finalTime);
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
                xs.append(curve.getSegment(i + 1).getX().doubleValue());
                ys.append(curve.getSegment(i).f(curve.getSegment(i + 1).getX()).doubleValue());
            } else {
                xs.append(finalSegmentEnd);
                ys.append(curve.getSegment(i).f(NumFactory.create(finalSegmentEnd)).doubleValue());
            }
        }

        System.out.println("# Concave hull up to " + finalSegmentEnd);
        System.out.println("x = [" + xs + "]");
        System.out.println("y = [" + ys + "]");
        System.out.println("plt.plot(x, y)");
        System.out.println();
    }

    public static void dumpMaxTrafficMatplotlib(ProtocolGraph graph, long maxTime) {
        PseudoPeriodicFunction f = graph.approximateSubadditive(maxTime);
        String xs = f.incrementTimeSteps.stream().map(Object::toString).collect(Collectors.joining(", "));
        String ys = f.incrementValues.stream().map(Object::toString).collect(Collectors.joining(", "));

        System.out.println("# Actual traffic up to " + maxTime);
        System.out.println("x = [" + xs + "]");
        System.out.println("y = [" + ys + "]");
        System.out.println("plt.step(x, y, where='post')");
        System.out.println();
    }
}
