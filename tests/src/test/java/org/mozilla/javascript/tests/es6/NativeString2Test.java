/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tests.Utils;

/** Test for handling const variables. */
public class NativeString2Test {

    @Test
    public void getOwnPropertyDescriptorWithIndex() {
        String js =
                "  var res = 'hello'.hasOwnProperty('0');"
                        + "  res += ';';"
                        + "  desc = Object.getOwnPropertyDescriptor('hello', '0');"
                        + "  res += desc.value;"
                        + "  res += ';';"
                        + "  res += desc.writable;"
                        + "  res += ';';"
                        + "  res += desc.enumerable;"
                        + "  res += ';';"
                        + "  res += desc.configurable;"
                        + "  res += ';';"
                        + "  res;";
        assertEvaluatesES6("true;h;false;true;false;", js);
    }

    @Test
    public void normalizeNoParam() {
        assertEvaluates("123", "'123'.normalize()");
    }

    @Test
    public void normalizeNoUndefined() {
        assertEvaluates("123", "'123'.normalize(undefined)");
    }

    @Test
    public void normalizeNoNull() {
        String js = "try { " + "  '123'.normalize(null);" + "} catch (e) { e.message }";
        assertEvaluates(
                "The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.", js);
    }

    @Test
    public void replaceReplacementAsString() {
        assertEvaluates("1null3", "'123'.replace('2', /x/);");
        assertEvaluatesES6("1/x/3", "'123'.replace('2', /x/);");
    }

    @Test
    public void indexOfEmpty() {
        assertEvaluates(0, "'1234'.indexOf('', 0);");
        assertEvaluates(1, "'1234'.indexOf('', 1);");
        assertEvaluates(4, "'1234'.indexOf('', 4);");
        assertEvaluates(4, "'1234'.indexOf('', 5);");
        assertEvaluates(4, "'1234'.indexOf('', 42);");
    }

    @Test
    public void includesEmpty() {
        assertEvaluates(true, "'1234'.includes('');");
        assertEvaluates(true, "'1234'.includes('', 0);");
        assertEvaluates(true, "'1234'.includes('', 1);");
        assertEvaluates(true, "'1234'.includes('', 4);");
        assertEvaluates(true, "'1234'.includes('', 5);");
        assertEvaluates(true, "'1234'.includes('', 42);");
    }

    @Test
    public void includesRegExpMatch() {
        String js =
                "var regExp = /./;\n"
                        + "var res = '';\n"
                        + "try {\n"
                        + "  res += '/./'.includes(regExp);\n"
                        + "} catch (e) {\n"
                        + "  res += e;\n"
                        + "}\n"
                        + "regExp[Symbol.match] = false;\n"
                        + "res += ' # ' + '/./'.includes(regExp);\n"
                        + "res;";

        assertEvaluatesES6(
                "TypeError: First argument to String.prototype.includes must not be a regular expression # true",
                js);
    }

    @Test
    public void startsWithEmpty() {
        assertEvaluates(true, "'1234'.startsWith('');");
        assertEvaluates(true, "'1234'.startsWith('', 0);");
        assertEvaluates(true, "'1234'.startsWith('', 1);");
        assertEvaluates(true, "'1234'.startsWith('', 4);");
        assertEvaluates(true, "'1234'.startsWith('', 5);");
        assertEvaluates(true, "'1234'.startsWith('', 42);");
    }

    @Test
    public void startsWithRegExpMatch() {
        String js =
                "var regExp = /./;\n"
                        + "var res = '';\n"
                        + "try {\n"
                        + "  res += '/./'.startsWith(regExp);\n"
                        + "} catch (e) {\n"
                        + "  res += e;\n"
                        + "}\n"
                        + "regExp[Symbol.match] = false;\n"
                        + "res += ' # ' + '/./'.includes(regExp);\n"
                        + "res;";

        assertEvaluatesES6(
                "TypeError: First argument to String.prototype.startsWith must not be a regular expression # true",
                js);
    }

    @Test
    public void endsWithEmpty() {
        assertEvaluates(true, "'1234'.endsWith('');");
        assertEvaluates(true, "'1234'.endsWith('', 0);");
        assertEvaluates(true, "'1234'.endsWith('', 1);");
        assertEvaluates(true, "'1234'.endsWith('', 4);");
        assertEvaluates(true, "'1234'.endsWith('', 5);");
        assertEvaluates(true, "'1234'.endsWith('', 42);");
    }

    @Test
    public void endsWithRegExpMatch() {
        String js =
                "var regExp = /./;\n"
                        + "var res = '';\n"
                        + "try {\n"
                        + "  res += '/./'.startsWith(regExp);\n"
                        + "} catch (e) {\n"
                        + "  res += e;\n"
                        + "}\n"
                        + "regExp[Symbol.match] = false;\n"
                        + "res += ' # ' + '/./'.includes(regExp);\n"
                        + "res;";

        assertEvaluatesES6(
                "TypeError: First argument to String.prototype.startsWith must not be a regular expression # true",
                js);
    }

    @Test
    public void tagify() {
        assertEvaluates("<big>tester</big>", "'tester'.big()");
        assertEvaluates("<big>\"tester\"</big>", "'\"tester\"'.big()");
        assertEvaluates("<font size=\"undefined\">tester</font>", "'tester'.fontsize()");
        assertEvaluates("<font size=\"null\">tester</font>", "'tester'.fontsize(null)");
        assertEvaluates("<font size=\"undefined\">tester</font>", "'tester'.fontsize(undefined)");
        assertEvaluates("<font size=\"123\">tester</font>", "'tester'.fontsize(123)");
        assertEvaluates(
                "<font size=\"&quot;123&quot;\">tester</font>", "'tester'.fontsize('\"123\"')");
    }

