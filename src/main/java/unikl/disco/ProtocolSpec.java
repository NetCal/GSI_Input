package unikl.disco;

import unikl.disco.curves.ArrivalCurve;
import unikl.disco.curves.Curve;
import unikl.disco.curves.LinearSegment;
import unikl.disco.numbers.Num;
import unikl.disco.numbers.NumFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Malte Schütze
 */
public class ProtocolSpec {
    // A list of all items in the spec
    // Ordered roughly chronological: If spec item a happens chronologically after spec item b, index(a) > index(b)
    private List<Message> items = new ArrayList<>();
    final int cycleLength;

    private Map<Integer, Integer> intervalTrafficCache = new HashMap<>();

    public ProtocolSpec(int cycleLength) {
        this.cycleLength = cycleLength;
    }

    public ArrivalCurve createArrivalCurve() {
        Curve curve = new ArrivalCurve(0);

        curve.addSegment(LinearSegment.createZeroSegment());
        int lastTraffic = 0;
        for (int intervalLen = 0; intervalLen < cycleLength; intervalLen++) {
            int traffic = maxTrafficInInterval(intervalLen);
            LinearSegment segment = new LinearSegment(intervalLen - 1, lastTraffic, traffic - lastTraffic);
            curve.addSegment(segment);

            lastTraffic = traffic;
        }

        return new ArrivalCurve(curve);
    }

    private Num slope(int from, int to) {
        return NumFactory.create(maxTrafficInInterval(to) - maxTrafficInInterval(from), to - from);
    }

    private List<Integer> getInitialConcaveHullSegments(int segmentEnd, Num minSlope) {
        if (segmentEnd == 1) {
            ArrayList<Integer> result = new ArrayList();
            result.add(0); // Initial segment starts at 0
            return result;
        }

        int segmentStart = segmentEnd - 1;
        Num currentSlope = slope(segmentStart, segmentEnd);
        Num maxSlope = currentSlope;

        // Once we fall below min slope, we know we can't find a concave hull with the current configuration
        while (currentSlope.geq(minSlope)) {

            // Can't grow in slope without intercepting curve
            if (currentSlope.leq(maxSlope)) {
                List<Integer> segments = getInitialConcaveHullSegments(segmentStart, currentSlope);
                if (segments != null) {
                    segments.add(segmentStart);
                }
            }

            segmentStart -= 1;
            if (currentSlope.leq(maxSlope)) {
                maxSlope = currentSlope;
            }
            currentSlope = slope(segmentStart, segmentEnd);
        }

        return null;
    }

    public void add(Message message) {
        if (message.transmissionTime < 0 || message.transmissionTime >= cycleLength) {
            throw new IllegalArgumentException("Transmission time not in interval [0, cycle-length - 1]");
        }
        if (!intervalTrafficCache.isEmpty()) {
            throw new IllegalStateException("Adding a message after the traffic cache has been populated is not supported");
        }
        this.items.add(message);
    }

    int maxTraffic(int toTime) {
        if (items.isEmpty()) return 0;

        Message first = items.get(0);
        int cycles = toTime / cycleLength;
        int remaining = toTime % cycleLength;

        if (cycles > 0) {
            return first.maxTraffic(cycleLength) * cycles + first.maxTraffic(remaining);
        } else {
            return first.maxTraffic(remaining);
        }
    }

    int maxTrafficInInterval(int intervalLength) {
        if (intervalLength >= 2 * cycleLength) {
            int fullCycles = (intervalLength / cycleLength) - 1;
            int remainingIntervalLength = intervalLength - (fullCycles * cycleLength);

            return maxTrafficInInterval(remainingIntervalLength) + fullCycles * maxTraffic(cycleLength);
        }

        if (intervalTrafficCache.containsKey(intervalLength)) {
            return intervalTrafficCache.get(intervalLength);
        }

        int maxTraffic = 0;
        for (Message msg : items) {
            int traffic = msg.maxTraffic(msg.transmissionTime + intervalLength);
            if (traffic > maxTraffic) {
                maxTraffic = traffic;
            }
        }

        intervalTrafficCache.put(intervalLength, maxTraffic);
        return maxTraffic;
    }
}
