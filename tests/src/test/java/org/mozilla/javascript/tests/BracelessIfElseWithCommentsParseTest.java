package org.mozilla.javascript.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

/**
 * Tests that braceless if/else statements with inline comments parse correctly when {@code
 * setRecordingComments(true)} is enabled. Multiple consecutive comments between a braceless {@code
 * if} and its body used to cause a parse failure because {@code
 * getNextStatementAfterInlineComments()} only consumed a single comment.
 *
 * @see <a href="https://github.com/mozilla/rhino/issues/XXX">GitHub issue</a>
 */
public class BracelessIfElseWithCommentsParseTest {

    @Test
    public void testBracelessIfElseWithMultipleComments() throws IOException {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingComments(true);

        Parser parser = new Parser(env);
        String source;
        try (BufferedReader reader =
                new BufferedReader(
                        new FileReader("testsrc/jstests/braceless-if-else-with-comments.js"))) {
            source = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        // Parsing should not throw a syntax error
        AstRoot ast = parser.parse(source, "test", 1);
        Assert.assertNotNull(ast);

        // The fixture has 18 comments — verify they were all recorded
        Assert.assertEquals(18, ast.getComments().size());
    }

    @Test
    public void testMinimalTwoCommentRepro() {
        String source =
                "if (x)\n"
                        + "    // comment 1\n"
                        + "    // comment 2\n"
                        + "    doSomething();\n"
                        + "else\n"
                        + "    doSomethingElse();\n";

        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingComments(true);

        Parser parser = new Parser(env);
        AstRoot ast = parser.parse(source, "test", 1);
        Assert.assertNotNull(ast);
        Assert.assertEquals(2, ast.getComments().size());
    }

    @Test
    public void testSingleCommentStillWorks() {
        String source =
                "if (x)\n"
                        + "    // single comment\n"
                        + "    doSomething();\n"
                        + "else\n"
                        + "    doSomethingElse();\n";

        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingComments(true);

        Parser parser = new Parser(env);
        AstRoot ast = parser.parse(source, "test", 1);
        Assert.assertNotNull(ast);
        Assert.assertEquals(1, ast.getComments().size());
    }

    @Test
    public void testConsecutiveCommentsAreMergedInToSource() {
        String source =
                "if (x)\n"
                        + "    // comment 1\n"
                        + "    // comment 2\n"
                        + "    doSomething();\n"
                        + "else\n"
                        + "    doSomethingElse();\n";

        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingComments(true);

        Parser parser = new Parser(env);
        AstRoot ast = parser.parse(source, "test", 1);

        String expected =
                "if (x)     // comment 1\n"
                        + "// comment 2\n"
                        + "  doSomething();\n"
                        + "else \n"
                        + "  doSomethingElse();\n";
        Assert.assertEquals(expected, ast.toSource());
    }

    @Test
    public void testWithoutRecordingCommentsStillWorks() {
        String source =
                "if (x)\n"
                        + "    // comment 1\n"
                        + "    // comment 2\n"
                        + "    doSomething();\n"
                        + "else\n"
                        + "    doSomethingElse();\n";

        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingComments(false);

        Parser parser = new Parser(env);
        AstRoot ast = parser.parse(source, "test", 1);
        Assert.assertNotNull(ast);
    }
}
