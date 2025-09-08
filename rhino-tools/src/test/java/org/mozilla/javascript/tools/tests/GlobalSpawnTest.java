package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Testcases for <code>global.spawn</code>.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class GlobalSpawnTest {

    @Test
    public void testSpawnFunction() {
        String cmd = "function g(f) { a = a * f }; a = 5; var t = spawn(g, [2]); t.join(); a";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(10, ((Number) result).intValue());
                    return null;
                });
    }

    @Test
    public void testSpawnScript() {
        String cmd = "a = 5; var t = spawn(script); t.join(); a";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    var script = cx.compileString("a *= 2", "script.js", 1, null);
                    assertTrue(script instanceof Script);
                    g.put("script", g, script);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(10, ((Number) result).intValue());
                    return null;
                });
    }

    @Test
    public void testSubmitFunction() {
        String cmd = "function g(f) { return a * f }; a = 5; var f = submit(g, [2]); f.get()";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.getWrapFactory().setJavaPrimitiveWrap(false);
                    var g = new Global(cx);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(10, ((Number) result).intValue());
                    return null;
                });
    }

    @Test
    public void testSubmitScript() {
        String cmd = "a = 5; var f = submit(script); f.get()";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    cx.getWrapFactory().setJavaPrimitiveWrap(false);
                    var g = new Global(cx);
                    var script = cx.compileString("a * 2", "script.js", 1, null);
                    assertTrue(script instanceof Script);
                    g.put("script", g, script);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(10, ((Number) result).intValue());
                    return null;
                });
    }
}
