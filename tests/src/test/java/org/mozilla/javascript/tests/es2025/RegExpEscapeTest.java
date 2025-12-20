package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ES2025 RegExp.escape() static method */
public class RegExpEscapeTest {

    // Basic syntax character escaping

    @Test
    public void testEscapeBasicSyntaxChars() {
        // All RegExp syntax characters should be escaped
        String script =
                "RegExp.escape('^') === '\\\\^' && "
                        + "RegExp.escape('$') === '\\\\$' && "
                        + "RegExp.escape('\\\\') === '\\\\\\\\' && "
                        + "RegExp.escape('.') === '\\\\.' && "
                        + "RegExp.escape('*') === '\\\\*' && "
                        + "RegExp.escape('+') === '\\\\+' && "
                        + "RegExp.escape('?') === '\\\\?'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEscapeParentheses() {
        String script = "RegExp.escape('(') === '\\\\(' && " + "RegExp.escape(')') === '\\\\)'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEscapeBrackets() {
        String script = "RegExp.escape('[') === '\\\\[' && " + "RegExp.escape(']') === '\\\\]'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEscapeBraces() {
        String script = "RegExp.escape('{') === '\\\\{' && " + "RegExp.escape('}') === '\\\\}'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEscapePipe() {
        String script = "RegExp.escape('|') === '\\\\|'";
        Utils.assertWithAllModes(true, script);
    }

    // Regular characters (should not be escaped)

    @Test
    public void testNoEscapeRegularChars() {
        // Regular characters should pass through unchanged
        String script =
                "RegExp.escape('a') === 'a' && "
                        + "RegExp.escape('Z') === 'Z' && "
                        + "RegExp.escape('0') === '0' && "
                        + "RegExp.escape('9') === '9' && "
                        + "RegExp.escape('_') === '_' && "
                        + "RegExp.escape(' ') === ' '";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testNoEscapeUnicode() {
        // Unicode characters should pass through unchanged
        String script =
                "RegExp.escape('π') === 'π' && "
                        + "RegExp.escape('€') === '€' && "
                        + "RegExp.escape('你好') === '你好'";
        Utils.assertWithAllModes(true, script);
    }

    // Complex strings

    @Test
    public void testEscapeComplexString() {
        // Mix of syntax chars and regular chars
        String script =
                "RegExp.escape('a.b*c+d?') === 'a\\\\.b\\\\*c\\\\+d\\\\?' && "
                        + "RegExp.escape('[a-z]') === '\\\\[a-z\\\\]' && "
                        + "RegExp.escape('(foo|bar)') === '\\\\(foo\\\\|bar\\\\)'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEscapeSentence() {
        String script = "RegExp.escape('How much? $5.00') === 'How much\\\\? \\\\$5\\\\.00'";
        Utils.assertWithAllModes(true, script);
    }

    // Edge cases

    @Test
    public void testEscapeEmptyString() {
        String script = "RegExp.escape('') === ''";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEscapeAllSyntaxChars() {
        // String with all syntax characters
        String script =
                "RegExp.escape('^$\\\\.*+?()[]{}|') === '\\\\^\\\\$\\\\\\\\\\\\.\\\\*\\\\+\\\\?\\\\(\\\\)\\\\[\\\\]\\\\{\\\\}\\\\|'";
        Utils.assertWithAllModes(true, script);
    }

    // Practical usage tests

    @Test
    public void testUsageInRegExpConstructor() {
        // Verify escaped string works in RegExp constructor
        String script =
                "var input = 'a.b*c'; "
                        + "var escaped = RegExp.escape(input); "
                        + "var re = new RegExp(escaped); "
                        + "re.test('a.b*c') && !re.test('abc')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testUsageWithLiteral() {
        // Combine escaped string with literal pattern
        String script =
                "var userInput = '$5.00'; "
                        + "var escaped = RegExp.escape(userInput); "
                        + "var pattern = '^' + escaped + '$'; "
                        + "var re = new RegExp(pattern); "
                        + "re.test('$5.00') && !re.test('5.00')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testUsageInSearch() {
        // Use escaped string to search in text
        String script =
                "var search = '(foo)'; "
                        + "var text = 'The (foo) function is here'; "
                        + "var escaped = RegExp.escape(search); "
                        + "var re = new RegExp(escaped); "
                        + "re.test(text)";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testUsageWithFlags() {
        // Verify escaped string works with flags
        String script =
                "var input = 'A.B'; "
                        + "var escaped = RegExp.escape(input); "
                        + "var re = new RegExp(escaped, 'i'); "
                        + "re.test('a.b') && re.test('A.B')";
        Utils.assertWithAllModes(true, script);
    }

    // Verify it's a static method

    @Test
    public void testIsStaticMethod() {
        // RegExp.escape should exist as a static method
        String script = "typeof RegExp.escape === 'function'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testNotOnPrototype() {
        // Should not be on prototype
        String script = "typeof RegExp.prototype.escape === 'undefined'";
        Utils.assertWithAllModes(true, script);
    }

    // Verify no double escaping

    @Test
    public void testNoDoubleEscaping() {
        // Escaped string should not be double-escaped
        String script =
                "var once = RegExp.escape('a.b'); "
                        + "var twice = RegExp.escape(once); "
                        + "once !== twice";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEscapedBackslash() {
        // Single backslash should become double backslash
        String script = "RegExp.escape('\\\\') === '\\\\\\\\'";
        Utils.assertWithAllModes(true, script);
    }
}
