package org.mozilla.javascript.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NativeSerializationTest {
    private Context cx;
    private Scriptable scope;

    @BeforeEach
    public void init() {
        cx = Context.enter();
        cx.setInterpretedMode(true);
        scope = new Global(cx);
    }

    @AfterEach
    public void close() {
        Context.exit();
    }

    @ParameterizedTest(name = "Serialize {0}")
    @MethodSource("getTestCases")
    public void testWithoutSerialization(String name, String createScript, String testScript) {
        cx.evaluateString(scope, "load('testsrc/assert.js');", "init.js", 1, null);
        cx.evaluateString(scope, createScript, "create.js", 1, null);
        cx.evaluateString(scope, testScript, "test.js", 1, null);
    }

    @ParameterizedTest(name = "Serialize {0}")
    @MethodSource("getTestCases")
    public void testSerialization(String name, String createScript, String testScript) throws IOException, ClassNotFoundException {
        cx.evaluateString(scope, "load('testsrc/assert.js');", "init.js", 1, null);
        cx.evaluateString(scope, createScript, "create.js", 1, null);
        cx.evaluateString(scope, testScript, "test.js", 1, null);
        Object testObj = ScriptableObject.getProperty(scope, "TESTOBJ");
        assertNotNull(testObj);
        Object newTestObj = serializeLoop(testObj);
        ScriptableObject.putProperty(scope, "TESTOBJ", newTestObj);
        cx.evaluateString(scope, testScript, "test.js", 1, null);
    }

    private static Object[][] getTestCases() {
        return new Object[][] {
                {"Map",
                "TESTOBJ = new Map();\n" +
                        "TESTOBJ.set('testing', '123');",
                "assertNotNull(TESTOBJ.get('testing'));\n" +
                        "assertEquals('123', TESTOBJ.get('testing'));"}
        };
    }

    private Object serializeLoop(Object obj) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(outBuf);
        out.writeObject(obj);
        out.close();
        ByteArrayInputStream inBuf = new ByteArrayInputStream(outBuf.toByteArray());
        ObjectInputStream in = new ObjectInputStream(inBuf);
        return in.readObject();
    }
}
