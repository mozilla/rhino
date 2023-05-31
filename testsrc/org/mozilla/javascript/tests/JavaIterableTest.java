/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

public class JavaIterableTest {

    protected final Global global = new Global();

    public JavaIterableTest() {
        global.init(ContextFactory.getGlobal());
    }

    @Test
    public void map() {
        Map<Object, Object> map = new LinkedHashMap<>();
        String script =
                "var a = [];\n"
                        + "for (var { key, value } of value.entrySet()) a.push(key, value);\n"
                        + "a";

        NativeArray resEmpty = (NativeArray) runScript(script, map);
        assertEquals(0, resEmpty.size());

        Object o = new Object();
        map.put("a", "b");
        map.put(123, 234);
        map.put(o, o);

        {
            NativeArray res = (NativeArray) runScript(script, map);
            assertEquals(6, res.size());
            assertEquals("a", res.get(0));
            assertEquals("b", res.get(1));
            assertEquals(123.0, Context.toNumber(res.get(2)), 0.000001);
            assertEquals(234.0, Context.toNumber(res.get(3)), 0.000001);
            assertEquals(o, res.get(4));
            assertEquals(o, res.get(5));
        }

        {
            NativeArray res = (NativeArray) runScript("Array.from(value.entrySet())", map);
            assertEquals(3, res.size());

            Map.Entry<Object, Object> e0 = (Map.Entry<Object, Object>) res.get(0);
            assertEquals("a", e0.getKey());
            assertEquals("b", e0.getValue());

            Map.Entry<Object, Object> e1 = (Map.Entry<Object, Object>) res.get(1);
            assertEquals(123.0, Context.toNumber(e1.getKey()), 0.000001);
            assertEquals(234.0, Context.toNumber(e1.getValue()), 0.000001);

            Map.Entry<Object, Object> e2 = (Map.Entry<Object, Object>) res.get(2);
            assertEquals(o, e2.getKey());
            assertEquals(o, e2.getValue());
        }
    }

    @Test
    public void list() {
        List<Object> list = new ArrayList<>();
        String script = "var a = [];\n" + "for (var e of value) a.push(e);\n" + "a";

        NativeArray resEmpty = (NativeArray) runScript(script, list);
        assertEquals(0, resEmpty.size());

        Object o = new Object();
        list.add("a");
        list.add(123);
        list.add(o);

        {
            NativeArray res = (NativeArray) runScript(script, list);
            assertEquals(3, res.size());
            assertEquals("a", res.get(0));
            assertEquals(123.0, Context.toNumber(res.get(1)), 0.000001);
            assertEquals(o, res.get(2));
        }

        {
            NativeArray res = (NativeArray) runScript("Array.from(value)", list);
            assertEquals(3, res.size());
            assertEquals("a", res.get(0));
            assertEquals(123.0, Context.toNumber(res.get(1)), 0.000001);
            assertEquals(o, res.get(2));
        }
    }

    @Test
    public void set() {
        Set<Object> set = new LinkedHashSet<>();
        String script = "var a = [];\n" + "for (var e of value) a.push(e);\n" + "a";

        NativeArray resEmpty = (NativeArray) runScript(script, set);
        assertEquals(0, resEmpty.size());

        Object o = new Object();
        set.add("a");
        set.add("a");
        set.add(123);
        set.add(o);

        {
            NativeArray res = (NativeArray) runScript(script, set);
            assertEquals(3, res.size());
            assertEquals("a", res.get(0));
            assertEquals(123.0, Context.toNumber(res.get(1)), 0.000001);
            assertEquals(o, res.get(2));
        }

        {
            NativeArray res = (NativeArray) runScript("Array.from(value)", set);
            assertEquals(3, res.size());
            assertEquals("a", res.get(0));
            assertEquals(123.0, Context.toNumber(res.get(1)), 0.000001);
            assertEquals(o, res.get(2));
        }
    }

    @Test
    public void noIterable() {
        Object o = new Object();
        String script = "for (var e of value) ;";

        assertThrows(
                EcmaError.class,
                () -> {
                    runScript(script, o);
                });
    }

    private Object runScript(String scriptSourceText, Object value) {
        return ContextFactory.getGlobal()
                .call(
                        context -> {
                            context.setLanguageVersion(Context.VERSION_ES6);
                            Scriptable scope = context.newObject(global);
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return context.evaluateString(scope, scriptSourceText, "", 1, null);
                        });
    }
}
