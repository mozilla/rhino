package org.mozilla.javascript.tests.es2024;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ES2018 RegExp dotAll flag ('s' flag) */
public class RegExpDotAllTest {

    @Test
    public void testDotAllGetter() {
        // Without s flag
        Utils.assertWithAllModes(false, "/./.dotAll");
        Utils.assertWithAllModes(false, "/./i.dotAll");
        Utils.assertWithAllModes(false, "/./g.dotAll");
        Utils.assertWithAllModes(false, "/./m.dotAll");
        Utils.assertWithAllModes(false, "new RegExp('.', '').dotAll");
        Utils.assertWithAllModes(false, "new RegExp('.', 'i').dotAll");

        // With s flag
        Utils.assertWithAllModes(true, "/./s.dotAll");
        Utils.assertWithAllModes(true, "/./is.dotAll");
        Utils.assertWithAllModes(true, "/./sg.dotAll");
        Utils.assertWithAllModes(true, "/./ms.dotAll");
        Utils.assertWithAllModes(true, "new RegExp('.', 's').dotAll");
        Utils.assertWithAllModes(true, "new RegExp('.', 'is').dotAll");
    }

    @Test
    public void testDotAllMatchingNewline() {
        // Without s flag, dot should not match newlines
        Utils.assertWithAllModes(false, "/^.$/.test('\\n')");
        Utils.assertWithAllModes(false, "/^.$/.test('\\r')");
        Utils.assertWithAllModes(false, "/^.$/.test('\\u2028')");
        Utils.assertWithAllModes(false, "/^.$/.test('\\u2029')");

        // With s flag, dot should match newlines
        Utils.assertWithAllModes(true, "/^.$/s.test('\\n')");
        Utils.assertWithAllModes(true, "/^.$/s.test('\\r')");
        Utils.assertWithAllModes(true, "/^.$/s.test('\\u2028')");
        Utils.assertWithAllModes(true, "/^.$/s.test('\\u2029')");
    }

    @Test
    public void testDotAllMatchingRegularChars() {
        // Should still match regular characters
        Utils.assertWithAllModes(true, "/^.$/s.test('a')");
        Utils.assertWithAllModes(true, "/^.$/s.test('3')");
        Utils.assertWithAllModes(true, "/^.$/s.test('π')");
        Utils.assertWithAllModes(true, "/^.$/s.test('\\v')");
        Utils.assertWithAllModes(true, "/^.$/s.test('\\f')");
    }

    @Test
    public void testDotAllWithMultiline() {
        // s and m flags should work together
        Utils.assertWithAllModes(true, "/^.$/sm.test('\\n')");
        Utils.assertWithAllModes(true, "/^.$/ms.test('\\n')");
    }

    @Test
    public void testDotAllMultipleChars() {
        // Test with multiple characters
        Utils.assertWithAllModes(true, "/^...$/s.test('a\\nb')");
        Utils.assertWithAllModes(false, "/^...$/.test('a\\nb')");
    }
}
