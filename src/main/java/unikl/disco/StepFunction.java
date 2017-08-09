package unikl.disco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Malte Schütze
 */
public class StepFunction {
    private List<Long> incrementTimeSteps = new ArrayList<>();
    private List<Integer> incrementValues = new ArrayList<>();
    private long validUpTo = 0;

    public void setValueAt(long time, int value) {
        if (incrementTimeSteps.isEmpty()) {
            incrementTimeSteps.add(time);
            incrementValues.add(value);
            validUpTo = time;
            return;
        }

        if (time < validUpTo) {
            throw new IllegalArgumentException("Tried to go back in time");
        }

        int lastIdx = incrementTimeSteps.size() - 1;
        if (time == incrementTimeSteps.get(lastIdx)) {
            incrementValues.set(lastIdx, value);
            return;
        }

        validUpTo = time;

        if (incrementValues.get(incrementValues.size() - 1) == value) {
            return; // only store distinct times + values
        }

        incrementTimeSteps.add(time);
        incrementValues.add(value);
    }

    /**
     * The maximum point for which this function was defined
     * @return
     */
    public long getValidUpTo() {
        return validUpTo;
    }

    public int getValue(long time) {
        if (time > validUpTo) throw new IllegalArgumentException("Function not defined to " + time + " (valid up to " + validUpTo + ")");

        int idx = Collections.binarySearch(incrementTimeSteps, time);
        if (idx >= 0) {
            return incrementValues.get(idx);
        } else {
            idx = -(idx + 1);
            if (idx == 0) {
                return 0;
            } else {
                return incrementValues.get(idx - 1);
            }
        }

    }

    public List<Integer> getIncrementValues() {
        return Collections.unmodifiableList(incrementValues);
    }

    public List<Long> getIncrementTimeSteps() {
        return Collections.unmodifiableList(incrementTimeSteps);
    }

    public int maximumInterval(long time, long latestOffset) {
        if (time < 0) throw new IllegalArgumentException("Negative interval");

        int max = 0;
        int idx = 0;
        while (idx < incrementTimeSteps.size() && incrementTimeSteps.get(idx) <= latestOffset) {
            int trafficInPrefix = idx == 0 ? 0 : incrementValues.get(idx - 1);
            long totalTime = incrementTimeSteps.get(idx) + time - 1;
            int intervalTraffic = getValue(totalTime) - trafficInPrefix;
            if (intervalTraffic > max) {
                max = intervalTraffic;
            }

            idx++;
        }

        return max;
    }
}
