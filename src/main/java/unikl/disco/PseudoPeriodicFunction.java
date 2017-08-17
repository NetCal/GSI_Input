package unikl.disco;

import unikl.disco.curves.ArrivalCurve;
import unikl.disco.curves.Curve;
import unikl.disco.curves.LinearSegment;
import unikl.disco.numbers.Num;
import unikl.disco.numbers.NumFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Malte Schütze
 */
public class PseudoPeriodicFunction {

    public final long periodBegin;
    public final long periodLength;
    public final double periodIncrement;
    private StepFunction initialPart = new StepFunction();
    // Readonly view to initalPart internal repr.
    public final List<Long> incrementTimeSteps = initialPart.getIncrementTimeSteps();
    public final List<Double> incrementValues = initialPart.getIncrementValues();

    public PseudoPeriodicFunction(long periodBegin, long periodLength, double periodIncrement) {
        this.periodBegin = periodBegin;
        this.periodLength = periodLength;
        this.periodIncrement = periodIncrement;
    }

    public void setValueAt(long time, double value) {
        initialPart.setValueAt(time, value);
    }

    public double getValue(long time) {
        if (time < periodBegin + periodLength) {
            return initialPart.getValue(time);
        }

        long repetitions = (time - periodBegin) / periodLength;
        long leftover = time - (repetitions * periodLength);

        return getValue(leftover) + repetitions * periodIncrement;
    }

    public ArrivalCurve concaveHull() {
        Num lastSegmentGrad = rational(periodIncrement, periodLength);

        // Due to time discretization, we need to assume that if a packet arrived at timestep `n`, it actually
        // arrived at `(n-1) + epsilon`
        // We therefore set the x-offset for each segment to n-1 and mark the segment as open to the left

        // First find the offset point, which is the highest point relative to the final slope, i.e. the point maximizing
        // f(x) - x*(increment/length)
        // This guarantees that the last segment will always be above the curve
        int lastSegmentStartIdx = 0;
        Num maxVerticalOffset = NumFactory.createZero();

        // Note that we only need to check the increment points, every other point will be closer to the long-term line
        for (int i = 0; i < incrementTimeSteps.size(); i++) {
            long time = incrementTimeSteps.get(i);
            if (time > 0) time = time - 1; // Compensate for time discretization, see above

            // f(x) - x*(increment/length) = (length*f(x) - x*increment) / length
            Num offset = rational(periodLength * incrementValues.get(i) - periodIncrement * time, periodLength);
            // If there a multiple possible points, pick the earliest
            if (offset.gt(maxVerticalOffset)) {
                maxVerticalOffset = offset;
                lastSegmentStartIdx = i;
            }
        }

        List<Integer> segmentPoints = getConcaveHullSegmentPoints(lastSegmentStartIdx, lastSegmentGrad);
        if (segmentPoints == null) {
            System.out.println("Unable to find a concave hull, using a very rough approximation");
            Curve curve = new ArrivalCurve();
            curve.addSegment(LinearSegment.createZeroSegment());
            curve.addSegment(new LinearSegment(NumFactory.createZero(), maxVerticalOffset, lastSegmentGrad, true));
            return new ArrivalCurve(curve);
        }

        Curve curve = new ArrivalCurve();
        for (int i = 0; i < segmentPoints.size(); i++) {
            int fromIdx = segmentPoints.get(i);
            int toIdx = i == segmentPoints.size() - 1 ? lastSegmentStartIdx : segmentPoints.get(i + 1);

            if (incrementTimeSteps.get(toIdx) == 1) {
                continue; // This segment would be mushed into a zero-length segment
            }

            Num slope = slope(fromIdx, toIdx);

            long startTime = incrementTimeSteps.get(fromIdx);
            Num time = NumFactory.create(startTime == 0 ? 0 : startTime - 1);
            Num value = NumFactory.create(incrementValues.get(fromIdx));
            curve.addSegment(new LinearSegment(time, value, slope, true));
        }

        Num lastSegmentTime = NumFactory.create(incrementTimeSteps.get(lastSegmentStartIdx) - 1);
        Num lastSegmentValue = NumFactory.create(incrementValues.get(lastSegmentStartIdx));
        curve.addSegment(new LinearSegment(lastSegmentTime, lastSegmentValue, lastSegmentGrad, true));

        return new ArrivalCurve(curve);
    }

    /**
     * Calculate the concave hull of the function from 0 to <code>time[toIdx]</code>.
     *
     * @param toIdx    Part of the function up to which the hull should be calculated
     * @param minSlope Minimum slope that may be taken. Concavity constraints mean that each segment must have
     *                 monotonically increasing slopes
     * @return A list of indices into <code>timeSteps</code> which are the start points of each segment.
     */
    private List<Integer> getConcaveHullSegmentPoints(int toIdx, Num minSlope) {


        if (toIdx == 0) {
            if (incrementTimeSteps.isEmpty() || incrementTimeSteps.get(toIdx) == 0) {
                return new ArrayList<>();
            }
        }

        int segmentStart = toIdx - 1;
        Num currentSlope = slope(segmentStart, toIdx);
        Num maxSlope = currentSlope; // Can't grow in slope without intercepting actual function

        // We want to maximize currentSlope without rising above maxSlope
        // therefore with each step currentSlope is decreasing
        // when we fall below minSlope, there is no concave hull
        while (currentSlope.geq(minSlope)) {
            // Check for function interception
            if (currentSlope.leq(maxSlope)) {
                List<Integer> segments = getConcaveHullSegmentPoints(segmentStart, currentSlope);
                if (segments != null) {
                    // Valid concave hull found (and it's optimal for our current configuration)
                    segments.add(segmentStart);
                    return segments;
                }
            }

            // function intercepted or no concave hull found
            // try to flatten our concave hull a bit
            segmentStart -= 1;
            if (segmentStart < 0) return null;
            if (currentSlope.leq(maxSlope)) {
                maxSlope = currentSlope;
            }
            currentSlope = slope(segmentStart, toIdx);
        }

        return null; // No concave hull found
    }

    private Num rational(double num, long den) {
        return NumFactory.create(num / den);
    }

    private Num slope(int first, int second) {
        long time = incrementTimeSteps.get(second) - incrementTimeSteps.get(first);
        double value = incrementValues.get(second) - incrementValues.get(first);

        return rational(value, time);
    }
}
