/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class RegExpEscapeTest {

    // Basic functionality
    @Test
    public void testRegExpEscapeExists() {
        String script = "typeof RegExp.escape === 'function'";
        Utils.assertWithAllModes(true, script);
    }

    // Syntax characters
    @Test
    public void testEscapeSyntaxCharacters() {
        String script =
                "RegExp.escape('.') === '\\\\.' && "
                        + "RegExp.escape('*') === '\\\\*' && "
                        + "RegExp.escape('+') === '\\\\+' && "
                        + "RegExp.escape('?') === '\\\\?' && "
                        + "RegExp.escape('^') === '\\\\^' && "
                        + "RegExp.escape('$') === '\\\\$' && "
                        + "RegExp.escape('|') === '\\\\|' && "
                        + "RegExp.escape('(') === '\\\\(' && "
                        + "RegExp.escape(')') === '\\\\)' && "
                        + "RegExp.escape('[') === '\\\\[' && "
                        + "RegExp.escape(']') === '\\\\]' && "
                        + "RegExp.escape('{') === '\\\\{' && "
                        + "RegExp.escape('}') === '\\\\}' && "
                        + "RegExp.escape('\\\\') === '\\\\\\\\' && "
                        + "RegExp.escape('/') === '\\\\/'";
        Utils.assertWithAllModes(true, script);
    }

    // Control characters
    @Test
    public void testEscapeControlCharacters() {
        String script =
                "RegExp.escape('\\t') === '\\\\t' && "
                        + "RegExp.escape('\\n') === '\\\\n' && "
                        + "RegExp.escape('\\v') === '\\\\v' && "
                        + "RegExp.escape('\\f') === '\\\\f' && "
                        + "RegExp.escape('\\r') === '\\\\r'";
        Utils.assertWithAllModes(true, script);
    }

    // Other punctuators (should use hex escapes)
    @Test
    public void testEscapeOtherPunctuators() {
        String script =
                "RegExp.escape(',') === '\\\\x2c' && "
                        + "RegExp.escape('-') === '\\\\x2d' && "
                        + "RegExp.escape('=') === '\\\\x3d' && "
                        + "RegExp.escape('<') === '\\\\x3c' && "
                        + "RegExp.escape('>') === '\\\\x3e' && "
                        + "RegExp.escape('#') === '\\\\x23' && "
                        + "RegExp.escape('&') === '\\\\x26' && "
                        + "RegExp.escape('!') === '\\\\x21' && "
                        + "RegExp.escape('%') === '\\\\x25' && "
                        + "RegExp.escape(':') === '\\\\x3a' && "
                        + "RegExp.escape(';') === '\\\\x3b' && "
                        + "RegExp.escape('@') === '\\\\x40' && "
                        + "RegExp.escape('~') === '\\\\x7e' && "
                        + "RegExp.escape(\"'\") === '\\\\x27' && "
                        + "RegExp.escape('`') === '\\\\x60' && "
                        + "RegExp.escape('\"') === '\\\\x22'";
        Utils.assertWithAllModes(true, script);
    }

    // WhiteSpace characters
    @Test
    public void testEscapeWhiteSpace() {
        String script =
                "RegExp.escape('\\u0020') === '\\\\x20' && "
                        + "RegExp.escape('\\u00A0') === '\\\\xa0' && "
                        + "RegExp.escape('\\uFEFF') === '\\\\ufeff' && "
                        + "RegExp.escape('\\u202F') === '\\\\u202f'";
        Utils.assertWithAllModes(true, script);
    }

    // Line terminators
    @Test
    public void testEscapeLineTerminators() {
        String script =
                "RegExp.escape('\\u2028') === '\\\\u2028' && "
                        + "RegExp.escape('\\u2029') === '\\\\u2029'";
        Utils.assertWithAllModes(true, script);
    }

    // Initial digits/letters are escaped, non-initial are not
    @Test
    public void testNotEscaped() {
        String script =
                "RegExp.escape('abc') === '\\\\x61bc' && "
                        + "RegExp.escape('123') === '\\\\x3123' && "
                        + "RegExp.escape('_') === '_' && "
                        + "RegExp.escape('ABC') === '\\\\x41BC' && "
                        + "RegExp.escape('_abc') === '_abc'";
        Utils.assertWithAllModes(true, script);
    }

    // Empty string
    @Test
    public void testEmptyString() {
        String script = "RegExp.escape('') === ''";
        Utils.assertWithAllModes(true, script);
    }

    // Mixed string (spaces are escaped, initial H is escaped)
    @Test
    public void testMixedString() {
        String script =
                "RegExp.escape('Hello. How are you?') === '\\\\x48ello\\\\.\\\\x20How\\\\x20are\\\\x20you\\\\?'";
        Utils.assertWithAllModes(true, script);
    }

    // Complex example
    @Test
    public void testComplexExample() {
        String script = "RegExp.escape('(*.*)') === '\\\\(\\\\*\\\\.\\\\*\\\\)'";
        Utils.assertWithAllModes(true, script);
    }

    // TypeError for non-string inputs
    @Test
    public void testTypeErrorNumber() {
        String script = "try { RegExp.escape(123); false; } catch(e) { e instanceof TypeError; }";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testTypeErrorObject() {
        String script = "try { RegExp.escape({}); false; } catch(e) { e instanceof TypeError; }";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testTypeErrorNull() {
        String script = "try { RegExp.escape(null); false; } catch(e) { e instanceof TypeError; }";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testTypeErrorUndefined() {
        String script =
                "try { RegExp.escape(undefined); false; } catch(e) { e instanceof TypeError; }";
        Utils.assertWithAllModes(true, script);
    }

    // Practical use case: user input escaping
    @Test
    public void testPracticalUseCase() {
        String script =
                "var userInput = 'example.com';"
                        + "var pattern = new RegExp(RegExp.escape(userInput));"
                        + "pattern.test('example.com') === true && "
                        + "pattern.test('exampleXcom') === false";
        Utils.assertWithAllModes(true, script);
    }
}
