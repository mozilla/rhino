package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ast.AstRoot;

/**
 * Tests for specific parser features targeted at IDE environments, namely the ability to warn about
 * missing semicolons for JavaScript programmers who follow that style.
 */
public class ParserIDETest {

    private static CompilerEnvirons environment;

    @BeforeClass
    public static void init() {
        environment = CompilerEnvirons.ideEnvirons();
    }

    private AstRoot parse(String script, String[] errors, String[] warnings) {
        return ParserTest.parse(script, errors, warnings, false, environment);
    }

    private AstRoot parse(String script) {
        return ParserTest.parse(script, new String[] {}, new String[] {}, false, environment);
    }

    @Test
    public void unterminatedRE() {
        String[] errors = {"unterminated regular expression literal"};
        String[] warnings = {"missing ; after statement"};

        parse("/", errors, warnings);
    }

    @Test
    public void missingSemiBeforeComment() {
        String[] errors = {};
        String[] warnings = {"missing ; after statement", "missing ; after statement"};

        parse("var a = 1\n/** a */ var b = 2", errors, warnings);
    }

    @Test
    public void missingSemiBeforeComment2() {
        String[] errors = {};
        String[] warnings = {"missing ; after statement", "missing ; after statement"};

        parse("var a = 1\n/** a */\n var b = 2", errors, warnings);
    }

    @Test
    public void missingSemiBeforeComment3() {
        String[] errors = {};
        String[] warnings = {"missing ; after statement", "missing ; after statement"};

        parse("var a = 1\n/** a */\n /** b */ var b = 2", errors, warnings);
    }

    @Test
    public void warnTrailingComma() {
        String[] errors = {};
        String[] warnings = {"Trailing comma is not legal in an ECMA-262 object initializer"};

        parse("var o = {a: 'foo', b: 'bar',};", errors, warnings);
    }

    @Test
    public void warnTrailingArrayComma() {
        String[] errors = {};
        String[] warnings = {"Trailing comma is not legal in an ECMA-262 object initializer"};

        parse("var a = [1, 2, 3,];", errors, warnings);
    }

    @Test
    public void newlineAndCommentsFunction() {
        AstRoot root = parse("f('1234', // Before\n'2345' // Second arg\n);");
        assertNotNull(root.getComments());
        assertEquals(2, root.getComments().size());
    }

    @Test
    public void newlineAndCommentsFunction2() {
        AstRoot root = parse("f('1234', // Before\n// Middle\n'2345' // Second arg\n);");
        assertNotNull(root.getComments());
        assertEquals(3, root.getComments().size());
    }

    @Test
    public void newlineAndCommentsFunction3() {
        AstRoot root =
                parse("f('1234', // Before\n// Middle\n// Middler\n'2345' // Second arg\n);");
        assertNotNull(root.getComments());
        assertEquals(4, root.getComments().size());
    }

    @Test
    public void objectLiteralComments() {
        AstRoot root =
                parse(
                        // "var o = {foo: '1234', // Before\nbar: '2345' // Second arg};\n);");
                        "var o = {foo: 1 // One\n, // Two\n bar: // bar\n2 // Two\n};");
        assertNotNull(root.getComments());
        assertEquals(4, root.getComments().size());
    }
}
