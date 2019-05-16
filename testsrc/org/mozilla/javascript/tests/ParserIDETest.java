package org.mozilla.javascript.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ast.AstRoot;

/**
 * Tests for specific parser features targeted at IDE environments, namely the ability
 * to warn about missing semicolons for JavaScript programmers who follow that style.
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

  @Test
  public void testUnterminatedRE() {
    String[] errors = {"unterminated regular expression literal"};
    String[] warnings = {"missing ; after statement"};

    parse("/", errors, warnings);
  }

  @Test
  public void testMissingSemiBeforeComment() {
    String[] errors = {};
    String[] warnings = {
        "missing ; after statement",
        "missing ; after statement"
    };

    parse("var a = 1\n/** a */ var b = 2", errors, warnings);
  }

  @Test
  public void testMissingSemiBeforeComment2() {
    String[] errors = {};
    String[] warnings = {
        "missing ; after statement",
        "missing ; after statement"
    };

    parse("var a = 1\n/** a */\n var b = 2", errors, warnings);
  }

  @Test
  public void testMissingSemiBeforeComment3() {
    String[] errors = {};
    String[] warnings = {
        "missing ; after statement",
        "missing ; after statement"
    };

    parse("var a = 1\n/** a */\n /** b */ var b = 2", errors, warnings);
  }

  @Test
  public void testWarnTrailingComma() {
    String[] errors = {};
    String[] warnings = {
        "Trailing comma is not legal in an ECMA-262 object initializer"
    };

    parse("var o = {a: 'foo', b: 'bar',};", errors, warnings);
  }

  @Test
  public void testWarnTrailingArrayComma() {
    String[] errors = {};
    String[] warnings = {
        "Trailing comma is not legal in an ECMA-262 object initializer"
    };

    parse("var a = [1, 2, 3,];", errors, warnings);
  }
}
