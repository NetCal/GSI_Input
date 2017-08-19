package unikl.disco;

/**
 * @author Malte Schütze
 */
public class FullyConnectedRescaledProtocolGraph extends ProtocolGraph {

    public PseudoPeriodicFunction approximateTightestLoop() {
        long shortestBlockLen = shortestBlockLength();
        int highestBlockTraffic = highestBlockTraffic();
        PseudoPeriodicFunction function = new PseudoPeriodicFunction(2 * shortestBlockLen, shortestBlockLen, highestBlockTraffic);

        long time = 0;
        double value = 0;
        while (time < 2 * shortestBlockLen) {
            System.out.println("[1] " + time + " / " + (3 * shortestBlockLen));
            function.setValueAt(time, value);
            time = firstTimeExceeding(value);
            value = maxTraffic(time);
        }

        time = shortestBlockLen;
        while (time < 2 * shortestBlockLen) {
            System.out.println("[2] " + (time + shortestBlockLen) + " / " + (3 * shortestBlockLen));
            value = divideTrafficBetweenPrefixAndSuffix(time);
            function.setValueAt(shortestBlockLen + time, highestBlockTraffic + value);
            time = nextStepForDividedTraffic(value);
        }

        function.setValueAt(3 * shortestBlockLen, highestBlockTraffic + divideTrafficBetweenPrefixAndSuffix(2 * shortestBlockLen));
        return function;
    }

    public long nextStepForDividedTraffic(double valueToExceed) {
        long minTime = Math.min(firstTimeExceedingInSuffix(valueToExceed), firstTimeExceedingInPrefix(valueToExceed));
        double inPrefixTraffic = 0;
        while (inPrefixTraffic <= valueToExceed){
            long inPrefixTime = firstTimeExceedingInPrefix(inPrefixTraffic);
            inPrefixTraffic = maxPrefix(inPrefixTime);
            minTime = Math.min(minTime, inPrefixTime + firstTimeExceedingInSuffix(valueToExceed - inPrefixTraffic));
        }

        return minTime;
    }

    public double divideTrafficBetweenPrefixAndSuffix(long time) {
        long inPrefixTime = 0;
        double inPrefixTraffic = 0;
        double maxTraffic = 0;
        while (inPrefixTime < time) {
            double value = maxSuffix(time - inPrefixTime) + inPrefixTraffic;
            if (value > maxTraffic) {
                maxTraffic = value;
            }
            inPrefixTime = firstTimeExceedingInPrefix(inPrefixTraffic);
            inPrefixTraffic = maxPrefix(inPrefixTime);
        }

        return maxTraffic;
    }
}
