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

    public void setValueAt(long time, int value) {
        if (incrementTimeSteps.isEmpty()) {
            incrementTimeSteps.add(time);
            incrementValues.add(value);
            return;
        }

        int lastIdx = incrementTimeSteps.size() - 1;
        if (time < incrementTimeSteps.get(lastIdx)) {
            throw new IllegalArgumentException("Tried to go back in time");
        }

        if (time == incrementTimeSteps.get(lastIdx)) {
            incrementValues.set(lastIdx, value);
            return;
        }

        if (incrementValues.get(incrementValues.size() - 1) == value) {
            return; // only store distinct times + values
        }

        incrementTimeSteps.add(time);
        incrementValues.add(value);
    }

    public int getValue(long time) {
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
}
