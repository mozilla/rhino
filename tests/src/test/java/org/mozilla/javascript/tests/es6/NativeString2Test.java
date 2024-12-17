/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.Context;
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
        Utils.assertWithAllOptimizationLevelsES6("true;h;false;true;false;", js);
    }

    @Test
    public void normalizeNoParam() {
        Utils.assertWithAllOptimizationLevels("123", "'123'.normalize()");
    }

    @Test
    public void normalizeNoUndefined() {
        Utils.assertWithAllOptimizationLevels("123", "'123'.normalize(undefined)");
    }

    @Test
    public void normalizeNoNull() {
        String js = "try { " + "  '123'.normalize(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllOptimizationLevels(
                "The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.", js);
    }

    @Test
    public void replaceReplacementAsString() {
        Utils.assertWithAllOptimizationLevels(
                Context.VERSION_DEFAULT, "1null3", "'123'.replace('2', /x/);");
        Utils.assertWithAllOptimizationLevelsES6("1/x/3", "'123'.replace('2', /x/);");
    }

    @Test
    public void indexOfEmpty() {
        Utils.assertWithAllOptimizationLevels(0, "'1234'.indexOf('', 0);");
        Utils.assertWithAllOptimizationLevels(1, "'1234'.indexOf('', 1);");
        Utils.assertWithAllOptimizationLevels(4, "'1234'.indexOf('', 4);");
        Utils.assertWithAllOptimizationLevels(4, "'1234'.indexOf('', 5);");
        Utils.assertWithAllOptimizationLevels(4, "'1234'.indexOf('', 42);");
    }

    @Test
    public void includesEmpty() {
        Utils.assertWithAllOptimizationLevels(true, "'1234'.includes('');");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.includes('', 0);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.includes('', 1);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.includes('', 4);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.includes('', 5);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.includes('', 42);");
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

        Utils.assertWithAllOptimizationLevelsES6(
                "TypeError: First argument to String.prototype.includes must not be a regular expression # true",
                js);
    }

    @Test
    public void startsWithEmpty() {
        Utils.assertWithAllOptimizationLevels(true, "'1234'.startsWith('');");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.startsWith('', 0);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.startsWith('', 1);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.startsWith('', 4);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.startsWith('', 5);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.startsWith('', 42);");
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

        Utils.assertWithAllOptimizationLevelsES6(
                "TypeError: First argument to String.prototype.startsWith must not be a regular expression # true",
                js);
    }

    @Test
    public void endsWithEmpty() {
        Utils.assertWithAllOptimizationLevels(true, "'1234'.endsWith('');");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.endsWith('', 0);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.endsWith('', 1);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.endsWith('', 4);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.endsWith('', 5);");
        Utils.assertWithAllOptimizationLevels(true, "'1234'.endsWith('', 42);");
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

        Utils.assertWithAllOptimizationLevelsES6(
                "TypeError: First argument to String.prototype.startsWith must not be a regular expression # true",
                js);
    }

    @Test
    public void tagify() {
        Utils.assertWithAllOptimizationLevels("<big>tester</big>", "'tester'.big()");
        Utils.assertWithAllOptimizationLevels("<big>\"tester\"</big>", "'\"tester\"'.big()");
        Utils.assertWithAllOptimizationLevels(
                "<font size=\"undefined\">tester</font>", "'tester'.fontsize()");
        Utils.assertWithAllOptimizationLevels(
                "<font size=\"null\">tester</font>", "'tester'.fontsize(null)");
        Utils.assertWithAllOptimizationLevels(
                "<font size=\"undefined\">tester</font>", "'tester'.fontsize(undefined)");
        Utils.assertWithAllOptimizationLevels(
                "<font size=\"123\">tester</font>", "'tester'.fontsize(123)");
        Utils.assertWithAllOptimizationLevels(
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

            Utils.assertWithAllOptimizationLevelsES6(expected, js);
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

            Utils.assertWithAllOptimizationLevelsES6(expected, js);
        }
    }

    @Test
    public void stringReplace() {
        Utils.assertWithAllOptimizationLevels("xyz", "''.replace('', 'xyz')");
        Utils.assertWithAllOptimizationLevels("1", "'121'.replace('21', '')");
        Utils.assertWithAllOptimizationLevels("xyz121", "'121'.replace('', 'xyz')");
        Utils.assertWithAllOptimizationLevels("a$c21", "'121'.replace('1', 'a$c')");
        Utils.assertWithAllOptimizationLevels("a121", "'121'.replace('1', 'a$&')");
        Utils.assertWithAllOptimizationLevels("a$c21", "'121'.replace('1', 'a$$c')");
        Utils.assertWithAllOptimizationLevels("abaabe", "'abcde'.replace('cd', 'a$`')");
        Utils.assertWithAllOptimizationLevels("a21", "'121'.replace('1', 'a$`')");
        Utils.assertWithAllOptimizationLevels("abaee", "'abcde'.replace('cd', \"a$'\")");
        Utils.assertWithAllOptimizationLevels("aba", "'abcd'.replace('cd', \"a$'\")");
        Utils.assertWithAllOptimizationLevels("aba$0", "'abcd'.replace('cd', 'a$0')");
        Utils.assertWithAllOptimizationLevels("aba$1", "'abcd'.replace('cd', 'a$1')");
        Utils.assertWithAllOptimizationLevels(
                "abCD",
                "'abcd'.replace('cd', function (matched) { return matched.toUpperCase() })");
        Utils.assertWithAllOptimizationLevels("", "'123456'.replace(/\\d+/, '')");
        Utils.assertWithAllOptimizationLevels(
                "123ABCD321abcd",
                "'123abcd321abcd'.replace(/[a-z]+/, function (matched) { return matched.toUpperCase() })");
    }

    @Test
    public void stringReplaceAll() {
        Utils.assertWithAllOptimizationLevels("xyz", "''.replaceAll('', 'xyz')");
        Utils.assertWithAllOptimizationLevels("1", "'12121'.replaceAll('21', '')");
        Utils.assertWithAllOptimizationLevels("xyz1xyz2xyz1xyz", "'121'.replaceAll('', 'xyz')");
        Utils.assertWithAllOptimizationLevels("a$c2a$c", "'121'.replaceAll('1', 'a$c')");
        Utils.assertWithAllOptimizationLevels("a12a1", "'121'.replaceAll('1', 'a$&')");
        Utils.assertWithAllOptimizationLevels("a$c2a$c", "'121'.replaceAll('1', 'a$$c')");
        Utils.assertWithAllOptimizationLevels("aaadaaabcda", "'abcdabc'.replaceAll('bc', 'a$`')");
        Utils.assertWithAllOptimizationLevels("a2a12", "'121'.replaceAll('1', 'a$`')");
        Utils.assertWithAllOptimizationLevels("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        Utils.assertWithAllOptimizationLevels("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        Utils.assertWithAllOptimizationLevels("aa$0daa$0", "'abcdabc'.replaceAll('bc', 'a$0')");
        Utils.assertWithAllOptimizationLevels("aa$1daa$1", "'abcdabc'.replaceAll('bc', 'a$1')");
        Utils.assertWithAllOptimizationLevels("", "'123456'.replaceAll(/\\d+/g, '')");
        Utils.assertWithAllOptimizationLevels("123456", "'123456'.replaceAll(undefined, '')");
        Utils.assertWithAllOptimizationLevels("afoobarb", "'afoob'.replaceAll(/(foo)/g, '$1bar')");
        Utils.assertWithAllOptimizationLevels("foobarb", "'foob'.replaceAll(/(foo)/gy, '$1bar')");
        Utils.assertWithAllOptimizationLevels("hllo", "'hello'.replaceAll(/(h)e/gy, '$1')");
        Utils.assertWithAllOptimizationLevels("$1llo", "'hello'.replaceAll(/he/g, '$1')");
        Utils.assertWithAllOptimizationLevels(
                "I$want$these$periods$to$be$$s",
                "'I.want.these.periods.to.be.$s'.replaceAll(/\\./g, '$')");
        Utils.assertWithAllOptimizationLevels("food bar", "'foo bar'.replaceAll(/foo/g, '$&d')");
        Utils.assertWithAllOptimizationLevels("foo foo ", "'foo bar'.replaceAll(/bar/g, '$`')");
        Utils.assertWithAllOptimizationLevels(" bar bar", "'foo bar'.replaceAll(/foo/g, '$\\'')");
        Utils.assertWithAllOptimizationLevels("$' bar", "'foo bar'.replaceAll(/foo/g, '$$\\'')");
        Utils.assertWithAllOptimizationLevels("ad$0db", "'afoob'.replaceAll(/(foo)/g, 'd$0d')");
        Utils.assertWithAllOptimizationLevels(
                "ad$0db", "'afkxxxkob'.replace(/(f)k(.*)k(o)/g, 'd$0d')");
        Utils.assertWithAllOptimizationLevels(
                "ad$0dbd$0dc", "'afoobfuoc'.replaceAll(/(f.o)/g, 'd$0d')");
        Utils.assertWithAllOptimizationLevels(
                "123FOOBAR321BARFOO123",
                "'123foobar321barfoo123'.replace(/[a-z]+/g, function (matched) { return matched.toUpperCase() })");

        Utils.assertWithAllOptimizationLevels(
                "TypeError: replaceAll must be called with a global RegExp",
                "try { 'hello'.replaceAll(/he/i, 'x'); } catch (e) { '' + e }");
    }
}
