package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.shell.Global;

public class UndefinedTest {
    private Context cx;
    private Scriptable global;

    @Before
    public void init() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        global = new Global(cx);
    }

    @After
    public void close() {
        cx.close();
    }

    @Test
    public void undefinedInstances() {
        // Ensure that our two internal representations of "undefined" are equivalent
        assertEquals(Undefined.instance, Undefined.SCRIPTABLE_UNDEFINED);
        assertEquals(Undefined.instance.hashCode(), Undefined.SCRIPTABLE_UNDEFINED.hashCode());
        assertTrue(Undefined.isUndefined(Undefined.instance));
        assertTrue(Undefined.isUndefined(Undefined.SCRIPTABLE_UNDEFINED));
        assertFalse(Undefined.isUndefined(cx.newObject(global)));
    }

    @Test
    public void undefinedThis() {
        // Run a function that checks the value of "this"
        final String testFuncSource =
                "load('testsrc/assert.js');\n"
                        + "function testFunc() {\n"
                        + "assertSame(this, undefined);\n"
                        + "}\n"
                        + "testFunc;";
        Callable testFunc =
                (Callable) cx.evaluateString(global, testFuncSource, "test.js", 1, null);
        // Ensure that using SCRIPTABLE_UNDEFINED means that "this" is really undefined
        testFunc.call(cx, global, Undefined.SCRIPTABLE_UNDEFINED, ScriptRuntime.emptyArgs);
    }

    @Test
    public void instanceOf() {
        // These are specific instanceof checks used in the "kangax/compat-table" code
        // that once failed because of a SCRIPTABLE_UNDEFINED implementation.
        final String testSource =
                "load('testsrc/assert.js');\n"
                        + "let x = ({ __proto__ : [] } instanceof Array);\n"
                        + "assertTrue(x);\n"
                        + "let y = ({ __proto__ : null } instanceof Object);\n"
                        + "assertFalse(y);";
        cx.evaluateString(global, testSource, "test.js", 1, null);
    }
}
