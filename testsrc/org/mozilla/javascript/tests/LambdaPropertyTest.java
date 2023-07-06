package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import java.io.FileReader;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class LambdaPropertyTest {
    private Context cx;
    private Scriptable global;

    @Before
    public void init() throws IOException {
        try (FileReader rdr = new FileReader("testsrc/assert.js")) {
            cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setGeneratingDebug(true);
            global = cx.initStandardObjects();
            cx.evaluateReader(global, rdr, "assert.js", 1, null);
        }
    }

    @After
    public void terminate() {
        Context.exit();
    }

    @Test
    public void basicProperty() {
        ScriptableObject testObj = (ScriptableObject) cx.newObject(global);
        ScriptableObject.defineProperty(global, "o", testObj, 0);

        SomeState state = new SomeState();
        testObj.defineProperty(
                "val",
                () -> state.val,
                (Object newVal) -> state.val = ScriptRuntime.toInt32(newVal),
                0);

        // Get and set should work as normal
        cx.evaluateString(global, "assertEquals(0, o.val)", "test.js", 1, null);
        cx.evaluateString(global, "o.val = 11; assertEquals(11, o.val)", "test.js", 1, null);
        assertEquals(11, state.val);

        // Check the descriptor
        cx.evaluateString(
                global,
                "let desc = Object.getOwnPropertyDescriptor(o, 'val');"
                        + "assertTrue(desc.writable);"
                        + "assertTrue(desc.enumerable);"
                        + "assertTrue(desc.configurable);"
                        + "assertEquals(11, desc.value);",
                "test.js",
                1,
                null);
    }

    @Test
    public void redefineProperty() {
        ScriptableObject testObj = (ScriptableObject) cx.newObject(global);
        ScriptableObject.defineProperty(global, "o", testObj, 0);
        // Define once
        cx.evaluateString(
                global,
                "Object.defineProperty(o, 'val', { value: 1, configurable: true });"
                        + "assertEquals(1, o.val);",
                "test.js",
                1,
                null);

        SomeState state = new SomeState();
        testObj.defineProperty(
                "val",
                () -> state.val,
                (Object newVal) -> state.val = ScriptRuntime.toInt32(newVal),
                0);

        // Get and set should work as normal
        cx.evaluateString(global, "assertEquals(0, o.val)", "test.js", 1, null);
        cx.evaluateString(global, "o.val = 2; assertEquals(2, o.val)", "test.js", 1, null);
        assertEquals(2, state.val);

        // Define again
        cx.evaluateString(
                global,
                "Object.defineProperty(o, 'val', { value: 3 });" + "assertEquals(3, o.val);",
                "test.js",
                1,
                null);
        assertEquals(2, state.val);
    }

    @Test
    public void redefinePropertyNonConfigurable() {
        ScriptableObject testObj = (ScriptableObject) cx.newObject(global);
        ScriptableObject.defineProperty(global, "o", testObj, 0);
        // Define once
        cx.evaluateString(
                global,
                "Object.defineProperty(o, 'val', { value: 1, configurable: true });"
                        + "assertEquals(1, o.val);",
                "test.js",
                1,
                null);

        SomeState state = new SomeState();
        testObj.defineProperty(
                "val",
                () -> state.val,
                (Object newVal) -> state.val = ScriptRuntime.toInt32(newVal),
                ScriptableObject.PERMANENT);

        // Get and set should work as normal
        cx.evaluateString(global, "assertEquals(0, o.val)", "test.js", 1, null);
        cx.evaluateString(global, "o.val = 2; assertEquals(2, o.val)", "test.js", 1, null);

        // Define again -- should fail
        cx.evaluateString(
                global,
                "assertThrows(() => { Object.defineProperty(o, 'val', { value: 2 }); });",
                "test.js",
                1,
                null);
    }

    @Test
    public void propertyNoSetter() {
        ScriptableObject testObj = (ScriptableObject) cx.newObject(global);
        ScriptableObject.defineProperty(global, "o", testObj, 0);

        SomeState state = new SomeState();
        testObj.defineProperty("val", () -> state.val, null, 0);

        // Set should do nothing
        cx.evaluateString(global, "assertEquals(0, o.val)", "test.js", 1, null);
        cx.evaluateString(global, "o.val = 11; assertEquals(0, o.val)", "test.js", 1, null);
        assertEquals(0, state.val);
    }

    @Test
    public void propertyNoGetter() {
        ScriptableObject testObj = (ScriptableObject) cx.newObject(global);
        ScriptableObject.defineProperty(global, "o", testObj, 0);

        SomeState state = new SomeState();
        testObj.defineProperty(
                "val", null, (Object newVal) -> state.val = ScriptRuntime.toInt32(newVal), 0);

        // Get should return the old value, which is unfortunately null
        cx.evaluateString(global, "assertEquals(null, o.val)", "test.js", 1, null);
        cx.evaluateString(global, "o.val = 11; assertEquals(null, o.val)", "test.js", 1, null);
    }

    private static class SomeState {
        int val;
    }
}
