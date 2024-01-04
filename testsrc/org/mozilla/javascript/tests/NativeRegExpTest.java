/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class NativeRegExpTest {

    @Test
    public void openBrace() {
        final String script = "/0{0/";
        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    final ScriptableObject scope = _cx.initStandardObjects();
                    final Object result = _cx.evaluateString(scope, script, "test script", 0, null);
                    assertEquals(script, Context.toString(result));
                    return null;
                });
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalCtor() throws Exception {
        testEvaluate("g-true-false-false-false-false", "new RegExp('foo', 'g');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void global() throws Exception {
        testEvaluate("g-true-false-false-false-false", "/foo/g;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCaseCtor() throws Exception {
        testEvaluate("i-false-true-false-false-false", "new RegExp('foo', 'i');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCase() throws Exception {
        testEvaluate("i-false-true-false-false-false", "/foo/i;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void multilineCtor() throws Exception {
        testEvaluate("m-false-false-true-false-false", "new RegExp('foo', 'm');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void multiline() throws Exception {
        testEvaluate("m-false-false-true-false-false", "/foo/m;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void dotAllCtor() throws Exception {
        testEvaluate("s-false-false-false-true-false", "new RegExp('foo', 's');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void dotAll() throws Exception {
        testEvaluate("s-false-false-false-true-false", "/foo/s;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void stickyCtor() throws Exception {
        testEvaluate("y-false-false-false-false-true", "new RegExp('foo', 'y');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void sticky() throws Exception {
        testEvaluate("y-false-false-false-false-true", "/foo/y;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalMultilineCtor() throws Exception {
        testEvaluate("gm-true-false-true-false-false", "new RegExp('foo', 'gm');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalMultiline() throws Exception {
        testEvaluate("gm-true-false-true-false-false", "/foo/gm;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalDotAll() throws Exception {
        testEvaluate("gs-true-false-false-true-false", "/foo/gs;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalIgnoreCaseCtor() throws Exception {
        testEvaluate("gi-true-true-false-false-false", "new RegExp('foo', 'ig');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalIgnoreCase() throws Exception {
        testEvaluate("gi-true-true-false-false-false", "/foo/ig;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalStickyCtor() throws Exception {
        testEvaluate("gy-true-false-false-false-true", "new RegExp('foo', 'gy');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalSticky() throws Exception {
        testEvaluate("gy-true-false-false-false-true", "/foo/gy;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalMultilineIgnoreCaseCtor() throws Exception {
        testEvaluate("gim-true-true-true-false-false", "new RegExp('foo', 'mig');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalMultilineIgnoreCase() throws Exception {
        testEvaluate("gim-true-true-true-false-false", "/foo/gmi;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalDotAllIgnoreCaseCtor() throws Exception {
        testEvaluate("gis-true-true-false-true-false", "new RegExp('foo', 'gsi');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalDotAllIgnoreCase() throws Exception {
        testEvaluate("gis-true-true-false-true-false", "/foo/gsi;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalIgnoreCaseStickyCtor() throws Exception {
        testEvaluate("giy-true-true-false-false-true", "new RegExp('foo', 'yig');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalIgnoreCaseSticky() throws Exception {
        testEvaluate("giy-true-true-false-false-true", "/foo/ygi;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalMultilineStickyCtor() throws Exception {
        testEvaluate("gmy-true-false-true-false-true", "new RegExp('foo', 'gmy');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalMultilineSticky() throws Exception {
        testEvaluate("gmy-true-false-true-false-true", "/foo/gmy;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalDotAllStickyCtor() throws Exception {
        testEvaluate("gsy-true-false-false-true-true", "new RegExp('foo', 'gys');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void globalDotAllSticky() throws Exception {
        testEvaluate("gsy-true-false-false-true-true", "/foo/gys;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCaseMultilineCtor() throws Exception {
        testEvaluate("im-false-true-true-false-false", "new RegExp('foo', 'im');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCaseMultiline() throws Exception {
        testEvaluate("im-false-true-true-false-false", "/foo/mi;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCaseDotAllCtor() throws Exception {
        testEvaluate("is-false-true-false-true-false", "new RegExp('foo', 'si');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCaseDotAll() throws Exception {
        testEvaluate("is-false-true-false-true-false", "/foo/si;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCaseStickyCtor() throws Exception {
        testEvaluate("iy-false-true-false-false-true", "new RegExp('foo', 'yi');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void ignoreCaseSticky() throws Exception {
        testEvaluate("iy-false-true-false-false-true", "/foo/iy;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void multilineStickyCtor() throws Exception {
        testEvaluate("my-false-false-true-false-true", "new RegExp('foo', 'my');");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void multilineSticky() throws Exception {
        testEvaluate("my-false-false-true-false-true", "/foo/my;");
    }

    /** @throws Exception if an error occurs */
    @Test
    public void dotAllStickyCtor() throws Exception {
        testEvaluate("sy-false-false-false-true-true", "new RegExp('foo', 'ys');");
    }

    /** @throws Exception if an error occurs */
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

        test(expected, script);
    }

    /** @throws Exception if an error occurs */
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
        test("true-9-false-0-false-0", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void stickyStartOfLine() throws Exception {
        final String script =
                "var regex = /^foo/y;\n"
                        + "regex.lastIndex = 2;\n"
                        + "var res = '' + regex.test('..foo');\n"
                        + "res;";
        test("false", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void stickyStartOfLineMultiline() throws Exception {
        final String script =
                "var regex = /^foo/my;\n"
                        + "regex.lastIndex = 2;\n"
                        + "var res = '' + regex.test('..foo');\n"
                        + "regex.lastIndex = 2;\n"
                        + "res = res + '-' + regex.test('.\\nfoo');\n"
                        + "res;";
        test("false-true", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void matchGlobal() throws Exception {
        final String script =
                "var result = 'aaba'.match(/a/g);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res;";
        test("3-a-a-a", script);
    }

    /** @throws Exception if an error occurs */
    // TODO @Test
    public void matchGlobalSymbol() throws Exception {
        final String script =
                "var result = /a/g[Symbol.match]('aaba');\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res = res + '-' + result[2];\n"
                        + "res;";
        test("3-a-a-a", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void matchDotAll() throws Exception {
        final String script =
                "var result = 'bar\\nfoo'.match(/bar.foo/s);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        test("1-bar\nfoo", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void matchSticky() throws Exception {
        final String script =
                "var result = 'aaba'.match(/a/y);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        test("1-a", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void matchStickySymbol() throws Exception {
        final String script =
                "var result = /a/y[Symbol.match]('aaba');\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res;";
        test("1-a", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void matchStickyAndGlobal() throws Exception {
        final String script =
                "var result = 'aaba'.match(/a/yg);\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res;";
        test("2-a-a", script);
    }

    /** @throws Exception if an error occurs */
    // TODO @Test
    public void matchStickyAndGlobalSymbol() throws Exception {
        final String script =
                "var result = /a/yg[Symbol.match]('aaba');\n"
                        + "var res = '' + result.length;\n"
                        + "res = res + '-' + result[0];\n"
                        + "res = res + '-' + result[1];\n"
                        + "res;";
        test("2-a-a", script);
    }

    /** @throws Exception if an error occurs */
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
        test("0-undefined-true-false-undefined", script);
    }

    /** @throws Exception if an error occurs */
    @Test
    public void objectToString() throws Exception {
        test("/undefined/undefined", "RegExp.prototype.toString.call({})");
        test("/Foo/undefined", "RegExp.prototype.toString.call({source: 'Foo'})");
        test("/undefined/gy", "RegExp.prototype.toString.call({flags: 'gy'})");
        test("/Foo/g", "RegExp.prototype.toString.call({source: 'Foo', flags: 'g'})");
        test("/Foo/g", "RegExp.prototype.toString.call({source: 'Foo', flags: 'g', sticky: true})");

        test(
                "TypeError: Method \"toString\" called on incompatible object",
                "try { RegExp.prototype.toString.call(''); } catch (e) { ('' + e).substr(0, 58) }");
        test(
                "TypeError: Method \"toString\" called on incompatible object",
                "try { RegExp.prototype.toString.call(undefined); } catch (e) { ('' + e).substr(0, 58) }");
        test(
                "TypeError: Method \"toString\" called on incompatible object",
                "var toString = RegExp.prototype.toString; try { toString(); } catch (e) { ('' + e).substr(0, 58) }");
    }

    private static void test(final String expected, final String script) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    // to have symbol available
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();
                    final String res =
                            (String) cx.evaluateString(scope, script, "test.js", 0, null);
                    assertEquals(expected, res);
                    return null;
                });
    }
}
