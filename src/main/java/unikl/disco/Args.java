package unikl.disco;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * @author Malte Schütze
 */
public class Args {
    @Parameter(names = {"-h", "--help"}, description = "Display this help", help = true)
    public boolean help;

    @Parameter(description = "Input path", required = true)
    public String path;

    @Parameter(names = {"-f", "--format"}, description = "How to format output", converter = OutputFormatterConverter.class)
    public OutputFormatter formatter = new DiscoDncFormatter();

    @Parameter(names = {"-v", "--verbose"}, description = "Include \"real\" arrival function and pseudoperiodic approximation in output")
    public boolean verbose;

    @Parameter(names = {"-H", "--heuristic"}, description = "The heuristic to use")
    public Heuristic heuristic = Heuristic.SUBADDITIVE;

    @Parameter(names = {"-k", "--threshold"}, description = "Threshold value for subadditive approximation. 0 for auto")
    public long threshold;

    @Parameter(names = {"-n", "--numblocks"}, description = "Number of sequential blocks for building fully-connected model. 0 for auto")
    public int numBlocks;

    @Parameter(names = {"-b", "--benchmark"}, description = "Run program in benchmark mode")
    public boolean benchmark;

    @Parameter(names = {"-B", "--bench-iters"}, description = "How many iterations for benchmark")
    public int benchmarkIterations = 5;

    public enum Heuristic {
        SUBADDITIVE,
        RESCALE,
        LOOP
    }

    private static class OutputFormatterConverter implements IStringConverter<OutputFormatter> {

        @Override
        public OutputFormatter convert(String s) {
            switch (s.toLowerCase()) {
                case "discodnc": return new DiscoDncFormatter();
                case "matplotlib": return new MatplotlibOutputFormatter();
                default: throw new ParameterException("Unknown parameter '" + s + "' for 'format': Options are 'discodnc' or 'matplotlib'");
            }
        }
    }
}
