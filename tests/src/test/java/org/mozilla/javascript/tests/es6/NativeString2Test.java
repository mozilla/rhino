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
        Utils.assertWithAllModes_ES6("true;h;false;true;false;", js);
    }

    @Test
    public void normalizeNoParam() {
        Utils.assertWithAllModes("123", "'123'.normalize()");
    }

    @Test
    public void normalizeNoUndefined() {
        Utils.assertWithAllModes("123", "'123'.normalize(undefined)");
    }

    @Test
    public void normalizeNoNull() {
        String js = "try { " + "  '123'.normalize(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes(
                "The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.", js);
    }

    @Test
    public void replaceReplacementAsString() {
        Utils.assertWithAllModes(
                Context.VERSION_DEFAULT, null, "1null3", "'123'.replace('2', /x/);");
        Utils.assertWithAllModes_ES6("1/x/3", "'123'.replace('2', /x/);");
    }

    @Test
    public void indexOfEmpty() {
        Utils.assertWithAllModes(0, "'1234'.indexOf('', 0);");
        Utils.assertWithAllModes(1, "'1234'.indexOf('', 1);");
        Utils.assertWithAllModes(4, "'1234'.indexOf('', 4);");
        Utils.assertWithAllModes(4, "'1234'.indexOf('', 5);");
        Utils.assertWithAllModes(4, "'1234'.indexOf('', 42);");
    }

    @Test
    public void includesEmpty() {
        Utils.assertWithAllModes(true, "'1234'.includes('');");
        Utils.assertWithAllModes(true, "'1234'.includes('', 0);");
        Utils.assertWithAllModes(true, "'1234'.includes('', 1);");
        Utils.assertWithAllModes(true, "'1234'.includes('', 4);");
        Utils.assertWithAllModes(true, "'1234'.includes('', 5);");
        Utils.assertWithAllModes(true, "'1234'.includes('', 42);");
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

        Utils.assertWithAllModes_ES6(
                "TypeError: First argument to String.prototype.includes must not be a regular expression # true",
                js);
    }

    @Test
    public void startsWithEmpty() {
        Utils.assertWithAllModes(true, "'1234'.startsWith('');");
        Utils.assertWithAllModes(true, "'1234'.startsWith('', 0);");
        Utils.assertWithAllModes(true, "'1234'.startsWith('', 1);");
        Utils.assertWithAllModes(true, "'1234'.startsWith('', 4);");
        Utils.assertWithAllModes(true, "'1234'.startsWith('', 5);");
        Utils.assertWithAllModes(true, "'1234'.startsWith('', 42);");
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

        Utils.assertWithAllModes_ES6(
                "TypeError: First argument to String.prototype.startsWith must not be a regular expression # true",
                js);
    }

    @Test
    public void endsWithEmpty() {
        Utils.assertWithAllModes(true, "'1234'.endsWith('');");
        Utils.assertWithAllModes(true, "'1234'.endsWith('', 0);");
        Utils.assertWithAllModes(true, "'1234'.endsWith('', 1);");
        Utils.assertWithAllModes(true, "'1234'.endsWith('', 4);");
        Utils.assertWithAllModes(true, "'1234'.endsWith('', 5);");
        Utils.assertWithAllModes(true, "'1234'.endsWith('', 42);");
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

        Utils.assertWithAllModes_ES6(
                "TypeError: First argument to String.prototype.startsWith must not be a regular expression # true",
                js);
    }

    @Test
    public void tagify() {
        Utils.assertWithAllModes("<big>tester</big>", "'tester'.big()");
        Utils.assertWithAllModes("<big>\"tester\"</big>", "'\"tester\"'.big()");
        Utils.assertWithAllModes("<font size=\"undefined\">tester</font>", "'tester'.fontsize()");
        Utils.assertWithAllModes("<font size=\"null\">tester</font>", "'tester'.fontsize(null)");
        Utils.assertWithAllModes(
                "<font size=\"undefined\">tester</font>", "'tester'.fontsize(undefined)");
        Utils.assertWithAllModes("<font size=\"123\">tester</font>", "'tester'.fontsize(123)");
        Utils.assertWithAllModes(
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

            Utils.assertWithAllModes_ES6(expected, js);
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

            Utils.assertWithAllModes_ES6(expected, js);
        }
    }

    @Test
    public void stringReplace() {
        Utils.assertWithAllModes("xyz", "''.replace('', 'xyz')");
        Utils.assertWithAllModes("1", "'121'.replace('21', '')");
        Utils.assertWithAllModes("xyz121", "'121'.replace('', 'xyz')");
        Utils.assertWithAllModes("a$c21", "'121'.replace('1', 'a$c')");
        Utils.assertWithAllModes("a121", "'121'.replace('1', 'a$&')");
        Utils.assertWithAllModes("a$c21", "'121'.replace('1', 'a$$c')");
        Utils.assertWithAllModes("abaabe", "'abcde'.replace('cd', 'a$`')");
        Utils.assertWithAllModes("a21", "'121'.replace('1', 'a$`')");
        Utils.assertWithAllModes("abaee", "'abcde'.replace('cd', \"a$'\")");
        Utils.assertWithAllModes("aba", "'abcd'.replace('cd', \"a$'\")");
        Utils.assertWithAllModes("aba$0", "'abcd'.replace('cd', 'a$0')");
        Utils.assertWithAllModes("aba$1", "'abcd'.replace('cd', 'a$1')");
        Utils.assertWithAllModes(
                "abCD",
                "'abcd'.replace('cd', function (matched) { return matched.toUpperCase() })");
        Utils.assertWithAllModes("", "'123456'.replace(/\\d+/, '')");
        Utils.assertWithAllModes(
                "123ABCD321abcd",
                "'123abcd321abcd'.replace(/[a-z]+/, function (matched) { return matched.toUpperCase() })");
    }

    @Test
    public void stringReplaceAll() {
        Utils.assertWithAllModes("xyz", "''.replaceAll('', 'xyz')");
        Utils.assertWithAllModes("1", "'12121'.replaceAll('21', '')");
        Utils.assertWithAllModes("xyz1xyz2xyz1xyz", "'121'.replaceAll('', 'xyz')");
        Utils.assertWithAllModes("a$c2a$c", "'121'.replaceAll('1', 'a$c')");
        Utils.assertWithAllModes("a12a1", "'121'.replaceAll('1', 'a$&')");
        Utils.assertWithAllModes("a$c2a$c", "'121'.replaceAll('1', 'a$$c')");
        Utils.assertWithAllModes("aaadaaabcda", "'abcdabc'.replaceAll('bc', 'a$`')");
        Utils.assertWithAllModes("a2a12", "'121'.replaceAll('1', 'a$`')");
        Utils.assertWithAllModes("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        Utils.assertWithAllModes("aadabcdaa", "'abcdabc'.replaceAll('bc', \"a$'\")");
        Utils.assertWithAllModes("aa$0daa$0", "'abcdabc'.replaceAll('bc', 'a$0')");
        Utils.assertWithAllModes("aa$1daa$1", "'abcdabc'.replaceAll('bc', 'a$1')");
        Utils.assertWithAllModes("", "'123456'.replaceAll(/\\d+/g, '')");
        Utils.assertWithAllModes("123456", "'123456'.replaceAll(undefined, '')");
        Utils.assertWithAllModes("afoobarb", "'afoob'.replaceAll(/(foo)/g, '$1bar')");
        Utils.assertWithAllModes("foobarb", "'foob'.replaceAll(/(foo)/gy, '$1bar')");
        Utils.assertWithAllModes("hllo", "'hello'.replaceAll(/(h)e/gy, '$1')");
        Utils.assertWithAllModes("$1llo", "'hello'.replaceAll(/he/g, '$1')");
        Utils.assertWithAllModes(
                "I$want$these$periods$to$be$$s",
                "'I.want.these.periods.to.be.$s'.replaceAll(/\\./g, '$')");
        Utils.assertWithAllModes("food bar", "'foo bar'.replaceAll(/foo/g, '$&d')");
        Utils.assertWithAllModes("foo foo ", "'foo bar'.replaceAll(/bar/g, '$`')");
        Utils.assertWithAllModes(" bar bar", "'foo bar'.replaceAll(/foo/g, '$\\'')");
        Utils.assertWithAllModes("$' bar", "'foo bar'.replaceAll(/foo/g, '$$\\'')");
        Utils.assertWithAllModes("ad$0db", "'afoob'.replaceAll(/(foo)/g, 'd$0d')");
        Utils.assertWithAllModes("ad$0db", "'afkxxxkob'.replace(/(f)k(.*)k(o)/g, 'd$0d')");
        Utils.assertWithAllModes("ad$0dbd$0dc", "'afoobfuoc'.replaceAll(/(f.o)/g, 'd$0d')");
        Utils.assertWithAllModes(
                "123FOOBAR321BARFOO123",
                "'123foobar321barfoo123'.replace(/[a-z]+/g, function (matched) { return matched.toUpperCase() })");

        Utils.assertWithAllModes(
                "TypeError: replaceAll must be called with a global RegExp",
                "try { 'hello'.replaceAll(/he/i, 'x'); } catch (e) { '' + e }");
    }
}
