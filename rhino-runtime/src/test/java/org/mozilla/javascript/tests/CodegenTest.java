/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * Test for falling back to the interpreter if generated code is too large.
 *
 * @author RBRi
 */
public class CodegenTest {

    @Test
    public void largeMethod() {
        final StringBuilder scriptSource = new StringBuilder();

        scriptSource.append("var a = 0;");
        for (int i = 0; i < 1000; i++) {
            scriptSource.append("a = a + 1;");
        }

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    if (_cx.getOptimizationLevel() > -1) {
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.gen.test_source_"));
                    }
                    return null;
                });

        // now with code that is too large
        for (int i = 0; i < 1000; i++) {
            scriptSource.append("a = a + 1;");
        }

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    Assert.assertTrue(
                            script.getClass().getName(),
                            script.getClass()
                                    .getName()
                                    .startsWith("org.mozilla.javascript.InterpretedFunction"));
                    return null;
                });

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    try {
                        Script script =
                                _cx.compileReader(
                                        new StringReader(scriptSource.toString()),
                                        "test-source",
                                        1,
                                        null);
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.InterpretedFunction"));
                        Assert.assertTrue(
                                "" + ((NativeFunction) script).getEncodedSource().length(),
                                ((NativeFunction) script).getEncodedSource().length() > 1000);
                        return null;
                    } catch (IOException e) {
                        Assert.fail(e.getMessage());
                        return null;
                    }
                });
    }

    @Test
    public void manyExceptionHandlers() {
        final StringBuilder scriptSource = new StringBuilder();

        scriptSource.append("var a = 0;");
        for (int i = 0; i < 1000; i++) {
            scriptSource.append("try { a = a + 1; } catch(e) { alert(e); }");
        }

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    final Scriptable scope = _cx.initStandardObjects();
                    Assert.assertEquals(
                            1000d,
                            (double)
                                    _cx.evaluateString(
                                            scope, scriptSource.toString(), "myScript.js", 1, null),
                            0.001);
                    return null;
                });
    }

    @Test
    public void largeVarList() {
        final StringBuilder scriptSource = new StringBuilder();

        int i = 0;
        for (; i < 1000; i++) {
            scriptSource.append("var a" + i + ";");
        }

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    if (_cx.getOptimizationLevel() > -1) {
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.gen.test_source_"));
                    }
                    return null;
                });

        // now with code that is too large
        for (; i < 10000; i++) {
            scriptSource.append("var a" + i + ";");
        }

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    Assert.assertTrue(
                            script.getClass().getName(),
                            script.getClass()
                                    .getName()
                                    .startsWith("org.mozilla.javascript.InterpretedFunction"));
                    Assert.assertTrue(
                            "" + ((NativeFunction) script).getEncodedSource().length(),
                            ((NativeFunction) script).getEncodedSource().length() > 1000);
                    return null;
                });

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    try {
                        Script script =
                                _cx.compileReader(
                                        new StringReader(scriptSource.toString()),
                                        "test-source",
                                        1,
                                        null);
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.InterpretedFunction"));
                        Assert.assertTrue(
                                "" + ((NativeFunction) script).getEncodedSource().length(),
                                ((NativeFunction) script).getEncodedSource().length() > 1000);
                        return null;
                    } catch (IOException e) {
                        Assert.fail(e.getMessage());
                        return null;
                    }
                });
    }

    @Test
    public void largeLocalVarList() {
        final StringBuilder scriptSource = new StringBuilder();

        scriptSource.append("function foo() {");
        for (int i = 0; i < 1000; i++) {
            scriptSource.append("a" + i + "= " + i + ";");
        }
        scriptSource.append("return 'done'; }");

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    if (_cx.getOptimizationLevel() > -1) {
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.gen.test_source_"));
                    }
                    return null;
                });

        // now with code that is too large
        scriptSource.setLength(0);
        scriptSource.append("function foo() {");
        for (int i = 0; i < 5000; i++) {
            scriptSource.append("a" + i + "= " + i + ";");
        }
        scriptSource.append("return 'done'; }");

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    Assert.assertTrue(
                            script.getClass().getName(),
                            script.getClass()
                                    .getName()
                                    .startsWith("org.mozilla.javascript.InterpretedFunction"));
                    Assert.assertTrue(
                            "" + ((NativeFunction) script).getEncodedSource().length(),
                            ((NativeFunction) script).getEncodedSource().length() > 1000);
                    return null;
                });

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    try {
                        Script script =
                                _cx.compileReader(
                                        new StringReader(scriptSource.toString()),
                                        "test-source",
                                        1,
                                        null);
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.InterpretedFunction"));
                        Assert.assertTrue(
                                "" + ((NativeFunction) script).getEncodedSource().length(),
                                ((NativeFunction) script).getEncodedSource().length() > 1000);
                        return null;
                    } catch (IOException e) {
                        Assert.fail(e.getMessage());
                        return null;
                    }
                });
    }

    @Test
    public void tooManyMethods() {
        final StringBuilder scriptSource = new StringBuilder();

        int i = 0;
        for (; i < 1000; i++) {
            scriptSource.append("function foo" + i + "() { return 7; }\n");
        }

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    if (_cx.getOptimizationLevel() > -1) {
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.gen.test_source_"));
                    }
                    return null;
                });

        // now with code that is too large
        for (; i < 5000; i++) {
            scriptSource.append("function foo" + i + "() { return 7; }\n");
        }

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    Script script =
                            _cx.compileString(scriptSource.toString(), "test-source", 1, null);
                    Assert.assertTrue(
                            script.getClass().getName(),
                            script.getClass()
                                    .getName()
                                    .startsWith("org.mozilla.javascript.InterpretedFunction"));
                    Assert.assertTrue(
                            "" + ((NativeFunction) script).getEncodedSource().length(),
                            ((NativeFunction) script).getEncodedSource().length() > 1000);
                    return null;
                });

        Utils.runWithAllOptimizationLevels(
                _cx -> {
                    try {
                        Script script =
                                _cx.compileReader(
                                        new StringReader(scriptSource.toString()),
                                        "test-source",
                                        1,
                                        null);
                        Assert.assertTrue(
                                script.getClass().getName(),
                                script.getClass()
                                        .getName()
                                        .startsWith("org.mozilla.javascript.InterpretedFunction"));
                        Assert.assertTrue(
                                "" + ((NativeFunction) script).getEncodedSource().length(),
                                ((NativeFunction) script).getEncodedSource().length() > 1000);
                        return null;
                    } catch (IOException e) {
                        Assert.fail(e.getMessage());
                        return null;
                    }
                });
    }
}
