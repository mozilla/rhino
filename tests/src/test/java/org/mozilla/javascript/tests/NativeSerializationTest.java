package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;
import org.mozilla.javascript.tools.shell.Global;

public class NativeSerializationTest {
    private Context cx;
    private Scriptable scope;

    @BeforeEach
    public void init() {
        cx = Context.enter();
        scope = new Global(cx);
    }

    @AfterEach
    public void close() {
        Context.exit();
    }

    private static Object[][] getTestCases() {
        ArrayList<Object[]> result = new ArrayList<>();
        for (Object[] testCase : getBaseTestCases()) {
            result.add(addFlag(testCase, true));
            result.add(addFlag(testCase, false));
        }
        return result.toArray(new Object[0][0]);
    }

    private static Object[] addFlag(Object[] tc, boolean flag) {
        Object[] ret = new Object[tc.length + 1];
        System.arraycopy(tc, 0, ret, 0, tc.length);
        ret[tc.length] = flag;
        return ret;
    }

    private static Object[][] getBaseTestCases() {
        return new Object[][] {
            {"String", "TESTOBJ = 'testing';", "assertEquals('testing', TESTOBJ);"},
            {"Number", "TESTOBJ = Number(123);", "assertEquals(123, TESTOBJ);"},
            {"Boolean", "TESTOBJ = Boolean(true);", "assertEquals(true, TESTOBJ);"},
            {
                "Symbol",
                "TESTOBJ = Symbol('test');",
                "assertEquals('Symbol(test)', TESTOBJ.toString());"
            },
            {
                "Date",
                "TESTOBJ = new Date(1737920959661);",
                "assertEquals(1737920959661, TESTOBJ.valueOf());"
            },
            {
                "Object",
                "TESTOBJ = {a: 1, b: 'two', c: {a: 3}};",
                "assertEquals(1, TESTOBJ.a);\nassertEquals('two', TESTOBJ.b);\nassertEquals(3, TESTOBJ.c.a);"
            },
            {
                "Map",
                "TESTOBJ = new Map();\nTESTOBJ.set('testing', '123');",
                "assertEquals('123', TESTOBJ.get('testing'));"
            },
            {
                "Set",
                "TESTOBJ = new Set();\nTESTOBJ.add('testing');",
                "assertTrue(TESTOBJ.has('testing'));"
            },
            // Don't expect WeakMap and WeakSet to retain values after serialization.
            // However, do expect them to serialize without error and work after
            // serialization.
            {
                "WeakMap",
                "let key = {};\nTESTOBJ = new WeakMap();\nTESTOBJ.set(key, 123);",
                "TESTOBJ.set(key, 456);\nassertEquals(456, TESTOBJ.get(key));"
            },
            {
                "WeakSet",
                "let key = {};\nTESTOBJ = new WeakSet();\nTESTOBJ.add(key);",
                "TESTOBJ.add(key);\nassertTrue(TESTOBJ.has(key));"
            }
        };
    }

    /*
     * Test to ensure that each pair of scripts works without serialization.
     */
    @ParameterizedTest(name = "Sanity Check {0} interpreted = {3}")
    @MethodSource("getTestCases")
    public void testWithoutSerialization(
            String name, String createScript, String testScript, boolean interpreted) {
        cx.setInterpretedMode(interpreted);
        cx.evaluateString(scope, "load('testsrc/assert.js');", "init.js", 1, null);
        cx.evaluateString(scope, createScript, "create.js", 1, null);
        cx.evaluateString(scope, testScript, "test.js", 1, null);
    }

    /*
     * Test to ensure that each type of object may be serialized and deserialized and end
     * up in a sane state.
     */
    @ParameterizedTest(name = "Serialize {0} interpreted = {3}")
    @MethodSource("getTestCases")
    public void testSerialization(
            String name, String createScript, String testScript, boolean interpreted)
            throws IOException, ClassNotFoundException {
        cx.setInterpretedMode(interpreted);
        cx.evaluateString(scope, "load('testsrc/assert.js');", "init.js", 1, null);
        cx.evaluateString(scope, createScript, "create.js", 1, null);
        cx.evaluateString(scope, testScript, "test.js", 1, null);
        Object testObj = ScriptableObject.getProperty(scope, "TESTOBJ");
        assertNotNull(testObj);
        Object newTestObj = serializeLoop(testObj);
        ScriptableObject.putProperty(scope, "TESTOBJ", newTestObj);
        cx.evaluateString(scope, testScript, "test.js", 1, null);
    }

    private Object serializeLoop(Object obj) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        // We need to use ScriptableOutputStream here to ensure that Rhino objects
        // are properly serialized, including classloading of generated code.
        ScriptableOutputStream out = new ScriptableOutputStream(outBuf, scope);
        out.writeObject(obj);
        out.close();
        ByteArrayInputStream inBuf = new ByteArrayInputStream(outBuf.toByteArray());
        ScriptableInputStream in = new ScriptableInputStream(inBuf, scope);
        return in.readObject();
    }
}
