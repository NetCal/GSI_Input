package unikl.disco;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang.time.DurationFormatUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Malte Schütze
 */
public class Main {
    public static void main(String... argv) throws IOException {
        Args args = new Args();
        JCommander parser = new JCommander.Builder().addObject(args).build();
        try {
            parser.parse(argv);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            parser.usage();
            System.exit(1);
        }

        if (args.help) {
            parser.usage();
            System.exit(0);
        }

        File file = new File(args.path);
        if (!file.exists()) {
            System.err.println(args.path + ": No such path");
            System.exit(1);
        }

        if (!file.isFile()) {
            System.err.println(args.path + ": Not a path");
            System.exit(1);
        }

        long time = System.currentTimeMillis();

        ProtocolGraph graph = new DotGraphParser(new FileInputStream(args.path), args).parse();
        switch (args.heuristic) {
            case SUBADDITIVE:
                approximateSubadditive(args, graph);
                break;
            case LOOP:
                approximateLoop(args, graph);
                break;
            case RESCALE:
                approximateRescale(args, graph);
                break;
        }

        if (args.verbose) DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - time);
    }

    private static void approximateSubadditive(Args args, ProtocolGraph graph) {
        long threshold = args.threshold;
        if (threshold == 0) {
            threshold = graph.longestBlockLength() * 4;
        }

        System.out.println("Using a threshold of " + threshold);
        PseudoPeriodicFunction f = graph.approximateSubadditive(threshold);
        System.out.println("Approximation created");

        if (args.verbose) {
            args.formatter.printMaxTraffic(graph, args.threshold > 0 ? 2 * threshold : f.periodBegin + 3 * f.periodLength);
            args.formatter.printPseudoperiodicFunction(f, args.threshold > 0 ? 2 * threshold : f.periodBegin + 3 * f.periodLength);
        }
        args.formatter.printArrivalCurve(f.concaveHull(), args.threshold > 0 ? 2 * threshold : f.periodBegin + 3 * f.periodLength);
    }

    private static void approximateLoop(Args args, ProtocolGraph graph) {
        int numBlocks = args.numBlocks;
        if (numBlocks == 0) {
            long fit = 20_000_000_000L / graph.longestBlockLength();
            numBlocks = Math.max(1, (int) Math.min(8, fit));
        }

        System.out.println("Using " + numBlocks + " consecutive blocks");
        FullyConnectedProtocolGraph fcGraph = graph.fullyConnected(numBlocks);
        System.out.println("Fully connected model created");
        PseudoPeriodicFunction f = fcGraph.approximateMostEfficientLoop();
        System.out.println("Approximation created");
        if (args.verbose) {
            args.formatter.printMaxTraffic(graph, args.threshold > 0 ? 2 * args.threshold :  f.periodBegin + 3 * f.periodLength);
            args.formatter.printPseudoperiodicFunction(f, args.threshold > 0 ? 2 * args.threshold : f.periodBegin + 3 * f.periodLength);
        }
        args.formatter.printArrivalCurve(f.concaveHull(), args.threshold > 0 ? 2 * args.threshold : f.periodBegin + 3 * f.periodLength);
    }

    private static void approximateRescale(Args args, ProtocolGraph graph) {
        int numBlocks = args.numBlocks;
        if (numBlocks == 0) {
            long fit = 20_000_000_000L / graph.shortestBlockLength();
            numBlocks = Math.max(1, (int) Math.min(8, fit));
        }

        System.out.println("Using " + numBlocks + " consecutive blocks");
        FullyConnectedRescaledProtocolGraph rescale = graph.fullyConnected(numBlocks).rescale();
        System.out.println("Rescaled model created");
        PseudoPeriodicFunction f = rescale.approximateTightestLoop();
        System.out.println("Approximation created");
        if (args.verbose) {
            args.formatter.printMaxTraffic(graph, args.threshold > 0 ? 2 * args.threshold :  f.periodBegin + 3 * f.periodLength);
            args.formatter.printPseudoperiodicFunction(f, args.threshold > 0 ? 2 * args.threshold : f.periodBegin + 3 * f.periodLength);
        }
        args.formatter.printArrivalCurve(f.concaveHull(), args.threshold > 0 ? 2 * args.threshold : f.periodBegin + 3 * f.periodLength);
    }
}
