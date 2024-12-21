/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

/**
 * @author Ronald Brill
 */
public class NativeRegExpTest {

    @Test
    public void regExIsCallableForBackwardCompatibility() {
        Utils.assertWithAllModes_1_8("1", "var a = new RegExp('1'); a(1).toString();");
        Utils.assertWithAllModes_1_8("{1234},1234", "/^\\{(.*)\\}$/('{1234}').toString();");
        Utils.assertWithAllModes_1_8(null, "RegExp('a|b','g')()");
        Utils.assertWithAllModes_1_8(null, "new /z/();");
        Utils.assertWithAllModes_1_8("", "(new new RegExp).toString()");
    }

    @Test
    public void regExMinusInRangeBorderCases() {
        Utils.assertWithAllModes_1_8(
                "axbxc d efg 1 23", "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-]+/g, 'x');");
        Utils.assertWithAllModes_1_8(
                "axbxcxdxefgx1x23",
                "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\s]+/g, 'x');");
        Utils.assertWithAllModes_1_8(
                "x x x x x", "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\S]+/g, 'x');");
        Utils.assertWithAllModes_1_8(
                "x x x x x", "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\w]+/g, 'x');");
        Utils.assertWithAllModes_1_8(
                "axbxcxdxefgx1x23",
                "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\W]+/g, 'x');");
        Utils.assertWithAllModes_1_8(
                "axbxc d efg x x",
                "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\d]+/g, 'x');");
        Utils.assertWithAllModes_1_8(
                "x1x23", "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\D]+/g, 'x');");
        Utils.assertWithAllModes_1_8(
                "x-bxc d efg 1 23",
                "var r = 'a-b_c d efg 1 23';\n" + "r.replace(/[_-\\a]+/g, 'x');");
    }

    @Test
    public void regExIsNotCallable() {
        Utils.assertEcmaErrorES6(
                "TypeError: a is not a function, it is object.", "var a = new RegExp('1'); a(1);");
        Utils.assertEcmaErrorES6(
                "TypeError: /^\\{(.*)\\}$/ is not a function, it is object.",
                "/^\\{(.*)\\}$/('{1234}');");
        Utils.assertEcmaErrorES6(
                "TypeError: /a|b/g is not a function, it is object.", "RegExp('a|b','g')();");
        Utils.assertEcmaErrorES6("TypeError: /z/ is not a function, it is object.", "new /z/();");
        Utils.assertEcmaErrorES6(
                "TypeError: /(?:)/ is not a function, it is object.", "new new RegExp");
    }

    @Test
    public void lastIndexReadonly() {
        final String script =
                "try { "
                        + "  var r = /c/g;"
                        + "  Object.defineProperty(r, 'lastIndex', { writable: false });"
                        + "  r.exec('abc');"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes("Cannot modify readonly property: lastIndex.", script);
    }

    @Test
    public void search() {
        Utils.assertWithAllModes_ES6(1, "'abc'.search(/b/);");
        Utils.assertWithAllModes_ES6(1, "/b/[Symbol.search]('abc');");
        Utils.assertWithAllModes_ES6(-1, "'abc'.search(/d/);");
        Utils.assertWithAllModes_ES6(-1, "/d/[Symbol.search]('abc');");
    }

    @Test
    public void regExWrongQuantifier() {
        Utils.assertEcmaError(
                "SyntaxError: Invalid regular expression: The quantifier maximum '1' is less than the minimum '2'.",
                "'abc'.search(/b{2,1}/);");
    }

    @Test
    public void canCreateRegExpPassingExistingRegExp() {
        String script =
                "var pattern = /./i;\n"
                        + "var re = new RegExp(pattern);\n"
                        + "pattern.script === re.script &&"
                        + "  pattern.multiline === re.multiline &&"
                        + "  pattern.global === re.global && "
                        + "  pattern.ignoreCase === re.ignoreCase";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void canCreateRegExpPassingExistingRegExpAndUndefinedFlags() {
        String script =
                "var pattern = /./i;\n"
                        + "var re = new RegExp(pattern, undefined);\n"
                        + "pattern.script === re.script &&"
                        + "  pattern.multiline === re.multiline &&"
                        + "  pattern.global === re.global && "
                        + "  pattern.ignoreCase === re.ignoreCase";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void cannotCreateRegExpPassingExistingRegExpAndNewFlagsBeforeEs6() {
        String script =
                "var pattern = /./im;\n"
                        + "pattern.lastIndex = 42;\n"
                        + "new RegExp(pattern, \"g\");\n";
        Utils.assertEcmaError_1_8(
                "TypeError: Only one argument may be specified if the first argument to RegExp.prototype.compile is a RegExp object.",
                script);
    }

    @Test
    public void canCreateRegExpPassingExistingRegExpAndNewFlagsEs6() {
        String script =
                "var pattern = /./im;\n"
                        + "pattern.lastIndex = 42;\n"
                        + "var re = new RegExp(pattern, \"g\");\n"
                        + "re.global && re.lastIndex === 0";
        Utils.assertWithAllModes_ES6(true, script);
    }
}
