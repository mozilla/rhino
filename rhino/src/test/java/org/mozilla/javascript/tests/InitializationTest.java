package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class InitializationTest {
    private static final String BASIC_SCRIPT = "'Hello, ' + 'World!';";

    @Test
    public void standard() {
        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initStandardObjects();
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void standardES6() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject root = cx.initStandardObjects();
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void safeStandard() {
        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initSafeStandardObjects();
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void safeStandardSupportsStringFromContext() {
        String code =
                "let res = '';\n"
                        + "res += str[0];\n"
                        + "res += str.length;\n"
                        + "res += ' ';\n"
                        + "res += str;\n"
                        + "res;";

        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initSafeStandardObjects();
            root.put("str", root, "Rhino");
            Object result = cx.evaluateString(root, code, "test", 1, null);
            assertEquals("R5 Rhino", result);
        }
    }

    @Test
    public void safeStandardSupportsArrayFromContext() {
        String code =
                "let res = '';\n"
                        + "res += arr[0];\n"
                        + "res += arr.length;\n"
                        + "res += ' ';\n"
                        + "for (let elem in arr) { res += arr[elem]; }\n"
                        + "res += ' ';\n"
                        + "for (let elem of arr) { res += elem; }\n"
                        + "res;";

        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initSafeStandardObjects();
            root.put("arr", root, new String[] {"a", "b", "d"});
            Object result = cx.evaluateString(root, code, "test", 1, null);
            assertEquals("a3 abd abd", result);
        }
    }

    @Test
    public void safeStandardSupportsListFromContext() {
        String code =
                "let res = '';\n"
                        + "res += lst[0];\n"
                        + "res += lst.length;\n"
                        + "res += ' ';\n"
                        + "for (let elem in lst) { res += lst[elem]; }\n"
                        + "res += ' ';\n"
                        + "for (let elem of lst) { res += elem; }\n"
                        + "res;";

        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initSafeStandardObjects();
            root.put("lst", root, List.of("a", "b", "c"));
            Object result = cx.evaluateString(root, code, "test", 1, null);
            assertEquals("a3 abc abc", result);
        }
    }

    @Test
    public void safeStandardSupportsMapFromContext() {
        String code = "let res = '';\n" + "for (let elem of mp) { res += elem; }\n" + "res;";

        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initSafeStandardObjects();
            root.put("mp", root, Map.of("k0", "v0"));
            Object result = cx.evaluateString(root, code, "test", 1, null);
            assertEquals("k0,v0", result);
        }
    }

    @Test
    public void safeStandardSupportsSetFromContext() {
        String code = "let res = '';\n" + "for (let elem of mp) { res += elem; }\n" + "res;";

        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initSafeStandardObjects();
            root.put("mp", root, Set.of("v0"));
            Object result = cx.evaluateString(root, code, "test", 1, null);
            assertEquals("v0", result);
        }
    }

    @Test
    public void standardSealed() {
        try (Context cx = Context.enter()) {
            ScriptableObject root = cx.initStandardObjects(null, true);
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }

    @Test
    public void standardSealedES6() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject root = cx.initStandardObjects(null, true);
            Object result = cx.evaluateString(root, BASIC_SCRIPT, "basic", 1, null);
            assertEquals("Hello, World!", result);
        }
    }
}
