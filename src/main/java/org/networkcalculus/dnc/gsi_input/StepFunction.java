package org.networkcalculus.dnc.gsi_input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Malte Schütze
 */
public class StepFunction {
    private List<Long> incrementTimeSteps = new ArrayList<>();
    private List<Double> incrementValues = new ArrayList<>();
    private long validUpTo = 0;

    public void setValueAt(long time, double value) {
        if (incrementTimeSteps.isEmpty()) {
            incrementTimeSteps.add(time);
            incrementValues.add(value);
            validUpTo = time;
            return;
        }

        if (time < validUpTo) {
            throw new IllegalArgumentException("Tried to go back in time");
        }

        if (value < maximumValue()) {
            throw new IllegalArgumentException("Step function must be monotonic");
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
     *
     * @return
     */
    public long getValidUpTo() {
        return validUpTo;
    }

    public double getValue(long time) {
        if (time > validUpTo)
            throw new IllegalArgumentException("Function not defined to " + time + " (valid up to " + validUpTo + ")");

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

    public List<Double> getIncrementValues() {
        return Collections.unmodifiableList(incrementValues);
    }

    public List<Long> getIncrementTimeSteps() {
        return Collections.unmodifiableList(incrementTimeSteps);
    }

    /**
     * Retrieve the maximum traffic generated in any interval of length `time` where the start time does not exceed
     * `latestOffset`
     *
     * @param time
     * @param latestOffset
     * @return
     */
    public double maximumInterval(long time, long latestOffset) {
        if (time < 0) throw new IllegalArgumentException("Negative interval");

        double max = 0;
        int idx = 0;
        while (idx < incrementTimeSteps.size() && incrementTimeSteps.get(idx) <= latestOffset) {
            double trafficInPrefix = idx == 0 ? 0 : incrementValues.get(idx - 1);
            long totalTime = incrementTimeSteps.get(idx) + time - 1;
            double intervalTraffic = getValue(totalTime) - trafficInPrefix;
            if (intervalTraffic > max) {
                max = intervalTraffic;
            }

            idx++;
        }

        return max;
    }

    /**
     * Retrieve the time of the last value change in the function
     */
    public long lastStepTime() {
        if (incrementTimeSteps.isEmpty()) {
            throw new IllegalStateException("No step in function");
        }
        return incrementTimeSteps.get(incrementTimeSteps.size() - 1);
    }

    /**
     * Retrieve the maximum value of the function
     */
    public double maximumValue() {
        if (incrementValues.isEmpty()) {
            throw new IllegalStateException("No step in function");
        }
        return incrementValues.get(incrementValues.size() - 1);
    }

    /**
     * Find the first time step at which the function increments <emph>after</emph> <code>time</code>
     *
     * @param time Time threshold
     */
    public long nextIncrementTimeAfter(long time) {
        if (time >= lastStepTime()) {
            throw new IllegalArgumentException("No increment time after " + time + " (last step at " + lastStepTime() + ")");
        }

        int idx = Collections.binarySearch(incrementTimeSteps, time);
        if (idx >= 0) {
            // We want the next step /after/ the specified time
            // So if the specified time is exactly at a step, we pick the next one
            return incrementTimeSteps.get(idx + 1);
        } else {
            idx = -(idx + 1);
            return incrementTimeSteps.get(idx);
        }
    }

    /**
     * Find the first time step at which the function (strictly) exceeds <code>value</code>
     *
     * @param value Value threshold
     */
    public long firstTimeExceeding(double value) {
        if (value >= maximumValue()) {
            throw new IllegalArgumentException("No value above " + value + " (fn max value: " + maximumValue() + ")");
        }

        int idx = Collections.binarySearch(incrementValues, value);
        if (idx >= 0) {
            return incrementTimeSteps.get(idx + 1);
        } else {
            idx = -(idx + 1);
            return incrementTimeSteps.get(idx);
        }
    }
}
