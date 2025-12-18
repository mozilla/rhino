/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Rhino-specific regexp implementation tests.
 *
 * <p>Tests internal optimizations and edge cases specific to Rhino's bytecode compiler that are not
 * covered by Test262.
 */
public class RegExpInternalTest {

    @Test
    public void flatStringOptimization() {
        // Tests REOP_FLAT opcode for literal string matching
        final String script = "'abcdef'.match(/bcd/)[0];";
        Utils.assertWithAllModes_ES6("bcd", script);
    }

    @Test
    public void flatStringCaseInsensitive() {
        // Tests REOP_FLATi opcode
        final String script = "'AbCdEf'.match(/bcd/i)[0];";
        Utils.assertWithAllModes_ES6("bCd", script);
    }

    @Test
    public void singleCharOptimization() {
        // Tests REOP_FLAT1 opcode
        final String script = "'abc'.match(/b/)[0];";
        Utils.assertWithAllModes_ES6("b", script);
    }

    @Test
    public void singleCharCaseInsensitive() {
        // Tests REOP_FLAT1i opcode
        final String script = "'aBc'.match(/B/i)[0];";
        Utils.assertWithAllModes_ES6("B", script);
    }

    @Test
    public void unicodeSurrogatePairOptimization() {
        // Tests REOP_UCSPFLAT1 opcode for surrogate pairs
        final String script = "'a😀b'.match(/😀/u)[0];";
        Utils.assertWithAllModes_ES6("😀", script);
    }

    @Test
    public void altPrereqOptimization() {
        // Tests REOP_ALTPREREQ optimization for alternatives
        final String script = "'xfoo'.match(/foo|bar/)[0];";
        Utils.assertWithAllModes_ES6("foo", script);
    }

    @Test
    public void altPrereqCaseInsensitive() {
        // Tests REOP_ALTPREREQi optimization
        final String script = "'xFOO'.match(/foo|bar/i)[0];";
        Utils.assertWithAllModes_ES6("FOO", script);
    }

    @Test
    public void repeatingCaptureGroupClears() {
        // Rhino-specific: quantified capture groups clear on each iteration
        final String script = "var re = /(\\d)+/;\n" + "var m = re.exec('123');\n" + "'' + m[1];";
        Utils.assertWithAllModes_ES6("3", script);
    }

    @Test
    public void greedyQuantifierBehavior() {
        // Tests greedy REOP_STAR behavior
        final String script = "'aaab'.match(/a*/)[0];";
        Utils.assertWithAllModes_ES6("aaa", script);
    }

    @Test
    public void nonGreedyQuantifierBehavior() {
        // Tests non-greedy REOP_MINIMALSTAR behavior
        final String script = "'aaab'.match(/a*?/)[0];";
        Utils.assertWithAllModes_ES6("", script);
    }

    @Test
    public void greedyPlusBehavior() {
        // Tests REOP_PLUS opcode
        final String script = "'aaab'.match(/a+/)[0];";
        Utils.assertWithAllModes_ES6("aaa", script);
    }

    @Test
    public void nonGreedyPlusBehavior() {
        // Tests REOP_MINIMALPLUS opcode
        final String script = "'aaab'.match(/a+?b/)[0];";
        Utils.assertWithAllModes_ES6("aaab", script);
    }

    @Test
    public void optionalGreedy() {
        // Tests REOP_OPT opcode
        final String script = "'ab'.match(/a?b/)[0];";
        Utils.assertWithAllModes_ES6("ab", script);
    }

    @Test
    public void optionalNonGreedy() {
        // Tests REOP_MINIMALOPT opcode
        final String script = "'ab'.match(/a??b/)[0];";
        Utils.assertWithAllModes_ES6("ab", script);
    }

    @Test
    public void quantifierRange() {
        // Tests REOP_QUANT opcode
        final String script = "'aaaa'.match(/a{2,3}/)[0];";
        Utils.assertWithAllModes_ES6("aaa", script);
    }

    @Test
    public void quantifierRangeNonGreedy() {
        // Tests REOP_MINIMALQUANT opcode
        final String script = "'aaaa'.match(/a{2,3}?/)[0];";
        Utils.assertWithAllModes_ES6("aa", script);
    }

    @Test
    public void backrefBasic() {
        // Tests REOP_BACKREF opcode
        final String script = "'aa'.match(/(a)\\1/)[0];";
        Utils.assertWithAllModes_ES6("aa", script);
    }

