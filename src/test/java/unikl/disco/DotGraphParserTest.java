package unikl.disco;

import junit.framework.TestCase;

/**
 * @author Malte Schütze
 */
public class DotGraphParserTest extends TestCase {

    private DotGraphParser parser;

    @Override
    public void setUp() {
        parser = new DotGraphParser(DotGraphParser.class.getResourceAsStream("/cryring_fictional.dot"));
    }

    public void testParse() throws Exception {
        parser.parse();
    }

}