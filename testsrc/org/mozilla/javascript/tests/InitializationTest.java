package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class InitializationTest {
    private static final String BASIC_SCRIPT = "'Hello, ' + 'World!';";

    @Test
    public void testStandard() {
        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initStandardObjects();
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void testStandardES6() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject root = cx.initStandardObjects();
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void testSafeStandard() {
        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initSafeStandardObjects();
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void testStandardSealed() {
        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initStandardObjects(null, true);
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void testStandardSealedES6() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject root = cx.initStandardObjects(null, true);
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }
}