    @Test
    public void namedBackrefOpcode() {
        // Tests REOP_NAMED_BACKREF opcode for bytecode generation
        final String script = "'aa'.match(/(?<x>a)\\k<x>/)[0];";
        Utils.assertWithAllModes_ES6("aa", script);
    }

    @Test
    public void wordBoundary() {
        // Tests REOP_WBDRY opcode
        final String script = "'foo bar'.match(/\\bbar/)[0];";
        Utils.assertWithAllModes_ES6("bar", script);
    }

    @Test
    public void wordNonBoundary() {
        // Tests REOP_WNONBDRY opcode
        final String script = "'foobar'.match(/o\\Bb/)[0];";
        Utils.assertWithAllModes_ES6("ob", script);
    }

    @Test
    public void beginningOfLine() {
        // Tests REOP_BOL opcode
        final String script = "'foo'.match(/^foo/)[0];";
        Utils.assertWithAllModes_ES6("foo", script);
    }

    @Test
    public void endOfLine() {
        // Tests REOP_EOL opcode
        final String script = "'foo'.match(/foo$/)[0];";
        Utils.assertWithAllModes_ES6("foo", script);
    }

    @Test
    public void dotOperator() {
        // Tests REOP_DOT opcode
        final String script = "'abc'.match(/a.c/)[0];";
        Utils.assertWithAllModes_ES6("abc", script);
    }

    @Test
    public void digitClass() {
        // Tests REOP_DIGIT opcode
        final String script = "'a1b'.match(/\\d/)[0];";
        Utils.assertWithAllModes_ES6("1", script);
    }

    @Test
    public void nonDigitClass() {
        // Tests REOP_NONDIGIT opcode
        final String script = "'1a2'.match(/\\D/)[0];";
        Utils.assertWithAllModes_ES6("a", script);
    }

    @Test
    public void wordClass() {
        // Tests REOP_ALNUM opcode
        final String script = "'_a1-'.match(/\\w+/)[0];";
        Utils.assertWithAllModes_ES6("_a1", script);
    }

    @Test
    public void nonWordClass() {
        // Tests REOP_NONALNUM opcode
        final String script = "'a-b'.match(/\\W/)[0];";
        Utils.assertWithAllModes_ES6("-", script);
    }

    @Test
    public void whitespaceClass() {
        // Tests REOP_SPACE opcode
        final String script = "'a b'.match(/\\s/)[0];";
        Utils.assertWithAllModes_ES6(" ", script);
    }

    @Test
    public void nonWhitespaceClass() {
        // Tests REOP_NONSPACE opcode
        final String script = "' a '.match(/\\S/)[0];";
        Utils.assertWithAllModes_ES6("a", script);
    }

    @Test
    public void characterClass() {
        // Tests REOP_CLASS opcode
        final String script = "'abc'.match(/[abc]/)[0];";
        Utils.assertWithAllModes_ES6("a", script);
    }

    @Test
    public void negatedCharacterClass() {
        // Tests REOP_NCLASS opcode
        final String script = "'abc'.match(/[^a]/)[0];";
        Utils.assertWithAllModes_ES6("b", script);
    }

    @Test
    public void unicodePropertyScript() {
        // Tests REOP_UPROP opcode with Script property
        final String script = "'A'.match(/\\p{Script=Latin}/u)[0];";
        Utils.assertWithAllModes_ES6("A", script);
    }

    @Test
    public void unicodePropertyNegated() {
        // Tests REOP_UPROP_NOT opcode
        final String script = "'1'.match(/\\P{Letter}/u)[0];";
        Utils.assertWithAllModes_ES6("1", script);
    }

    @Test
    public void lookaheadPositive() {
        // Tests REOP_ASSERT opcode
        final String script = "'foo'.match(/f(?=oo)/)[0];";
        Utils.assertWithAllModes_ES6("f", script);
    }

    @Test
    public void lookaheadNegative() {
        // Tests REOP_ASSERT_NOT opcode
        final String script = "'bar'.match(/b(?!oo)/)[0];";
        Utils.assertWithAllModes_ES6("b", script);
    }

    @Test
    public void lookbehindPositiveInternal() {
        // Tests REOP_ASSERTBACK opcode
        final String script = "'foo'.match(/(?<=f)oo/)[0];";
        Utils.assertWithAllModes_ES6("oo", script);
    }

    @Test
    public void lookbehindNegativeInternal() {
        // Tests REOP_ASSERTBACK_NOT opcode
        final String script = "'bar'.match(/(?<!f)ar/)[0];";
        Utils.assertWithAllModes_ES6("ar", script);
    }
}
