package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class FindingJavaStringsFromJSArray extends TestCase {
    public void testFindingJSStringFromJavaList() {
        List<String> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        list.add("baz");

        assertEquals(-1, runScriptAsInt("value.indexOf(\"foobar\")", list));
        assertEquals(1, runScriptAsInt("value.indexOf(\"bar\")", list));
        assertEquals(0, runScriptAsInt("Array.prototype.includes.call(value, \"foobar\")", list));
        assertEquals(1, runScriptAsInt("Array.prototype.includes.call(value, \"bar\")", list));
    }

    public void testFindingJSStringFromJavaArray() {
        String[] array = new String[3];
        array[0] = "foo";
        array[1] = "bar";
        array[2] = "baz";

        assertEquals(-1, runScriptAsInt("value.indexOf(\"foobar\")", array));
        assertEquals(1, runScriptAsInt("value.indexOf(\"bar\")", array));
        assertEquals(0, runScriptAsInt("value.includes(\"foobar\")", array));
        assertEquals(1, runScriptAsInt("value.includes(\"bar\")", array));
    }

    public void testFindingJavaStringFromJavaList() {
        List<String> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        list.add("baz");

        assertEquals(-1, runScriptAsInt("value.indexOf(value2)", list, "foobar"));
        assertEquals(1, runScriptAsInt("value.indexOf(value2)", list, "bar"));
        assertEquals(
                0, runScriptAsInt("Array.prototype.includes.call(value, value2)", list, "foobar"));
        assertEquals(
                1, runScriptAsInt("Array.prototype.includes.call(value, value2)", list, "bar"));
    }

    public void testFindingJavaStringFromJavaArray() {
        String[] array = new String[3];
        array[0] = "foo";
        array[1] = "bar";
        array[2] = "baz";

        assertEquals(-1, runScriptAsInt("value.indexOf(value2)", array, "foobar"));
        assertEquals(1, runScriptAsInt("value.indexOf(value2)", array, "bar"));
    }

    public void testFindingJavaStringFromJSArray() {
        assertEquals(-1, runScriptAsInt("[\"foo\", \"bar\", \"baz\"].indexOf(value)", "foobar"));
        assertEquals(1, runScriptAsInt("[\"foo\", \"bar\", \"baz\"].indexOf(value)", "bar"));
    }

    public void testFindingJSStringFromJSArray() {
        assertEquals(-1, runScriptAsInt("[\"foo\", \"bar\", \"baz\"].indexOf(\"foobar\")", null));
        assertEquals(1, runScriptAsInt("[\"foo\", \"bar\", \"baz\"].indexOf(\"bar\")", null));
    }

    private int runScriptAsInt(final String scriptSourceText, final Object value) {
        return ContextFactory.getGlobal()
                .call(
                        context -> {
                            Scriptable scope = context.initStandardObjects();
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return (int)
                                    Context.toNumber(
                                            context.evaluateString(
                                                    scope, scriptSourceText, "", 1, null));
                        });
    }

    private int runScriptAsInt(
            final String scriptSourceText, final Object value, final Object value2) {
        return ContextFactory.getGlobal()
                .call(
                        context -> {
                            Scriptable scope = context.initStandardObjects();
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            scope.put("value2", scope, Context.javaToJS(value2, scope));
                            return (int)
                                    Context.toNumber(
                                            context.evaluateString(
                                                    scope, scriptSourceText, "", 1, null));
                        });
    }
}