    @Test
    public void tagifyPrototypeNull() {
        for (String call :
                new String[] {
                    "big",
                    "blink",
                    "bold",
                    "fixed",
                    "fontcolor",
                    "fontsize",
                    "italics",
                    "link",
                    "small",
                    "strike",
                    "sub",
                    "sup"
                }) {
            String js = "try { String.prototype." + call + ".call(null);} catch (e) { e.message }";
            String expected = "String.prototype." + call + " method called on null or undefined";

            assertEvaluatesES6(expected, js);
        }
    }

    @Test
    public void tagifyPrototypeUndefined() {
        for (String call :
                new String[] {
                    "big",
                    "blink",
                    "bold",
                    "fixed",
                    "fontcolor",
                    "fontsize",
                    "italics",
                    "link",
                    "small",
                    "strike",
                    "sub",
                    "sup"
                }) {
            String js =
                    "try { String.prototype." + call + ".call(undefined);} catch (e) { e.message }";
            String expected = "String.prototype." + call + " method called on null or undefined";

            assertEvaluatesES6(expected, js);
        }
    }

    @Test
    public void stringReplace() {
        assertEvaluates("xyz", "''.replace('', 'xyz')");
        assertEvaluates("1", "'121'.replace('21', '')");
        assertEvaluates("xyz121", "'121'.replace('', 'xyz')");
        assertEvaluates("a$c21", "'121'.replace('1', 'a$c')");
        assertEvaluates("a121", "'121'.replace('1', 'a$&')");
        assertEvaluates("a$c21", "'121'.replace('1', 'a$$c')");
        assertEvaluates("abaabe", "'abcde'.replace('cd', 'a$`')");
        assertEvaluates("a21", "'121'.replace('1', 'a$`')");
        assertEvaluates("abaee", "'abcde'.replace('cd', \"a$'\")");
        assertEvaluates("aba", "'abcd'.replace('cd', \"a$'\")");
        assertEvaluates("aba$0", "'abcd'.replace('cd', 'a$0')");
        assertEvaluates("aba$1", "'abcd'.replace('cd', 'a$1')");
        assertEvaluates(
                "abCD",
                "'abcd'.replace('cd', function (matched) { return matched.toUpperCase() })");
        assertEvaluates("", "'123456'.replace(/\\d+/, '')");
        assertEvaluates(
                "123ABCD321abcd",
                "'123abcd321abcd'.replace(/[a-z]+/, function (matched) { return matched.toUpperCase() })");
    }

    @Test
    public void stringReplaceAll() {
        assertEvaluates("xyz", "''.replaceAll('', 'xyz')");
        assertEvaluates("1", "'12121'.replaceAll('21', '')");
        assertEvaluates("xyz1xyz2xyz1xyz", "'121'.replaceAll('', 'xyz')");
        assertEvaluates("a$c2a$c", "'121'.replaceAll('1', 'a$c')");
        assertEvaluates("a12a1", "'121'.replaceAll('1', 'a$&')");
        assertEvaluates("a$c2a$c", "'121'.replaceAll('1', 'a$$c')");
        assertEvaluates("aaadaaabcda", "'abcdabc'.replaceAll('bc', 'a$`')");
        assertEvaluates("a2a12", "'121'.replaceAll('1', 'a$`')");
        assertEvaluates("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        assertEvaluates("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        assertEvaluates("aa$0daa$0", "'abcdabc'.replaceAll('bc', 'a$0')");
        assertEvaluates("aa$1daa$1", "'abcdabc'.replaceAll('bc', 'a$1')");
        assertEvaluates("", "'123456'.replaceAll(/\\d+/g, '')");
        assertEvaluates("123456", "'123456'.replaceAll(undefined, '')");
        assertEvaluates("afoobarb", "'afoob'.replaceAll(/(foo)/g, '$1bar')");
        assertEvaluates("foobarb", "'foob'.replaceAll(/(foo)/gy, '$1bar')");
        assertEvaluates("hllo", "'hello'.replaceAll(/(h)e/gy, '$1')");
        assertEvaluates("$1llo", "'hello'.replaceAll(/he/g, '$1')");
        assertEvaluates(
                "I$want$these$periods$to$be$$s",
                "'I.want.these.periods.to.be.$s'.replaceAll(/\\./g, '$')");
        assertEvaluates("food bar", "'foo bar'.replaceAll(/foo/g, '$&d')");
        assertEvaluates("foo foo ", "'foo bar'.replaceAll(/bar/g, '$`')");
        assertEvaluates(" bar bar", "'foo bar'.replaceAll(/foo/g, '$\\'')");
        assertEvaluates("$' bar", "'foo bar'.replaceAll(/foo/g, '$$\\'')");
        assertEvaluates("ad$0db", "'afoob'.replaceAll(/(foo)/g, 'd$0d')");
        assertEvaluates("ad$0db", "'afkxxxkob'.replace(/(f)k(.*)k(o)/g, 'd$0d')");
        assertEvaluates("ad$0dbd$0dc", "'afoobfuoc'.replaceAll(/(f.o)/g, 'd$0d')");
        assertEvaluates(
                "123FOOBAR321BARFOO123",
                "'123foobar321barfoo123'.replace(/[a-z]+/g, function (matched) { return matched.toUpperCase() })");

        assertEvaluates(
                "TypeError: replaceAll must be called with a global RegExp",
                "try { 'hello'.replaceAll(/he/i, 'x'); } catch (e) { '' + e }");
    }

    private static void assertEvaluates(final Object expected, final String source) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                    assertEquals(expected, rep);
                    return null;
                });
    }

    private static void assertEvaluatesES6(final Object expected, final String source) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();
                    final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                    assertEquals(expected, rep);
                    return null;
                });
    }
}
