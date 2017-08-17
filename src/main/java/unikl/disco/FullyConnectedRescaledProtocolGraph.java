package unikl.disco;

/**
 * @author Malte Schütze
 */
public class FullyConnectedRescaledProtocolGraph extends ProtocolGraph {

    public PseudoPeriodicFunction approximateTightestLoop() {
        long shortestBlockLen = shortestBlockLength();
        PseudoPeriodicFunction function = new PseudoPeriodicFunction(2 * shortestBlockLen, shortestBlockLen, highestBlockTraffic());

        long time = 0;
        int value = 0;
        while (time < 2 * shortestBlockLen) {
            function.setValueAt(time, value);
            time = firstTimeExceeding(value);
            value = maxTraffic(time);
        }

        for (time = shortestBlockLen; time < 2 * shortestBlockLen; time++) {
            value = highestBlockTraffic() + divideTrafficBetweenPrefixAndSuffix(time);
            function.setValueAt(shortestBlockLen + time, value);
        }

        return function;
    }

    private int divideTrafficBetweenPrefixAndSuffix(long time) {
        long inPrefix = 0;
        int inPrefixTraffic = 0;
        int maxTraffic = 0;
        while (inPrefix < time) {
            maxTraffic = Math.max(maxTraffic, maxSuffix(time - inPrefix) + inPrefixTraffic);
            inPrefix = firstTimeExceeding(inPrefixTraffic);
            inPrefixTraffic = maxPrefix(time);
        }

        return maxTraffic;
    }
}
