/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author André Bargull
 */
public class Bug714204Test {

    private Context cx;
    private ScriptableObject scope;

    @BeforeEach
    public void setUp() {
        cx = Context.enter();
        scope = cx.initStandardObjects();
        cx.setLanguageVersion(Context.VERSION_1_7);
    }

    @AfterEach
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void assign_this() {
        StringBuilder sb = new StringBuilder();
        sb.append("function F() {\n");
        sb.append("  [this.x] = arguments;\n");
        sb.append("}\n");
        sb.append("var f = new F('a');\n");
        sb.append("(f.x == 'a')\n");
        Script script = cx.compileString(sb.toString(), "<eval>", 1, null);
        Object result = script.exec(cx, scope, scope);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void varThis() {
        assertThrows(
                EvaluatorException.class,
                () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("function F() {\n");
                    sb.append("  var [this.x] = arguments;\n");
                    sb.append("}\n");
                    cx.compileString(sb.toString(), "<eval>", 1, null);
                });
    }

    @Test
    public void letThis() {
        assertThrows(
                EvaluatorException.class,
                () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("function F() {\n");
                    sb.append("  let [this.x] = arguments;\n");
                    sb.append("}\n");
                    cx.compileString(sb.toString(), "<eval>", 1, null);
                });
    }

    @Test
    public void constThis() {
        assertThrows(
                EvaluatorException.class,
                () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("function F() {\n");
                    sb.append("  const [this.x] = arguments;\n");
                    sb.append("}\n");
                    cx.compileString(sb.toString(), "<eval>", 1, null);
                });
    }

    @Test
    public void argsThis() {
        assertThrows(
                EvaluatorException.class,
                () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("function F([this.x]) {\n");
                    sb.append("}\n");
                    cx.compileString(sb.toString(), "<eval>", 1, null);
                });
    }
}
