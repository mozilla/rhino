/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.BiFunction;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.regexp.NativeRegExp;
import org.mozilla.javascript.testutils.Utils;

public class NativeRegExpTest {

    @Test
    public void openBrace() {
        final String script = "/0{0/";
        Utils.runWithAllModes(
                _cx -> {
                    final ScriptableObject scope = _cx.initStandardObjects();
                    final Object result = _cx.evaluateString(scope, script, "test script", 0, null);
                    assertEquals(script, Context.toString(result));
                    return null;
                });
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalCtor() throws Exception {
        testEvaluate("g-true-false-false-false-false", "new RegExp('foo', 'g');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void global() throws Exception {
        testEvaluate("g-true-false-false-false-false", "/foo/g;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCaseCtor() throws Exception {
        testEvaluate("i-false-true-false-false-false", "new RegExp('foo', 'i');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCase() throws Exception {
        testEvaluate("i-false-true-false-false-false", "/foo/i;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void multilineCtor() throws Exception {
        testEvaluate("m-false-false-true-false-false", "new RegExp('foo', 'm');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void multiline() throws Exception {
        testEvaluate("m-false-false-true-false-false", "/foo/m;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void dotAllCtor() throws Exception {
        testEvaluate("s-false-false-false-true-false", "new RegExp('foo', 's');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void dotAll() throws Exception {
        testEvaluate("s-false-false-false-true-false", "/foo/s;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void stickyCtor() throws Exception {
        testEvaluate("y-false-false-false-false-true", "new RegExp('foo', 'y');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void sticky() throws Exception {
        testEvaluate("y-false-false-false-false-true", "/foo/y;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalMultilineCtor() throws Exception {
        testEvaluate("gm-true-false-true-false-false", "new RegExp('foo', 'gm');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalMultiline() throws Exception {
        testEvaluate("gm-true-false-true-false-false", "/foo/gm;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalDotAll() throws Exception {
        testEvaluate("gs-true-false-false-true-false", "/foo/gs;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalIgnoreCaseCtor() throws Exception {
        testEvaluate("gi-true-true-false-false-false", "new RegExp('foo', 'ig');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalIgnoreCase() throws Exception {
        testEvaluate("gi-true-true-false-false-false", "/foo/ig;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalStickyCtor() throws Exception {
        testEvaluate("gy-true-false-false-false-true", "new RegExp('foo', 'gy');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalSticky() throws Exception {
        testEvaluate("gy-true-false-false-false-true", "/foo/gy;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalMultilineIgnoreCaseCtor() throws Exception {
        testEvaluate("gim-true-true-true-false-false", "new RegExp('foo', 'mig');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalMultilineIgnoreCase() throws Exception {
        testEvaluate("gim-true-true-true-false-false", "/foo/gmi;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalDotAllIgnoreCaseCtor() throws Exception {
        testEvaluate("gis-true-true-false-true-false", "new RegExp('foo', 'gsi');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalDotAllIgnoreCase() throws Exception {
        testEvaluate("gis-true-true-false-true-false", "/foo/gsi;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalIgnoreCaseStickyCtor() throws Exception {
        testEvaluate("giy-true-true-false-false-true", "new RegExp('foo', 'yig');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalIgnoreCaseSticky() throws Exception {
        testEvaluate("giy-true-true-false-false-true", "/foo/ygi;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalMultilineStickyCtor() throws Exception {
        testEvaluate("gmy-true-false-true-false-true", "new RegExp('foo', 'gmy');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalMultilineSticky() throws Exception {
        testEvaluate("gmy-true-false-true-false-true", "/foo/gmy;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalDotAllStickyCtor() throws Exception {
        testEvaluate("gsy-true-false-false-true-true", "new RegExp('foo', 'gys');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void globalDotAllSticky() throws Exception {
        testEvaluate("gsy-true-false-false-true-true", "/foo/gys;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCaseMultilineCtor() throws Exception {
        testEvaluate("im-false-true-true-false-false", "new RegExp('foo', 'im');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCaseMultiline() throws Exception {
        testEvaluate("im-false-true-true-false-false", "/foo/mi;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCaseDotAllCtor() throws Exception {
        testEvaluate("is-false-true-false-true-false", "new RegExp('foo', 'si');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCaseDotAll() throws Exception {
        testEvaluate("is-false-true-false-true-false", "/foo/si;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCaseStickyCtor() throws Exception {
        testEvaluate("iy-false-true-false-false-true", "new RegExp('foo', 'yi');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void ignoreCaseSticky() throws Exception {
        testEvaluate("iy-false-true-false-false-true", "/foo/iy;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void multilineStickyCtor() throws Exception {
        testEvaluate("my-false-false-true-false-true", "new RegExp('foo', 'my');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void multilineSticky() throws Exception {
        testEvaluate("my-false-false-true-false-true", "/foo/my;");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void dotAllStickyCtor() throws Exception {
        testEvaluate("sy-false-false-false-true-true", "new RegExp('foo', 'ys');");
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void dotAllSticky() throws Exception {
        testEvaluate("sy-false-false-false-true-true", "/foo/ys;");
    }

    private static void testEvaluate(final String expected, final String regex) {
        final String script =
                "var regex = "
                        + regex
                        + "\n"
                        + "var res = ''\n;"
                        + "res += regex.flags;\n"
                        + "res += '-';\n"
                        + "res += regex.global;\n"
                        + "res += '-';\n"
                        + "res += regex.ignoreCase;\n"
                        + "res += '-';\n"
                        + "res += regex.multiline;\n"
                        + "res += '-';\n"
                        + "res += regex.dotAll;\n"
                        + "res += '-';\n"
                        + "res += regex.sticky;\n"
                        + "res";

        Utils.assertWithAllModes_ES6(expected, script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void stickyTest() throws Exception {
        final String script =
                "var str = 'table football';\n"
                        + "var regex = new RegExp('foo', 'y');\n"
                        + "regex.lastIndex = 6;\n"
                        + "var res = '' + regex.test(str);\n"
                        + "res = res + '-' + regex.lastIndex;\n"
                        + "res = res + '-' + regex.test(str);\n"
                        + "res = res + '-' + regex.lastIndex;\n"
                        + "res = res + '-' + regex.test(str);\n"
                        + "res = res + '-' + regex.lastIndex;\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("true-9-false-0-false-0", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void stickyStartOfLine() throws Exception {
        final String script =
                "var regex = /^foo/y;\n"
                        + "regex.lastIndex = 2;\n"
                        + "var res = '' + regex.test('..foo');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("false", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void stickyStartOfLineMultiline() throws Exception {
        final String script =
                "var regex = /^foo/my;\n"
                        + "regex.lastIndex = 2;\n"
                        + "var res = '' + regex.test('..foo');\n"
                        + "regex.lastIndex = 2;\n"
                        + "res = res + '-' + regex.test('.\\nfoo');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("false-true", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void matchGlobal() throws Exception {
        final String script =
                "var result = 'aaba'.match(/a/g);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("3-a-a-a", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void matchGlobalSymbol() throws Exception {
        final String script =
                "var result = /a/g[Symbol.match]('aaba');\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("3-a-a-a", script);
    }

    @Test
    public void execStickySymbol() throws Exception {
        final String script =
                "var regex = /[abc]/y;\n"
                        + "var res = regex.exec('ab-c') + '-' + regex.lastIndex + '-'\n"
                        + "res += regex.exec('ab-c') + '-' + regex.lastIndex + '-'\n"
                        + "res += regex.exec('ab-c') + '-' + regex.lastIndex\n"
                        + "res;";

        Utils.assertWithAllModes_ES6("a-1-b-2-null-0", script);
    }

    @Test
    public void exeGlobalStickySymbol() throws Exception {
        final String script =
                "var regex = /[abc]/gy;\n"
                        + "var res = regex.exec('ab-c') + '-' + regex.lastIndex + '-'\n"
                        + "res += regex.exec('ab-c') + '-' + regex.lastIndex + '-'\n"
                        + "res += regex.exec('ab-c') + '-' + regex.lastIndex\n"
                        + "res;";

        Utils.assertWithAllModes_ES6("a-1-b-2-null-0", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void matchDotAll() throws Exception {
        final String script =
                "var result = 'bar\\nfoo'.match(/bar.foo/s);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("1-bar\nfoo", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void matchSticky() throws Exception {
        final String script =
                "var result = 'aaba'.match(/a/y);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("1-a", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void matchStickySymbol() throws Exception {
        final String script =
                "var result = /a/y[Symbol.match]('aaba');\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("1-a", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void matchStickyAndGlobal() throws Exception {
        final String script =
                "var result = 'aaba'.match(/a/yg);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("2-a-a", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void matchStickyAndGlobalSymbol() throws Exception {
        final String script =
                "var result = /a/yg[Symbol.match]('aaba');\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("2-a-a", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    // TODO @Test
    public void flagsPropery() throws Exception {
        final String script =
                "var get = Object.getOwnPropertyDescriptor(RegExp.prototype, 'flags');\n"
                        + "var res = '';" // + get.get.length;\n"
                        + "res = res + '-' + get.value;\n"
                        + "res = res + '-' + get.configurable;\n"
                        + "res = res + '-' + get.enumerable;\n"
                        + "res = res + '-' + get.writable;\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("0-undefined-true-false-undefined", script);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void objectToString() throws Exception {
        Utils.assertWithAllModes_ES6("/undefined/undefined", "RegExp.prototype.toString.call({})");
        Utils.assertWithAllModes_ES6(
                "/Foo/undefined", "RegExp.prototype.toString.call({source: 'Foo'})");
        Utils.assertWithAllModes_ES6(
                "/undefined/gy", "RegExp.prototype.toString.call({flags: 'gy'})");
        Utils.assertWithAllModes_ES6(
                "/Foo/g", "RegExp.prototype.toString.call({source: 'Foo', flags: 'g'})");
        Utils.assertWithAllModes_ES6(
                "/Foo/g",
                "RegExp.prototype.toString.call({source: 'Foo', flags: 'g', sticky: true})");

        Utils.assertWithAllModes_ES6(
                "TypeError: Method \"toString\" called on incompatible object",
                "try { RegExp.prototype.toString.call(''); } catch (e) { ('' + e).substr(0, 58) }");
        Utils.assertWithAllModes_ES6(
                "TypeError: Method \"toString\" called on incompatible object",
                "try { RegExp.prototype.toString.call(undefined); } catch (e) { ('' + e).substr(0, 58) }");
        Utils.assertWithAllModes_ES6(
                "TypeError: Method \"toString\" called on incompatible object",
                "var toString = RegExp.prototype.toString; try { toString(); } catch (e) { ('' + e).substr(0, 58) }");
    }

    @Test
    public void prettyPrinterDoesntBlowUp() {
        final String regexp = "/(a{3,6})(?=\\1)/g";
        Utils.runWithAllModes(
                _cx -> {
                    final ScriptableObject scope = _cx.initStandardObjects();
                    final Object result = _cx.evaluateString(scope, regexp, "test script", 0, null);
                    assertTrue(result instanceof NativeRegExp);

                    NativeRegExp nr = (NativeRegExp) result;
                    // use reflection to access the private member 're' of the nativeregexp object
                    // of the NativeRegExp object and call the
                    // private method prettyPrintRE static method with the 're' member
                    // to make sure it doesn't blow up
                    try {
                        java.lang.reflect.Field reField = NativeRegExp.class.getDeclaredField("re");
                        reField.setAccessible(true);
                        Object re = reField.get(nr);
                        java.lang.reflect.Method prettyPrintRE =
                                NativeRegExp.class.getDeclaredMethod(
                                        "prettyPrintRE", re.getClass());
                        prettyPrintRE.setAccessible(true);
                        prettyPrintRE.invoke(NativeRegExp.class, re);
                    } catch (Exception e) {
                        fail("NativeRegExp::prettyPrintRE blew up");
                    }
                    return null;
                });
    }

    @Test
    public void lookbehindPositive() throws Exception {
        // matches numbers that are preceded by a dollar sign
        final String script =
                "var regex = /(?<=\\$)\\d+/g;\n"
                        + "var result = '$123 $456 789'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("2-123-456", script);
    }

    @Test
    public void lookbehindNegative() throws Exception {
        // matches numbers that are not preceded by a dollar sign
        final String script =
                "var regex = /(?<!\\$)\\d/g;\n"
                        + "var result = '$1 $4 7'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("1-7", script);
    }

    @Test
    public void lookbehindCapture() throws Exception {
        // This shows that the lookbehind matches the input backwards
        // this is why, the second capture group is 'bc' and not 'c'
        final String script =
                "var regex = /(?<=([ab]+)([bc]+))$/;\n"
                        + "var result = 'abc'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("3-a-bc", script);
    }

    @Test
    public void lookbehindQuantifiedCapture() throws Exception {
        // When a lookbehind has quantified capture groups, the left-most match is captured
        final String script =
                "var regex = /(?<=(\\d)+)a/;\n"
                        + "var result = '123a'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("2-a-1", script);
    }

    @Test
    public void lookbehindBackreference2() throws Exception {
        final String script =
                "var regex = /(?<=(\\2)x(.))y/;\n"
                        + "var result = '4x4y'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("3-y-4-4", script);
    }

    @Test
    public void lookbehindNested() throws Exception {
        // lookbehind inside a lookbehind matches the input backwards
        final String script =
                "var regex = /(?<=([ab]+)([bc]+)(?<=([ab]+)([bc]+)))$/;\n"
                        + "var result = 'abcabc'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res = res + '-' + result[3];\n"
                        + "res = res + '-' + result[4];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("5--a-bc-a-bc", script);
    }

    @Test
    public void lookbehindLookahead() throws Exception {
        // lookahead inside a lookbehind matches forward
        final String script =
                "var regex = /(?<=([ab]+)([bc]+)(?=([xy]+)([yz]+)))/;\n"
                        + "var result = 'abcxyz'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res = res + '-' + result[3];\n"
                        + "res = res + '-' + result[4];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("5--a-bc-xy-z", script);
    }

    @Test
    public void lookbehindFlatCaseInsensitive() throws Exception {
        final String script =
                "var regex = /abc(?<=ABC)/i;\n"
                        + "var result = 'abc'.match(regex);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("1-abc", script);
    }

    @Test
    public void backwardFlatCaseInsensitiveNoMatch() throws Exception {
        final String script = "var regex = /abc(?<=XYZ)/i;\n" + "'abc'.match(regex);";
        Utils.assertWithAllModes_ES6(null, script);
    }

    @Test
    public void quantifiedCaptureClearsPreviousCaptures() {
        // With the pattern /(?:(\2)(\d))*/, the first capture group should be always empty
        // when used with a quantifier
        BiFunction<String, String, String> test =
                (quantifier, input) -> {
                    return "var regexStr = '(?:(\\\\2)(\\\\d))'\n"
                            + "function test(quantifier, input) {\n"
                            + "  var regexp = new RegExp(regexStr + quantifier + '$');\n"
                            + "  var res = regexp.exec(input);\n"
                            + "  return res != null && res.length == 3 && res[1] == '' && res[2] != '';\n"
                            + "}\n"
                            + "test('"
                            + quantifier
                            + "','"
                            + input
                            + "');";
                };

        Utils.assertWithAllModes_ES6("greedy-*", true, test.apply("*", "123"));
        Utils.assertWithAllModes_ES6("greedy-+", true, test.apply("+", "123"));
        Utils.assertWithAllModes_ES6("greedy-?", true, test.apply("?", "123"));
        Utils.assertWithAllModes_ES6("greedy-{2}", true, test.apply("{2}", "123"));
        Utils.assertWithAllModes_ES6("greedy-{2,3}", true, test.apply("{2,}", "123"));
        Utils.assertWithAllModes_ES6("non-greedy-*", true, test.apply("*?", "123"));
        Utils.assertWithAllModes_ES6("non-greedy-+", true, test.apply("+?", "123"));
        Utils.assertWithAllModes_ES6("non-greedy-?", true, test.apply("??", "123"));
        Utils.assertWithAllModes_ES6("non-greedy-{2}", true, test.apply("{2}?", "123"));
        Utils.assertWithAllModes_ES6("non-greedy-{2,3}", true, test.apply("{2,}?", "123"));
    }

    @Test
    public void namedBackrefWithoutNamedCapture() {
        final String script =
                "var regex = /\\k<name>/;\n" + "var res = '' + regex.test('k<name>');\n" + "res;";
        Utils.assertWithAllModes_ES6("true", script);
    }

    @Test
    public void invalidNamedBackrefWithoutNamedCapture() {
        final String script =
                "var regex = /\\k<nam/;\n" + "var res = '' + regex.test('k<nam');\n" + "res;";
        Utils.assertWithAllModes_ES6("true", script);
    }

    @Test
    public void invalidNamedBackrefWithNamedCapture() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Invalid named capture referenced", "/\\k<nam(?<foo>)/.compile()");
    }

    @Test
    public void duplicateNamedCapture() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Duplicate capture group name", "/(?<foo>)(?<foo>)/.compile()");
    }

    @Test
    public void namedBackrefNotFound() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Invalid named capture referenced", "/(?<foo>)\\k<bar>/.compile()");
    }

    @Test
    public void namedBackref() {
        final String script =
                "var regex = /(?<foo>\\d)\\k<foo>/m;\n"
                        + "var res = '' + regex.test('11');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("true", script);
    }

    @Test
    public void duplicateNamedCapturesInDisjunction() {
        final String script =
                "var regex = /(?:(?<x>a)|(?<x>b))\\k<x>/;\n"
                        + "var res = '' + regex.test('bb');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("true", script);
    }

    @Test
    public void duplicateNamedCaptures() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Duplicate capture group name \"x\"", "/(?<x>a)(?<x>b)/.compile()");

        Utils.assertEcmaErrorES6(
                "SyntaxError: Duplicate capture group name \"x\"",
                "/(?<x>a)(?:b|(?<x>c))/.compile()");
    }

    @Test
    public void namedCaptureInDisjunction() {
        final String script =
                "var regex = /a|(?<x>b)/;\n"
                        + "var result = regex.exec('b');\n"
                        + "var res = '' + result.groups.x;\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("b", script);
    }

    @Test
    public void duplicateNamedCaptureInDisjunction() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Duplicate capture group name \"a\"",
                "/(?<a>a)(?:(?<a>b)|(?<a>c))/.compile()");
    }

    @Test
    public void execNamedCapture() {
        final String script =
                "var regex = /(?<foo>\\d)(?<bar>\\d)/;\n"
                        + "var result = regex.exec('12');\n"
                        + "var res = '' + result.groups.foo;\n"
                        + "res = res + '-' + result.groups.bar;\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("1-2", script);
    }

    @Test
    public void execNamedCaptureBackref() {
        final String script =
                "var regex = /(?<foo>\\d)(?<bar>\\d)\\1\\2/;\n"
                        + "var result = regex.exec('1212');\n"
                        + "var res = '' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("1-2", script);
    }

    // just a \k without a name is invalid when there is a named capture group
    @Test
    public void invalidNamedBackref() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Invalid named capture referenced", "/(?<a>.)\\k/.compile()");
    }

    @Test
    public void slashKInCharClass() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: invalid Unicode escape sequence", "/(?<a>.)[\\k]/.compile()");
    }

    // test that \0000 is octal escape for 0 (only supported by Rhino, inherited from SpiderMonkey)
    @Test
    public void octalEscapeSpiderMonkey() {
        final String script =
                "var regex = /\\0000101/;\n" + "var res = '' + regex.test('A');\n" + "res;";
        Utils.assertWithAllModes_ES6("true", script);
    }

    // test that \0000 in character class is two chars \000 and 0
    @Test
    public void octalEscapeInCharacterClass() {
        final String script =
                "var regex = /[\\0000]/;\n"
                        + "var res = '' + regex.test('\\0') + '-' + regex.test('0');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("true-true", script);
    }

    @Test
    public void controlEscapeParsing() {
        // tests for \c within and without char class and in unicode mode and not
        final String script =
                "var regex = /[\\c]/;\n" + "var res = '' + regex.test('\\c');\n" + "res;";
        Utils.assertWithAllModes_ES6("true", script);

        final String script2 =
                "var regex = /\\c/;\n" + "var res = '' + regex.test('\\\\c');\n" + "res;";
        Utils.assertWithAllModes_ES6("true", script2);

        // same above, but unicode flag
        final String script3 =
                "var regex = /[\\c]/u;\n" + "var res = '' + regex.test('\\c');\n" + "res;";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script3);

        final String script4 =
                "var regex = /\\c/u;\n" + "var res = '' + regex.test('\\\\c');\n" + "res;";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script4);
    }

    @Test
    public void unterminatedBackslash() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Trailing \\ in regular expression", "new RegExp('[\\\\')");
        Utils.assertEcmaErrorES6(
                "SyntaxError: Trailing \\ in regular expression", "new RegExp('\\\\')");
    }

    // test z-a is invalid character range
    @Test
    public void invalidCharacterRange() {
        Utils.assertEcmaErrorES6(
                "SyntaxError: Invalid range in character class", "/[z-a]/.compile()");
    }

    @Test
    public void matchEmptyCharacterClass() {
        Utils.assertWithAllModes_ES6(null, "''.match(/[]/)");
        Utils.assertWithAllModes_ES6(null, "'abc'.match(/[]/)");

        Utils.assertWithAllModes_ES6("", "''.match(/[]*/)[0]");
        Utils.assertWithAllModes_ES6(1, "''.match(/[]*/).length");

        Utils.assertWithAllModes_ES6("", "'abc'.match(/[]*/)[0]");
        Utils.assertWithAllModes_ES6(1, "'abc'.match(/[]*/).length");
    }

    @Test
    public void replaceEmptyCharacterClass() {
        Utils.assertWithAllModes_ES6("", "''.replace(/[]/, 'x')");
        Utils.assertWithAllModes_ES6("abc", "'abc'.replace(/[]/, 'x')");

        Utils.assertWithAllModes_ES6("x", "''.replace(/[]*/, 'x')");
        Utils.assertWithAllModes_ES6("xabc", "'abc'.replace(/[]*/, 'x')");

        Utils.assertWithAllModes_ES6("x", "''.replace(/[]*/g, 'x')");
        Utils.assertWithAllModes_ES6("xaxbxcx", "'abc'.replace(/[]*/g, 'x')");

        Utils.assertWithAllModes_ES6("xaxbxxcx*xdx", "'ab]c*d'.replace(/[]*]*/g, 'x')");
    }

    @Test
    public void testEmptyCharacterClass() {
        Utils.assertWithAllModes_ES6(false, "/[]/.test('')");
        Utils.assertWithAllModes_ES6(false, "/[]/.test('abc')");

        Utils.assertWithAllModes_ES6(true, "/[]*/.test('')");
        Utils.assertWithAllModes_ES6(true, "/[]*/.test('abc')");
    }

    @Test
    public void characterClassRangeWithSingleItemCharacterClasses() {
        final String script =
                "var regex = /[\\b-\\x10]/;\n"
                        + "var res = '' + regex.test('\\x09') + '-' + regex.test('\\x07') + '-' + regex.test('\\x11');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("true-false-false", script);
    }

    @Test
    public void characterClassWithMultiCharacterEscape() {
        // [\d-A] should be parsed as a union of 3 character sets - digits,  '-', and 'A'.
        // Therefore it shouldn't match something in between '9' and 'A', say ';'
        final String script =
                "var regex = /[\\d-A]/;\n"
                        + "var res = '' + regex.test('5') + '-' + regex.test('A') + '-' + regex.test('-') + '-' + regex.test(';');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("true-true-true-false", script);
    }

    // The spec says that \\u and \\x should be followed by exactly 4 and 2 hex digits respectively
    // for them to be valid unicode escapes. Rhino allows them to be followed by less than 4 or 2
    @Test
    public void unicodeEscapeFallback() {
        final String script =
                "var res = '' + /\\u/.test('u') + '-';\n"
                        + "res += '' + /\\x/.test('x') + '-';\n"
                        + "res += '' + /\\x1/.test('\\x01') + '-';\n"
                        + "res += '' + /\\u61/.test('a');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("true-true-true-true", script);
    }

    @Test
    public void unterminatedCharacterClass() {
        Utils.assertEcmaErrorES6("SyntaxError: Unterminated character class", "new RegExp('[')");

        // rhino permits hex and unicode escapes with less than 2 and 4 digits respectively
        final String script3 =
                "var regex = /\\x1/;\n" + "var res = '' + regex.test('\\x01');\n" + "res;";
        Utils.assertWithAllModes_ES6("true", script3);

        final String script4 =
                "var regex = /\\u61/;\n" + "var res = '' + regex.test('a');\n" + "res;";
        Utils.assertWithAllModes_ES6("true", script4);
    }

    @Test
    public void unicodeEscapeFallbackWithUFlag() {
        final String script =
                "var regex = /\\u/u;\n" + "var res = '' + regex.test('u');\n" + "res;";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script);

        final String script2 =
                "var regex = /\\x/u;\n" + "var res = '' + regex.test('x');\n" + "res;";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script2);

        // same as script3 and 4 above,  but they fail too
        final String script3 =
                "var regex = /\\x1/u;\n" + "var res = '' + regex.test('\\x01');\n" + "res;";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script3);

        final String script4 =
                "var regex = /\\u61/u;\n" + "var res = '' + regex.test('a');\n" + "res;";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script4);
    }

    @Test
    public void testUnicodeModeDot() {
        final String script =
                "var regex = /./u;\n"
                        + "var res = '' + regex.exec('\\uD83D\\uDE00') + '-' + regex.test('\\uD83D');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("😀-true", script);
    }

    @Test
    public void surrogatePairInputReadCorrectly() {
        final String script =
                "var regex = /\\uD83D/u;\n"
                        + "var res = '' + regex.test('\\uD83D\\uDE00') + '-' + regex.test('\\uD83D');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("false-true", script);

        final String script2 =
                "var regex = /a\\uD83D/u;\n"
                        + "var res = '' + regex.test('a\\uD83D\\uDE00') + '-' + regex.test('a\\uD83D');\n"
                        + "res;";
        Utils.assertWithAllModes_ES6("false-true", script2);
    }

    @Test
    public void testUnicodeEscapes() {
        Utils.assertWithAllModes_ES6(
                "high-low surrogate pair", "😀", "'😀'.match(/\\uD83D\\uDE00/u)[0]");
        Utils.assertWithAllModes_ES6("high surrogate", "\uD83D", "'\\uD83D'.match(/\\uD83D/u)[0]");
        Utils.assertWithAllModes_ES6("low surrogate", "\uDE00", "'\\uDE00'.match(/\\uDE00/u)[0]");
        Utils.assertWithAllModes_ES6("non surrogate", "\u0000", "'\\u0000'.match(/\\u0000/u)[0]");
        Utils.assertWithAllModes_ES6("ASCII", "a", "'a'.match(/\\u0061/u)[0]");
        Utils.assertWithAllModes_ES6(
                "unicode code point inside the BMP", "©", "'©'.match(/\\u00A9/u)[0]");
        Utils.assertWithAllModes_ES6(
                "unicode code point outside the BMP", "😀", "'😀'.match(/\\u{1F600}/u)[0]");
    }

    @Test
    public void testIncompleteDigitHexEscape() {
        final String script1 =
                "'\\x01'.match(/\\x1/)[0] + '\\x01'.match(/\\u1/)[0] == '\\x01\\x01';";
        Utils.assertWithAllModes_ES6(true, script1);

        final String script2 = "'x1k'.match(/\\x1k/)[0] + 'u1k'.match(/\\u1k/)[0];";
        Utils.assertWithAllModes_ES6("x1ku1k", script2);

        // same as above, with u flag throws
        final String script3 =
                "'\\x01'.match(/\\x1/u)[0] + '\\x01'.match(/\\u1/u)[0] == '\\x01\\x01';";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script3);

        final String script4 = "'x1k'.match(/\\x1k/u)[0] + 'u1k'.match(/\\u1k/u)[0];";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script4);
    }

    // tests that in non-unicode mode \\u{5} is treated as a quantifier
    @Test
    public void testUQuantifier() {
        final String script = "'uu'.match(/\\u{2}/)[0];";
        Utils.assertWithAllModes_ES6("uu", script);
    }

    @Test
    public void testInvalidUnicodeEscape() {
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", "/\\u/u");
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", "/\\u{/u");
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", "/\\u{}/u");
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", "/\\u{1/u");
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", "/\\u{k}/u");
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", "/\\ua/u");
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", "/\\u}/u");
    }

    @Test
    public void testDecimalEscapeAsOctalEscape() {
        final String script1 = "'x\\2'.match(/(.)\\2/u)[0];";
        Utils.assertEcmaErrorES6("SyntaxError: invalid Unicode escape sequence", script1);

        final String script2 = "'x\\x02'.match(/(.)\\2/)[0] == 'x\\x02'";
        Utils.assertWithAllModes_ES6(true, script2);
    }

    @Test
    public void testSimpleRegExpWithNonLatinLiterals() {
        final String script =
                "var result = '€'.match(/€/)"
                        + " + '-' + '€'.match(/[€]/)"
                        + " + '-' + '😀'.match(/😀/)"
                        + " + '-' + '😀'.match(/[😀]/u)"
                        // without the 'u' flag the surrogate pair is treated as two chars in the
                        // character class
                        + " + '-' + '😀'.match(/[😀]/)"
                        + " + '-' + '😀'.match(/\\uD83D/)"
                        + " + '-' + '😀'.match(/\\uDE00/)\n"
                        // with the 'u' flag 😀is consumed as a single codepoint
                        + " + '-' + '😀'.match(/\\uDE00/u)\n"
                        + "result";
        Utils.assertWithAllModes_ES6("€-€-😀-😀-\uD83D-\uD83D-\uDE00-null", script);
    }

    @Test
    public void testUnicodeEscapesInClass() {
        Utils.assertWithAllModes_ES6(
                "high-low surrogate pair", "😀", "'😀'.match(/[\\uD83D\\uDE00]/u)[0]");
        Utils.assertWithAllModes_ES6(
                "high surrogate", "\uD83D", "'\\uD83D'.match(/[\\uD83D]/u)[0]");
        Utils.assertWithAllModes_ES6("low surrogate", "\uDE00", "'\\uDE00'.match(/[\\uDE00]/u)[0]");
        Utils.assertWithAllModes_ES6("non surrogate", "\u0000", "'\\u0000'.match(/[\\u0000]/u)[0]");
        Utils.assertWithAllModes_ES6("ASCII", "a", "'a'.match(/[\\u0061]/u)[0]");
        Utils.assertWithAllModes_ES6(
                "unicode code point inside the BMP", "©", "'©'.match(/[\\u00A9]/u)[0]");
        Utils.assertWithAllModes_ES6(
                "unicode code point outside the BMP", "😀", "'😀'.match(/[\\u{1F600}]/u)[0]");
    }

    @Test
    public void testUnicodePropertyEscape() {
        Utils.assertWithAllModes_ES6("uppercase letter", "A", "'A'.match(/\\p{Lu}/u)[0]");
    }

    @Test
    public void testUnicodePropertyEscapeZanabazarSquare() {
        Utils.assertWithAllModes_ES6(
                "Zanabazar Square",
                "\uD806\uDE45",
                "'\\u{11A45}'.match(/\\p{sc=Zanabazar_Square}/u)[0]");
    }

    @Test
    public void atomicQuantifierOptimization_greedyQuantifiers() {
        // Patterns ending with $ have greedy quantifiers marked as atomic
        String expected =
                String.join(
                        "-",
                        "b", // non-atomic sanity check: /a*b/.exec('b')[0]
                        "aaa", // +$ match: /a+$/.exec('aaa')[0]
                        "true", // +$ no match: /a+$/.exec('aaab') === null
                        "aaa", // *$ match: /a*$/.exec('aaa')[0]
                        "true", // *$ empty at end: /a*$/.exec('b')[0] === ""
                        "1", // *$ index: /a*$/.exec('b').index
                        "true", // *$ trailing non-a: /a*$/.exec('aaab')[0] === ""
                        "4", // *$ index trailing: /a*$/.exec('aaab').index
                        "a", // ?$ match: /a?$/.exec('a')[0]
                        "true", // ?$ empty at end: /a?$/.exec('b')[0] === ""
                        "true", // ?$ trailing non-a: /a?$/.exec('ab')[0] === ""
                        "2", // ?$ index trailing: /a?$/.exec('ab').index
                        "aaa", // {2,}$ match: /a{2,}$/.exec('aaa')[0]
                        "true" // {2,}$ no match: /a{2,}$/.exec('aaab') === null
                        );
        Utils.assertWithAllModes_ES6(
                expected,
                "["
                        + "/a*b/.exec('b')[0],"
                        + "/a+$/.exec('aaa')[0],"
                        + "/a+$/.exec('aaab') === null,"
                        + "/a*$/.exec('aaa')[0],"
                        + "/a*$/.exec('b')[0] === '',"
                        + "/a*$/.exec('b').index,"
                        + "/a*$/.exec('aaab')[0] === '',"
                        + "/a*$/.exec('aaab').index,"
                        + "/a?$/.exec('a')[0],"
                        + "/a?$/.exec('b')[0] === '',"
                        + "/a?$/.exec('ab')[0] === '',"
                        + "/a?$/.exec('ab').index,"
                        + "/a{2,}$/.exec('aaa')[0],"
                        + "/a{2,}$/.exec('aaab') === null"
                        + "].join('-')");
    }

    @Test
    public void atomicQuantifierOptimization_unicodeAndNonGreedy() {
        // Unicode property escapes and non-greedy quantifiers
        String expected =
                String.join(
                        "-",
                        "\u03B1\u03B2\u03B3", // unicode +$: /\p{Script=Greek}+$/u.exec('αβγ')[0]
                        "true", // unicode +$ no match: /\p{Script=Greek}+$/u.exec('αβX') === null
                        "aaa", // non-greedy +?$: /a+?$/.exec('aaa')[0]
                        "true" // non-greedy +?$ no match: /a+?$/.exec('aaab') === null
                        );
        Utils.assertWithAllModes_ES6(
                expected,
                "["
                        + "/\\p{Script=Greek}+$/u.exec('\u03B1\u03B2\u03B3')[0],"
                        + "/\\p{Script=Greek}+$/u.exec('\u03B1\u03B2X') === null,"
                        + "/a+?$/.exec('aaa')[0],"
                        + "/a+?$/.exec('aaab') === null"
                        + "].join('-')");
    }

    @Test
    public void atomicQuantifierOptimization_multilineMode() {
        // Multiline mode: atomic optimization is disabled because $ matches at line boundaries
        String expected =
                String.join(
                        "-",
                        "aa", // simple multiline: /a*$/m.exec('aa\na')[0]
                        "0", // simple multiline index
                        "a", // char class with newline: /[a\n]*$/m.exec('a\nb')[0]
                        "0", // char class with newline index
                        "a" // dot no dotall: /.*$/m.exec('a\nb')[0]
                        );
        Utils.assertWithAllModes_ES6(
                expected,
                "["
                        + "/a*$/m.exec('aa\\na')[0],"
                        + "/a*$/m.exec('aa\\na').index,"
                        + "/[a\\n]*$/m.exec('a\\nb')[0],"
                        + "/[a\\n]*$/m.exec('a\\nb').index,"
                        + "/.*$/m.exec('a\\nb')[0]"
                        + "].join('-')");
    }

    @Test
    public void atomicQuantifierOptimization_dotAllMode() {
        // dotAll mode tests
        String expected =
                String.join(
                        "-",
                        "a\nb", // multiline dotall: /.*$/ms.exec('a\nb')[0]
                        "0", // multiline dotall index
                        "a\nb\nc", // multiline dotall full: /.*$/ms.exec('a\nb\nc')[0]
                        "a", // dotall char class needs backtrack: /[a\n]*$/ms.exec('a\nb')[0]
                        "a\nb", // dotall single-line: /.*$/s.exec('a\nb')[0]
                        "0", // dotall single-line index
                        "a\nb" // single-line char class: /[a\nb]*$/s.exec('a\nb')[0]
                        );
        Utils.assertWithAllModes_ES6(
                expected,
                "["
                        + "/.*$/ms.exec('a\\nb')[0],"
                        + "/.*$/ms.exec('a\\nb').index,"
                        + "/.*$/ms.exec('a\\nb\\nc')[0],"
                        + "/[a\\n]*$/ms.exec('a\\nb')[0],"
                        + "/.*$/s.exec('a\\nb')[0],"
                        + "/.*$/s.exec('a\\nb').index,"
                        + "/[a\\nb]*$/s.exec('a\\nb')[0]"
                        + "].join('-')");
    }

    @Test
    public void testUnicodeCaseInsensitiveWordCharacter() {
        // İ (U+0130) has no simple case fold, so it does not match \w
        // Only K (Kelvin sign U+212A) matches because it folds to K which is in \w
        // The match returns the original Kelvin sign character, not ASCII 'K'
        Utils.assertWithAllModes_ES6("\u212A", "'\\u{212A}\\u{0130}'.match(/\\w+/ui)[0]");
    }

    @Test
    public void testUnicodeCaseInsensitiveFlatMatching() {
        // İ (U+0130) has no simple case fold, so /ki$/ui does NOT match 'Kİ'
        Utils.assertWithAllModes_ES6(
                "true-true-true-false",
                "/cAfé$/ui.test('CAFÉ') + '-' + "
                        + "/straße$/ui.test('Straße') + '-' + "
                        + "/ελληνικά$/ui.test('ΕΛΛΗΝΙΚΆ') + '-' + "
                        + "/ki$/ui.test('Kİ')");
    }

    @Test
    public void testUnicodeCaseInsensitiveBackwardFlatMatching() {
        // İ (U+0130) has no simple case fold, so İ does NOT match i in lookbehind
        Utils.assertWithAllModes_ES6(false, "/(?<=Kİ)1/ui.test('ki1')");
    }

    // --- Case Folding Tests ---

    @Test
    public void testUnicodeCaseFoldLongS() {
        // ſ (U+017F) should match S and s
        Utils.assertWithAllModes_ES6(
                "true-true-true",
                "/^\\u{017F}$/ui.test('s') + '-' + "
                        + "/^\\u{017F}$/ui.test('S') + '-' + "
                        + "/^s$/ui.test('\\u{017F}')");
    }

    @Test
    public void testUnicodeCaseFoldTurkishINoMatch() {
        // İ (U+0130) should NOT match ASCII i or I
        // ı (U+0131) should NOT match ASCII i or I
        Utils.assertWithAllModes_ES6(
                "false-false-false-false",
                "/^\\u{0130}$/ui.test('i') + '-' + "
                        + "/^\\u{0130}$/ui.test('I') + '-' + "
                        + "/^\\u{0131}$/ui.test('i') + '-' + "
                        + "/^\\u{0131}$/ui.test('I')");
    }

    @Test
    public void testUnicodeCaseFoldLatinLigaturesST() {
        // U+FB05 (ſt) and U+FB06 (st) should match each other via case folding through 'S'
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/^\\u{FB05}$/ui.test('\\u{FB06}') + '-' + " + "/^\\u{FB06}$/ui.test('\\u{FB05}')");
    }

    // --- Word Character Tests ---

    @Test
    public void testUnicodeCaseInsensitiveNonWordCharacter() {
        // \W should NOT match Kelvin sign since it folds to 'K' (a word char)
        // \W SHOULD match İ since it has no simple case fold
        Utils.assertWithAllModes_ES6(
                "false-true",
                "/^\\W$/ui.test('\\u{212A}') + '-' + " + "/^\\W$/ui.test('\\u{0130}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveWordBoundary() {
        // Word boundary before Kelvin sign
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/\\b\\u{212A}$/ui.test(' \\u{212A}') + '-' + " + "/\\bk$/ui.test(' \\u{212A}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveNonWordBoundary() {
        // \B inside words with Kelvin sign
        Utils.assertWithAllModes_ES6("true", "'' + /a\\Bk$/ui.test('a\\u{212A}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveWordBoundaryWithLongS() {
        // ſ (U+017F) folds to 's' which is a word char
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/\\w$/ui.test('\\u{017F}') + '-' + " + "/\\b\\u{017F}$/ui.test(' \\u{017F}')");
    }

    // --- Non-BMP Character Tests ---

    @Test
    public void testUnicodeCaseInsensitiveNonBMPDeseret() {
        // DESERET CAPITAL LETTER LONG I (U+10400) -> lowercase (U+10428)
        Utils.assertWithAllModes_ES6(
                "true-true-true",
                "/^\\u{10400}$/ui.test('\\u{10428}') + '-' + "
                        + "/^\\u{10428}$/ui.test('\\u{10400}') + '-' + "
                        + "/^\\u{10400}\\u{10401}$/ui.test('\\u{10428}\\u{10429}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveNonBMPAdlam() {
        // ADLAM CAPITAL LETTER ALIF (U+1E900) -> lowercase (U+1E922)
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/^\\u{1E900}$/ui.test('\\u{1E922}') + '-' + "
                        + "/^\\u{1E922}$/ui.test('\\u{1E900}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveNonBMPOsage() {
        // OSAGE CAPITAL LETTER A (U+104B0) -> lowercase (U+104D8)
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/^\\u{104B0}$/ui.test('\\u{104D8}') + '-' + "
                        + "/^\\u{104D8}$/ui.test('\\u{104B0}')");
    }

    // --- Anchor Optimization Tests ---

    @Test
    public void testUnicodeCaseInsensitiveAnchorOptimization() {
        // Test that anchor optimization works with case folding
        Utils.assertWithAllModes_ES6(
                "true-true-true",
                "/\\u{212A}$/ui.test('k') + '-' + "
                        + "/K$/ui.test('\\u{212A}') + '-' + "
                        + "/\\u{10400}$/ui.test('\\u{10428}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveAnchorWithPrefix() {
        // Test anchor optimization with preceding literal text
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/abc\\u{212A}$/ui.test('abck') + '-' + " + "/abcK$/ui.test('abc\\u{212A}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveNonBMPAnchor() {
        // Non-BMP literal as first char exercises REOP_UCSPFLAT1i anchor optimization
        Utils.assertWithAllModes_ES6(
                "true-true-false",
                "/\\u{10400}abc/ui.test('xyz\\u{10428}abc') + '-' + "
                        + "/\\u{10400}abc/ui.test('\\u{10428}abc') + '-' + "
                        + "/\\u{10400}abc/ui.test('xyzabc')");
    }

    // --- Flat Matcher Tests ---

    @Test
    public void testUnicodeCaseInsensitiveFlatMixedBMPNonBMP() {
        // Mix BMP case-folding chars with non-BMP
        // Note: ß does NOT match SS because ß has no simple case fold in Unicode
        Utils.assertWithAllModes_ES6(
                "true-false",
                "/café\\u{10400}$/ui.test('CAFÉ\\u{10428}') + '-' + "
                        + "/straße\\u{10400}$/ui.test('STRASSE\\u{10428}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveLookbehindWithNonBMP() {
        // Lookbehind with non-BMP case-insensitive matching
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/(?<=\\u{10400})x$/ui.test('\\u{10428}x') + '-' + "
                        + "/(?<=AB\\u{10400})x$/ui.test('ab\\u{10428}x')");
    }

    @Test
    public void testUnicodeCaseInsensitiveFlatKelvinInMiddle() {
        // Flat matching with Kelvin sign in middle of string
        Utils.assertWithAllModes_ES6(
                "true-true",
                "/a\\u{212A}b$/ui.test('akb') + '-' + " + "/aKb$/ui.test('a\\u{212A}b')");
    }

    @Test
    public void testUnicodeCaseInsensitiveQuantifierWithFolding() {
        // Quantifiers with case folding
        Utils.assertWithAllModes_ES6(
                "true-true", "/^K+$/ui.test('kKk') + '-' + " + "/^K{2}$/ui.test('k\\u{212A}')");
    }

    @Test
    public void testUnicodeCaseInsensitiveLookahead() {
        // Lookahead with case folding
        Utils.assertWithAllModes_ES6(
                "true-true", "/^a(?=K)/ui.test('ak') + '-' + " + "/^a(?!K)/ui.test('ax')");
    }

    @Test
    public void testUnicodeCaseInsensitiveNegativeLookbehind() {
        // Negative lookbehind with case folding
        Utils.assertWithAllModes_ES6(
                "true-false", "/(?<!K)x$/ui.test('ax') + '-' + " + "/(?<!K)x$/ui.test('kx')");
    }
}
