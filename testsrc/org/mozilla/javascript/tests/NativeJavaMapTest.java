/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * From @makusuko (Markus Sunela), imported from PR https://github.com/mozilla/rhino/pull/561
 */
public class NativeJavaMapTest extends TestCase {
    protected final Global global = new Global();

    public NativeJavaMapTest() {
        global.init(ContextFactory.getGlobal());
    }


    public void testAccessingJavaMapIntegerValues() {
        Map<Number, Number> map = new HashMap<>();
        map.put(0, 1);
        map.put(1, 2);
        map.put(2, 3);

        assertEquals(2, runScriptAsInt("value[1]", map));
        assertEquals(3, runScriptAsInt("value[2]", map));
    }

    public void testJavaMethodCalls() {
        Map<String, Number> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, runScriptAsInt("value.size()", map));
        assertEquals(1, runScriptAsInt("value.get('a')", map));
        assertEquals(4, runScriptAsInt("value.put('d', 4);value.size()", map));
    }

    public void testUpdatingJavaMapIntegerValues() {
        Map<Number, Number> map = new HashMap<>();
        map.put(0,1);
        map.put(1,2);
        map.put(2,3);

        assertEquals(2, runScriptAsInt("value[1]", map));
        assertEquals(5, runScriptAsInt("value[1]=5;value[1]", map));
        assertEquals(5, map.get(1).intValue());
    }

    public void testAccessingJavaMapStringValues() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "a");
        map.put("b", "b");
        map.put("c", "c");

        assertEquals("b", runScriptAsString("value['b']", map));
        assertEquals("c", runScriptAsString("value['c']", map));
        assertEquals("b", runScriptAsString("value.b", map));
        assertEquals("c", runScriptAsString("value.c", map));
    }

    public void testUpdatingJavaMapStringValues() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "a");
        map.put("b", "b");
        map.put("c", "c");

        assertEquals("b", runScriptAsString("value['b']", map));
        assertEquals("b", runScriptAsString("value.b", map));
        assertEquals("f", runScriptAsString("value['b']=\"f\";value['b']", map));
        assertEquals("f", map.get("b"));
        assertEquals("g", runScriptAsString("value.b=\"g\";value.b", map));
    }

    public void testAccessMapInMap() {
        Map<String, Map<String, String>> map = new HashMap<>();
        map.put("a", new HashMap<>());
        map.get("a").put("a", "a");

        assertEquals("a", runScriptAsString("value['a']['a']", map));
        assertEquals("a", runScriptAsString("value.a.a", map));
    }

    public void testUpdatingMapInMap() {
        Map<String, Map<String, String>> map = new HashMap<>();
        map.put("a", new HashMap<>());
        map.get("a").put("a", "a");

        assertEquals("a", runScriptAsString("value['a']['a']", map));
        assertEquals("a", runScriptAsString("value.a.a", map));
        assertEquals("b", runScriptAsString("value.a.a = 'b';value.a.a", map));
    }

    public void testKeys() {
        Map<String, String> map = new HashMap<>();
        NativeArray resEmpty = (NativeArray) runScript("Object.keys(value)", map, Function.identity());
        assertEquals(0, resEmpty.size());

        map.put("a", "a");
        map.put("b", "b");
        map.put("c", "c");

        NativeArray res = (NativeArray) runScript("Object.keys(value)", map, Function.identity());
        assertEquals(3, res.size());
        assertTrue(res.contains("a"));
        assertTrue(res.contains("b"));
        assertTrue(res.contains("c"));

        Map<Integer, String> mapInt = new HashMap<>();
        mapInt.put(42, "test");
        NativeArray resInt = (NativeArray) runScript("Object.keys(value)", mapInt, Function.identity());
        assertTrue(resInt.contains("42")); // Object.keys always return Strings as key
    }

    private int runScriptAsInt(String scriptSourceText, Object value) {
        return runScript(scriptSourceText, value, Context::toNumber).intValue();
    }

    private String runScriptAsString(String scriptSourceText, Object value) {
        return runScript(scriptSourceText, value, Context::toString);
    }

    private <T> T runScript(String scriptSourceText, Object value, Function<Object, T> convert) {
        return ContextFactory.getGlobal().call(context -> {
            Scriptable scope = context.initStandardObjects(global);
            scope.put("value", scope, Context.javaToJS(value, scope));
            return convert.apply(context.evaluateString(scope, scriptSourceText, "", 1, null));
        });
    }
}