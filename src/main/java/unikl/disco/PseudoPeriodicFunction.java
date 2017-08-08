package unikl.disco;

import unikl.disco.curves.ArrivalCurve;
import unikl.disco.curves.Curve;
import unikl.disco.curves.LinearSegment;
import unikl.disco.numbers.Num;
import unikl.disco.numbers.implementations.RationalBigInt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Malte Schütze
 */
public class PseudoPeriodicFunction {

    private StepFunction initialPart = new StepFunction();
    // Readonly view to initalPart internal repr.
    private List<Long> incrementTimeSteps = initialPart.getIncrementTimeSteps();
    private List<Integer> incrementValues = initialPart.getIncrementValues();

    private final long periodBegin;
    private final long periodLength;
    private final int periodIncrement;

    public PseudoPeriodicFunction(long periodBegin, long periodLength, int periodIncrement) {
        this.periodBegin = periodBegin;
        this.periodLength = periodLength;
        this.periodIncrement = periodIncrement;
    }

    public void setValueAt(long time, int value) {
        initialPart.setValueAt(time, value);
    }

    public long getValue(long time) {
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
        Num maxVerticalOffset = rational(0, 1);

        List<Long> incrementTimeSteps = initialPart.getIncrementTimeSteps();
        List<Integer> incrementValues = initialPart.getIncrementValues();

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
            curve.addSegment(new LinearSegment(RationalBigInt.createZero(), maxVerticalOffset, lastSegmentGrad, true));
            return new ArrivalCurve(curve);
        }

        Curve curve = new ArrivalCurve();
        curve.addSegment(LinearSegment.createZeroSegment());
        for (int i = 0; i < segmentPoints.size(); i++) {
            int fromIdx = segmentPoints.get(i);
            int toIdx = i == segmentPoints.size() - 1 ? lastSegmentStartIdx : segmentPoints.get(i+1);

            Num slope = slope(fromIdx, toIdx);
            Num time = rational(incrementTimeSteps.get(fromIdx) - 1, 1);
            Num value = rational(incrementValues.get(fromIdx), 1);
            curve.addSegment(new LinearSegment(time, value, slope, true));
        }

        Num lastSegmentTime = rational(incrementTimeSteps.get(lastSegmentStartIdx) - 1, 1);
        Num lastSegmentValue = rational(incrementValues.get(lastSegmentStartIdx), 1);
        curve.addSegment(new LinearSegment(lastSegmentTime, lastSegmentValue, lastSegmentGrad, true));

        return new ArrivalCurve(curve);
    }

    /**
     * Calculate the concave hull of the function from 0 to <code>time[toIdx]</code>.
     * @param toIdx Part of the function up to which the hull should be calculated
     * @param minSlope Minimum slope that may be taken. Concavity constraints mean that each segment must have
     *                 monotonically increasing slopes
     * @return A list of indices into <code>timeSteps</code> which are the start points of each segment. Idx 0 is only
     *         include if the time at idx 0 is not zero
     */
    private List<Integer> getConcaveHullSegmentPoints(int toIdx, Num minSlope) {


        if (toIdx == 0) {
            if (incrementTimeSteps.isEmpty() || incrementTimeSteps.get(toIdx) == 0) {
                return new ArrayList<>();
            }

            ArrayList<Integer> result = new ArrayList<>();
            result.add(0);
            return result;
        }

        int segmentStart = toIdx -1;
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
            toIdx -= 1;
            if (currentSlope.leq(maxSlope)) {
                maxSlope = currentSlope;
            }
            currentSlope = slope(segmentStart, toIdx);
        }

        return null; // No concave hull found
    }

    private Num rational(long num, long den) {
        return new RationalBigInt(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    private Num slope(int first, int second) {
        long time = incrementTimeSteps.get(second) - incrementTimeSteps.get(first);
        long value = incrementValues.get(second) - incrementValues.get(first);

        return rational(value, time);
    }
}
