package org.networkcalculus.dnc.gsi_input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import org.apache.commons.lang3.time.DurationFormatUtils;

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


        System.out.println("Parsing graph at '" + args.path + "'");
        ProtocolGraph graph = new DotGraphParser(new FileInputStream(args.path), args).parse();
        System.out.println("Done (" + graph.getBlockCount() + " blocks)");

        int iterations = args.benchmark ? args.benchmarkIterations : 1;
        if (args.benchmark && args.benchmarkIterations <= 1) {
            System.err.println(args.benchmarkIterations + ": Invalid number of benchmark iterations (must be >= 2)");
            System.exit(1);
        }

        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long time = System.currentTimeMillis();

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

            long delta = System.currentTimeMillis() - time;
            times[i] = delta;
            if (args.verbose) {
                System.out.println("Finished in " + DurationFormatUtils.formatDurationHMS(delta));
            }
        }

        if (args.benchmark) {
            long total = 0;
            for (int i = 0; i < iterations; i++) {
                total += times[i];
            }

            double average = total / (double) iterations;
            System.out.println("Average: " + DurationFormatUtils.formatDurationHMS((long) average));

            double var = 0;
            for (int i = 0; i < iterations; i++) {
                var += Math.pow(times[i] - average, 2);
            }

            double stdDev = Math.sqrt(var / (iterations - 1));
            System.out.println("StdDev: " + DurationFormatUtils.formatDurationHMS((long) stdDev));
        }
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
            args.formatter.printPseudoperiodicFunction(graph, f, args.threshold > 0 ? 2 * threshold : f.periodBegin + 3 * f.periodLength);
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
            args.formatter.printPseudoperiodicFunction(graph, f, args.threshold > 0 ? 2 * args.threshold : f.periodBegin + 3 * f.periodLength);
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
            args.formatter.printPseudoperiodicFunction(graph, f, args.threshold > 0 ? 2 * args.threshold : f.periodBegin + 3 * f.periodLength);
        }
        args.formatter.printArrivalCurve(f.concaveHull(), args.threshold > 0 ? 2 * args.threshold : f.periodBegin + 3 * f.periodLength);
    }
}
