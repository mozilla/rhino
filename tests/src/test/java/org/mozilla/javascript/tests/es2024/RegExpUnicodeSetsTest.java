package org.mozilla.javascript.tests.es2024;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ES2024 RegExp v flag (unicodeSets) */
public class RegExpUnicodeSetsTest {

    // ES2024 unicodeSets ('v' flag) set operations tests

    @Test
    public void testVFlagSetSubtraction() {
        String script =
                "var re = /[a-z--[aeiou]]/v;"
                        + "re.test('b') && re.test('z') && !re.test('a') && !re.test('e')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagSetIntersection() {
        String script =
                "var re = /[a-z&&[a-m]]/v;"
                        + "re.test('a') && re.test('m') && !re.test('n') && !re.test('z')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagMultipleOperations() {
        String script =
                "var re = /[a-z--[aeiou]--[xyz]]/v;"
                        + "re.test('b') && !re.test('a') && !re.test('x')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagWithDigits() {
        String script =
                "var re = /[\\d--[5-9]]/v;" + "re.test('0') && re.test('4') && !re.test('5')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagComplexExpression() {
        String script =
                "var re = /[a-z&&[^aeiou]]/v;" // Letters that are not vowels (consonants)
                        + "re.test('b') && re.test('z') && !re.test('a') && !re.test('e')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagSetOperationWithoutVFlagFails() {
        String script =
                "try {"
                        + "  eval('var re = /[a--b]/;');"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof SyntaxError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // String literal tests using \q{} syntax

    @Test
    public void testVFlagStringLiteralBasic() {
        String script =
                "var re = /[\\q{abc}]/v;" + "re.test('abc') && !re.test('ab') && !re.test('a')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagStringLiteralWithCharacter() {
        String script =
                "var re = /[\\q{foo}bar]/v;"
                        + "re.test('foo') && re.test('b') && re.test('a') && re.test('r')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagStringLiteralMultipleAlternatives() {
        String script =
                "var re = /[\\q{abc|def}]/v;"
                        + "re.test('abc') && re.test('def') && !re.test('ab')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testCharClassEscapeAsOperandIntersection() {
        // Test \d as operand in intersection
        String script = "var re = /[[0-9]&&\\d]/v; re.test('5') && re.test('0') && !re.test('a')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testCharClassEscapeAsOperandSubtraction() {
        // Test \d as operand in subtraction
        String script =
                "var re = /[\\w--\\d]/v; re.test('a') && re.test('_') && !re.test('5')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testMultipleCharClassEscapeOperands() {
        // Test \w && \d (should match digits only)
        String script =
                "var re = /[\\w&&\\d]/v; re.test('0') && re.test('9') && !re.test('a')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testNonWordCharClassEscapeOperand() {
        // Test \W (non-word) intersection with specific punctuation
        String script =
                "var re = /[\\W&&[!@#]]/v; re.test('!') && re.test('@') && !re.test('$')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testWhitespaceCharClassEscapeOperand() {
        // Test \s intersection
        String script =
                "var re = /[\\s&&[ \\t]]/v; re.test(' ') && re.test('\\t') && !re.test('\\n')";
        Utils.assertWithAllModes(true, script);
    }
}
