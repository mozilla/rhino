/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

/** From @makusuko (Markus Sunela), imported from PR https://github.com/mozilla/rhino/pull/561 */
public class NativeJavaMapTest {

    protected final Global global = new Global();

    private final ContextFactory contextFactoryWithMapAccess =
            new ContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS) {
                        return true;
                    }
                    return super.hasFeature(cx, featureIndex);
                }
            };

    public NativeJavaMapTest() {
        global.init(ContextFactory.getGlobal());
    }

    @Test
    public void accessingNullValues() {
        Map<Object, Number> map = new HashMap<>();
        map.put("a", null);
        map.put(1, null);
        assertEquals(2, runScriptAsInt("value.size()", map, true));
        assertEquals(null, runScript("value.a", map, true));
        assertEquals(null, runScript("value[1]", map, true));
    }

    @Test
    public void accessingJavaMapIntegerValues() {
        Map<Number, Number> map = new HashMap<>();
        map.put(0, 1);
        map.put(1, 2);
        map.put(2, 3);

        assertEquals(2, runScriptAsInt("value[1]", map, true));
        assertEquals(3, runScriptAsInt("value[2]", map, true));
    }

    @Test
    public void javaMethodCalls() {
        Map<String, Number> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, runScriptAsInt("value.size()", map, true));
        assertEquals(1, runScriptAsInt("value.get('a')", map, true));
        assertEquals(4, runScriptAsInt("value.put('d', 4);value.size()", map, true));
    }

    @Test
    public void updatingJavaMapIntegerValues() {
        Map<Number, Number> map = new HashMap<>();
        map.put(0, 1);
        map.put(1, 2);
        map.put(2, 3);

        assertEquals(2, runScriptAsInt("value[1]", map, true));
        assertEquals(5, runScriptAsInt("value[1]=5;value[1]", map, true));
        assertEquals(5, map.get(1).intValue());
    }

    @Test
    public void accessingJavaMapStringValues() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "a");
        map.put("b", "b");
        map.put("c", "c");

        assertEquals("b", runScriptAsString("value['b']", map, true));
        assertEquals("c", runScriptAsString("value['c']", map, true));
        assertEquals("b", runScriptAsString("value.b", map, true));
        assertEquals("c", runScriptAsString("value.c", map, true));
    }

    @Test
    public void updatingJavaMapStringValues() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "a");
        map.put("b", "b");
        map.put("c", "c");

        assertEquals("b", runScriptAsString("value['b']", map, true));
        assertEquals("b", runScriptAsString("value.b", map, true));
        assertEquals("f", runScriptAsString("value['b']=\"f\";value['b']", map, true));
        assertEquals("f", map.get("b"));
        assertEquals("g", runScriptAsString("value.b=\"g\";value.b", map, true));
    }

    @Test
    public void accessMapInMap() {
        Map<String, Map<String, String>> map = new HashMap<>();
        map.put("a", new HashMap<>());
        map.get("a").put("a", "a");

        assertEquals("a", runScriptAsString("value['a']['a']", map, true));
        assertEquals("a", runScriptAsString("value.a.a", map, true));
    }

    @Test
    public void updatingMapInMap() {
        Map<String, Map<String, String>> map = new HashMap<>();
        map.put("a", new HashMap<>());
        map.get("a").put("a", "a");

        assertEquals("a", runScriptAsString("value['a']['a']", map, true));
        assertEquals("a", runScriptAsString("value.a.a", map, true));
        assertEquals("b", runScriptAsString("value.a.a = 'b';value.a.a", map, true));
    }

    @Test
    public void keys() {
        Map<String, String> map = new HashMap<>();
        NativeArray resEmpty = (NativeArray) runScript("Object.keys(value)", map, true);
        assertEquals(0, resEmpty.size());

        map.put("a", "a");
        map.put("b", "b");
        map.put("c", "c");

        NativeArray res = (NativeArray) runScript("Object.keys(value)", map, true);
        assertEquals(3, res.size());
        assertTrue(res.contains("a"));
        assertTrue(res.contains("b"));
        assertTrue(res.contains("c"));

        Map<Integer, String> mapInt = new HashMap<>();
        mapInt.put(42, "test");
        NativeArray resInt = (NativeArray) runScript("Object.keys(value)", mapInt, true);
        assertTrue(resInt.contains("42")); // Object.keys always return Strings as key
    }

    @Test
    public void javaMapWithoutAccessEntries() {
        Map<Object, Object> map = new HashMap<>();
        map.put(0, 1);
        map.put("put", "method");
        map.put("a", "abc");

        assertThrows(EvaluatorException.class, () -> runScript("value[0]", map, false));
        assertTrue(runScript("value.put", map, false) instanceof NativeJavaMethod);
        assertThrows(EvaluatorException.class, () -> runScript("value['a'] = 0", map, false));
        assertEquals(false, runScript("'a' in value", map, false));
        assertEquals(true, runScript("Object.keys(value).includes('getClass')", map, false));
    }

    @Test
    public void symbolIterator() {
        Map map = new LinkedHashMap();
        String script =
                "var a = [];\n" + "for (var [key, value] of value) a.push(key, value);\n" + "a";

        NativeArray resEmpty = (NativeArray) runScriptES6(script, map);
        assertEquals(0, resEmpty.size());

        Object o = new Object();
        map.put("a", "b");
        map.put(123, 234);
        map.put(o, o);

        {
            NativeArray res = (NativeArray) runScriptES6(script, map);
            assertEquals(6, res.size());
            assertEquals("a", res.get(0));
            assertEquals("b", res.get(1));
            assertEquals(123.0, Context.toNumber(res.get(2)), 0.000001);
            assertEquals(234.0, Context.toNumber(res.get(3)), 0.000001);
            assertEquals(o, res.get(4));
            assertEquals(o, res.get(5));
        }

        {
            NativeArray res = (NativeArray) runScriptES6("Array.from(value)", map);
            assertEquals(3, res.size());

            NativeArray e0 = (NativeArray) res.get(0);
            assertEquals("a", e0.get(0));
            assertEquals("b", e0.get(1));

            NativeArray e1 = (NativeArray) res.get(1);
            assertEquals(123.0, Context.toNumber(e1.get(0)), 0.000001);
            assertEquals(234.0, Context.toNumber(e1.get(1)), 0.000001);

            NativeArray e2 = (NativeArray) res.get(2);
            assertEquals(o, e2.get(0));
            assertEquals(o, e2.get(1));
        }
    }

    private int runScriptAsInt(String scriptSourceText, Object value, boolean enableJavaMapAccess) {
        return runScript(scriptSourceText, value, Context::toNumber, enableJavaMapAccess)
                .intValue();
    }

    private String runScriptAsString(
            String scriptSourceText, Object value, boolean enableJavaMapAccess) {
        return runScript(scriptSourceText, value, Context::toString, enableJavaMapAccess);
    }

    private Object runScript(String scriptSourceText, Object value, boolean enableJavaMapAccess) {
        return runScript(scriptSourceText, value, Function.identity(), enableJavaMapAccess);
    }

    private <T> T runScript(
            String scriptSourceText,
            Object value,
            Function<Object, T> convert,
            boolean enableJavaMapAccess) {
        return getContextFactory(enableJavaMapAccess)
                .call(
                        context -> {
                            Scriptable scope = context.newObject(global);
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return convert.apply(
                                    context.evaluateString(scope, scriptSourceText, "", 1, null));
                        });
    }

    private Object runScriptES6(String scriptSourceText, Object value) {
        return getContextFactory(false)
                .call(
                        context -> {
                            Scriptable scope = context.newObject(global);
                            context.setLanguageVersion(Context.VERSION_ES6);
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return context.evaluateString(scope, scriptSourceText, "", 1, null);
                        });
    }

    private ContextFactory getContextFactory(boolean enableJavaMapAccess) {
        return enableJavaMapAccess ? contextFactoryWithMapAccess : ContextFactory.getGlobal();
    }
}
