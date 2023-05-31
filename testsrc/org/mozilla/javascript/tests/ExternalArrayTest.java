package org.mozilla.javascript.tests;

import static org.junit.Assert.assertFalse;

import java.io.FileReader;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ExternalArrayData;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.typedarrays.NativeFloat64Array;
import org.mozilla.javascript.typedarrays.NativeInt16Array;
import org.mozilla.javascript.typedarrays.NativeInt32Array;

public class ExternalArrayTest {
    private Context cx;
    private Scriptable root;

    @Before
    public void init() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        cx.setGeneratingDebug(true);

        Global global = new Global(cx);
        root = cx.newObject(global);
    }

    @After
    public void terminate() {
        Context.exit();
    }

    @Test
    public void regularArray() {
        Scriptable a = cx.newArray(root, 10);
        root.put("testArray", root, a);
        root.put("testArrayLength", root, 10);
        root.put("regularArray", root, true);
        runScript("testsrc/jstests/extensions/external-array-test.js", 1);
    }

    @Test
    public void intArray() {
        ScriptableObject a = (ScriptableObject) cx.newObject(root);
        TestIntArray l = new TestIntArray(10);
        a.setExternalArrayData(l);
        for (int i = 0; i < 10; i++) {
            l.setArrayElement(i, i);
        }
        root.put("testArray", root, a);
        root.put("testArrayLength", root, 10);
        root.put("regularArray", root, false);
        runScript("testsrc/jstests/extensions/external-array-test.js", 1);
    }

    @Test
    public void intArrayThenRemove() {
        ScriptableObject a = (ScriptableObject) cx.newObject(root);
        // Set the external array data
        TestIntArray l = new TestIntArray(10);
        a.setExternalArrayData(l);
        for (int i = 0; i < 10; i++) {
            l.setArrayElement(i, i);
        }
        root.put("testArray", root, a);
        root.put("testArrayLength", root, 10);
        root.put("regularArray", root, false);
        runScript("testsrc/jstests/extensions/external-array-test.js", 1);

        // Clear it and test again. When cleared, object should go back to behaving like a
        // regular JavaScript object.
        a.delete("stringField");
        a.delete("intField");
        a.setExternalArrayData(null);
        for (int i = 0; i < 10; i++) {
            a.put(i, a, i);
        }
        a.defineProperty("length", 10, ScriptableObject.DONTENUM);
        root.put("regularArray", root, true);
        runScript("testsrc/jstests/extensions/external-array-test.js", 1);
    }

    @Test
    public void nativeIntArray() {
        ScriptableObject a = (ScriptableObject) cx.newObject(root);
        NativeInt32Array l = new NativeInt32Array(10);
        a.setExternalArrayData(l);

        root.put("testArray", root, a);
        root.put("testArrayLength", root, 10);
        root.put("regularArray", root, false);
        runScript("testsrc/jstests/extensions/external-array-test.js", 1);
    }

    @Test
    public void nativeShortArray() {
        ScriptableObject a = (ScriptableObject) cx.newObject(root);
        NativeInt16Array l = new NativeInt16Array(10);
        a.setExternalArrayData(l);

        root.put("testArray", root, a);
        root.put("testArrayLength", root, 10);
        root.put("regularArray", root, false);
        runScript("testsrc/jstests/extensions/external-array-test.js", 1);
    }

    @Test
    public void nativeDoubleArray() {
        ScriptableObject a = (ScriptableObject) cx.newObject(root);
        NativeFloat64Array l = new NativeFloat64Array(10);
        a.setExternalArrayData(l);

        root.put("testArray", root, a);
        root.put("testArrayLength", root, 10);
        root.put("regularArray", root, false);
        runScript("testsrc/jstests/extensions/external-array-test.js", 1);
    }

    private void runScript(String script, int opt) {
        try {
            cx.setOptimizationLevel(opt);
            try (FileReader rdr = new FileReader(script)) {
                cx.evaluateReader(root, rdr, script, 1, null);
            }
        } catch (IOException ioe) {
            assertFalse("I/O Error: " + ioe, true);
        }
    }

    private static class TestIntArray implements ExternalArrayData {
        private int[] elements;

        public TestIntArray(int length) {
            elements = new int[length];
        }

        @Override
        public Object getArrayElement(int index) {
            return elements[index];
        }

        @Override
        public void setArrayElement(int index, Object value) {
            elements[index] = (int) Context.toNumber(value);
        }

        @Override
        public int getArrayLength() {
            return elements.length;
        }
    }
}
