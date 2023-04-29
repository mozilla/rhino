/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

/** From @makusuko (Markus Sunela), imported from PR https://github.com/mozilla/rhino/pull/561 */
public class NativeJavaListTest {

    protected final Global global = new Global();

    public NativeJavaListTest() {
        global.init(ContextFactory.getGlobal());
    }

    @Test
    public void accessingNullValues() {
        List<Integer> list = new ArrayList<>();
        list.add(null);

        assertEquals(null, runScript("value[0]", list, Function.identity()));
        assertEquals(1, runScriptAsInt("value.length", list));
    }

    @Test
    public void autoGrowList() {
        List<String> list = new ArrayList<>();
        runScriptAsInt("value[10] = 'Foo'", list);
        assertEquals(11, list.size());
        assertEquals(null, list.get(9));
        assertEquals("Foo", list.get(10));

        list = new LinkedList<>();
        runScriptAsInt("value[10] = 'Foo'", list);
        assertEquals(11, list.size());
        assertEquals(null, list.get(9));
        assertEquals("Foo", list.get(10));
    }

    @Test
    public void accessingJavaListIntegerValues() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(2, runScriptAsInt("value[1]", list));
        assertEquals(3, runScriptAsInt("value[2]", list));
        assertEquals(3, runScriptAsInt("value.length", list));
    }

    @Test
    public void lengthProperty() {
        List<Integer> list = new ArrayList<>();
        assertEquals(0, runScriptAsInt("value.length", list));
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, runScriptAsInt("value.length", list));
        runScriptAsInt("value.length = 6", list);
        assertEquals(6, list.size());
        runScriptAsInt("value.length = 2", list);
        assertEquals(2, list.size());
    }

    @Test
    public void javaMethodsCalls() {
        List<Integer> list = new ArrayList<>();
        assertEquals(0, runScriptAsInt("value.size()", list));
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, runScriptAsInt("value.size()", list));
    }

    @Test
    public void updatingJavaListIntegerValues() {
        List<Number> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(2, runScriptAsInt("value[1]", list));
        // setting values in lists will set them as double
        assertEquals("5.0", runScriptAsString("value[1]=5;value[1]", list));
        assertEquals(5.0, list.get(1));
    }

    @Test
    public void accessingJavaListStringValues() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertEquals("b", runScriptAsString("value[1]", list));
        assertEquals("c", runScriptAsString("value[2]", list));
    }

    @Test
    public void updatingJavaListStringValues() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertEquals("b", runScriptAsString("value[1]", list));
        assertEquals("f", runScriptAsString("value[1]=\"f\";value[1]", list));
        assertEquals("f", list.get(1));
    }

    @Test
    public void autoGrow() {
        List<String> list = new ArrayList<>();
        // Object list = runScript("[]", null, Function.identity());
        assertEquals(0, runScriptAsInt("value.length", list));
        assertEquals(1, runScriptAsInt("value[0]='a'; value.length", list));
        assertEquals(3, runScriptAsInt("value[2]='c'; value.length", list));
        assertEquals("a", runScriptAsString("value[0]", list));
        // NativeArray will have 'undefined' here.
        assertEquals("null", runScriptAsString("value[1]", list));
        assertEquals("c", runScriptAsString("value[2]", list));
        // NativeList will return "a,,c"
        assertEquals("a,,c", runScriptAsString("Array.prototype.join.call(value)", list));
    }

    @Test
    public void length() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        runScriptAsString("value.length = 0", list);
        assertEquals(0, list.size());
        runScriptAsString("value.length = 10", list);
        assertEquals(10, list.size());

        try {
            runScriptAsString("value.length = -10", list);
            fail();
        } catch (EcmaError e) {
            assertEquals("RangeError: Inappropriate array length. (#1)", e.getMessage());
        }

        try {
            runScriptAsString("value.length = 2.1", list);
            fail();
        } catch (EcmaError e) {
            assertEquals("RangeError: Inappropriate array length. (#1)", e.getMessage());
        }

        try {
            runScriptAsString("value.length = 2147483648", list); // Integer.MAX_VALUE + 1
            fail();
        } catch (EcmaError e) {
            assertEquals("RangeError: Inappropriate array length. (#1)", e.getMessage());
        }
    }

    @Test
    public void delete() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        //        Object list = runScript("['a','b','c']", null, Function.identity());
        // TODO: should NativeJavaList distinguish between 'null' and 'undefined'?
        assertEquals("false", runScriptAsString("delete value[1]", list));
        assertEquals("a,,c", runScriptAsString("Array.prototype.join.call(value)", list));
    }

    @Test
    public void add() {
        List<String> list = new ArrayList<>();
        runScriptAsString("value[0] = 'a'", list);
        runScriptAsString("value[1] = 'b'", list);
        runScriptAsString("value[2] = 'c'", list);
        assertEquals("[a, b, c]", list.toString());
        runScriptAsString("value[5] = 'f'", list);
        assertEquals("[a, b, c, null, null, f]", list.toString());
        runScriptAsString("value[4] = 'e'", list);
        runScriptAsString("value[3] = 'd'", list);
        assertEquals("[a, b, c, d, e, f]", list.toString());
    }

    @Test
    public void keys() {
        List<String> list = new ArrayList<>();
        NativeArray resEmpty =
                (NativeArray) runScript("Object.keys(value)", list, Function.identity());
        Assert.assertEquals(0, resEmpty.size());

        list.add("a");
        list.add("b");
        list.add("c");

        NativeArray res = (NativeArray) runScript("Object.keys(value)", list, Function.identity());
        assertEquals(3, res.size());
        assertTrue(res.contains("0"));
        assertTrue(res.contains("1"));
        assertTrue(res.contains("2"));
    }

    private int runScriptAsInt(String scriptSourceText, Object value) {
        return runScript(scriptSourceText, value, Context::toNumber).intValue();
    }

    private String runScriptAsString(String scriptSourceText, Object value) {
        return runScript(scriptSourceText, value, Context::toString);
    }

    private <T> T runScript(String scriptSourceText, Object value, Function<Object, T> convert) {
        return ContextFactory.getGlobal()
                .call(
                        context -> {
                            Scriptable scope = context.newObject(global);
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return convert.apply(
                                    context.evaluateString(scope, scriptSourceText, "", 1, null));
                        });
    }
}
