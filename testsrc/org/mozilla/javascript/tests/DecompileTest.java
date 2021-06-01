/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;

/**
 * Test for {@link Context#decompileScript(Script, int)}.
 *
 * @author Marc Guillemot
 */
public class DecompileTest {

    /**
     * As of head of trunk on 30.09.09, decompile of "new Date()" returns "new Date" without
     * parentheses.
     *
     * @see <a href="https://bugzilla.mozilla.org/show_bug.cgi?id=519692">Bug 519692</a>
     */
    @Test
    public void newObject0Arg() {
        final String source = "var x = new Date().getTime();";
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Script script = cx.compileString(source, "my script", 0, null);
                    Assert.assertEquals(source, cx.decompileScript(script, 4).trim());
                    return null;
                });
    }

    @Test
    public void bigInt() {
        final String source = "var x = 123n;";
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Script script = cx.compileString(source, "my script", 0, null);
                    Assert.assertEquals(source, cx.decompileScript(script, 4).trim());
                    return null;
                });
    }

    @Test
    public void templateLiteral() {
        final String source = "var x = `test${1}`;";
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Script script = cx.compileString(source, "my script", 0, null);
                    Assert.assertEquals(source, cx.decompileScript(script, 4).trim());
                    return null;
                });
    }

    @Test
    public void taggedTemplateLiteral() {
        final String source = "var x = (() => { } )`test${2}\\unicode`;";
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Script script = cx.compileString(source, "my script", 0, null);
                    Assert.assertEquals(
                            source,
                            cx.decompileScript(script, 4)
                                    .trim()
                                    .replaceAll("\\R", "")
                                    .replaceAll("\\s+", " "));
                    return null;
                });
    }
}
