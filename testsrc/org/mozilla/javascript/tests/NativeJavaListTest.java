/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

/** From @makusuko (Markus Sunela), imported from PR https://github.com/mozilla/rhino/pull/561 */
public class NativeJavaListTest extends TestCase {
    protected final Global global = new Global();

    public NativeJavaListTest() {
        global.init(ContextFactory.getGlobal());
    }

    public void testAccessingNullValues() {
        List<Integer> list = new ArrayList<>();
        list.add(null);

        assertEquals(null, runScript("value[0]", list, Function.identity()));
        assertEquals(1, runScriptAsInt("value.length", list));
    }

    public void testAutoGrowList() {
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

    public void testAccessingJavaListIntegerValues() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(2, runScriptAsInt("value[1]", list));
        assertEquals(3, runScriptAsInt("value[2]", list));
        assertEquals(3, runScriptAsInt("value.length", list));
    }

    public void testLengthProperty() {
        List<Integer> list = new ArrayList<>();
        assertEquals(0, runScriptAsInt("value.length", list));
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, runScriptAsInt("value.length", list));
    }

    public void testJavaMethodsCalls() {
        List<Integer> list = new ArrayList<>();
        assertEquals(0, runScriptAsInt("value.size()", list));
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, runScriptAsInt("value.size()", list));
    }

    public void testUpdatingJavaListIntegerValues() {
        List<Number> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(2, runScriptAsInt("value[1]", list));
        assertEquals(5, runScriptAsInt("value[1]=5;value[1]", list));
        assertEquals(5, list.get(1).intValue());
    }

    public void testAccessingJavaListStringValues() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertEquals("b", runScriptAsString("value[1]", list));
        assertEquals("c", runScriptAsString("value[2]", list));
    }

    public void testUpdatingJavaListStringValues() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertEquals("b", runScriptAsString("value[1]", list));
        assertEquals("f", runScriptAsString("value[1]=\"f\";value[1]", list));
        assertEquals("f", list.get(1));
    }

    public void testAutoGrow() {
        List<String> list = new ArrayList<>();
        // Object list = runScript("[]", null, Function.identity());
        assertEquals(0, runScriptAsInt("value.length", list));
        assertEquals(1, runScriptAsInt("value[0]='a'; value.length", list));
        assertEquals(3, runScriptAsInt("value[2]='c'; value.length", list));
        assertEquals("a", runScriptAsString("value[0]", list));
        // NativeList will have 'undefined' here.
        assertEquals("null", runScriptAsString("value[1]", list));
        assertEquals("c", runScriptAsString("value[2]", list));
        // NativeList will return "a,,c"
        assertEquals("a,,c", runScriptAsString("value.join()", list));
    }

    public void testLength() {
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

    public void testDelete() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        //        Object list = runScript("['a','b','c']", null, Function.identity());
        // TODO: should NativeJavaList distinguish between 'null' and 'undefined'?
        assertEquals("false", runScriptAsString("delete value[1]", list));
        assertEquals("a,,c", runScriptAsString("value.join()", list));
    }

    public void testArrayConcat() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("x,a,b,c", runScriptAsString("['x'].concat(value).join()", list));
        assertEquals("a,b,c,x", runScriptAsString("value.concat(['x']).join()", list));
    }

    public void testArrayCopyWithin() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        // copy to index 0 the element at index 3
        assertEquals("d,b,c,d,e", runScriptAsString("value.copyWithin(0, 3, 4).join()", list));
    }

    public void testArrayFill() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("a,d,d", runScriptAsString("value.fill('d', 1, 4).join()", list));
        assertEquals("[a, d, d]", list.toString());
    }

    public void testArrayIncludes() {

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("true", runScriptAsString("value.includes('b')", list));
        assertEquals("false", runScriptAsString("value.includes('d')", list));

        List<Double> listD = new ArrayList<>();
        listD.add(1.0);
        listD.add(2.0);
        listD.add(3.0);
        assertEquals("true", runScriptAsString("value.includes(2)", listD));
        assertEquals("false", runScriptAsString("value.includes(4)", listD));

        List<Integer> listI = new ArrayList<>();
        listI.add(1);
        listI.add(2);
        listI.add(3);
        assertEquals("true", runScriptAsString("value.includes(2)", listI));
        assertEquals("false", runScriptAsString("value.includes(4)", listD));
    }

    public void testArrayIndexOf() {

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("1", runScriptAsString("value.indexOf('b')", list));
        assertEquals("-1", runScriptAsString("value.indexOf('d')", list));

        List<Double> listD = new ArrayList<>();
        listD.add(1.0);
        listD.add(2.0);
        listD.add(3.0);
        assertEquals("1", runScriptAsString("value.indexOf(2)", listD));
        assertEquals("-1", runScriptAsString("value.indexOf(4)", listD));

        List<Integer> listI = new ArrayList<>();
        listI.add(1);
        listI.add(2);
        listI.add(3);

        // FIXME: This will invoke the java.util.List.indexOf method, which
        // is not yet type aware!
        // assertEquals("1", runScriptAsString("value.indexOf(2)", listI));
        // assertEquals("-1", runScriptAsString("value.indexOf(4)", listD));

        assertEquals("1", runScriptAsString("Array.indexOf(value, 2)", listI));
        assertEquals("-1", runScriptAsString("Array.indexOf(value, 4)", listD));
    }

    public void testArrayLastIndexOf() {

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("1", runScriptAsString("value.lastIndexOf('b')", list));
        assertEquals("-1", runScriptAsString("value.lastIndexOf('d')", list));

        List<Double> listD = new ArrayList<>();
        listD.add(1.0);
        listD.add(2.0);
        listD.add(3.0);
        assertEquals("1", runScriptAsString("value.lastIndexOf(2)", listD));
        assertEquals("-1", runScriptAsString("value.lastIndexOf(4)", listD));

        List<Integer> listI = new ArrayList<>();
        listI.add(1);
        listI.add(2);
        listI.add(3);

        // FIXME: This will invoke the java.util.List.indexOf method, which
        // is not yet type aware! This means, the argument is always converted
        // to double, so we only may find elements in listD
        assertEquals("-1", runScriptAsString("value.lastIndexOf(2)", listI));
        assertEquals("-1", runScriptAsString("value.lastIndexOf(2.0)", listI));
        assertEquals("1", runScriptAsString("value.lastIndexOf(2)", listD));
        assertEquals("1", runScriptAsString("value.lastIndexOf(2.0)", listD));

        // elements in list
        assertEquals("1", runScriptAsString("Array.lastIndexOf(value, 2.0)", listI));
        assertEquals("1", runScriptAsString("Array.lastIndexOf(value, 2.0)", listD));
        assertEquals("1", runScriptAsString("Array.lastIndexOf(value, 2)", listI));
        assertEquals("1", runScriptAsString("Array.lastIndexOf(value, 2)", listD));
        // elements not found
        assertEquals("-1", runScriptAsString("Array.lastIndexOf(value, 4.0)", listD));
        assertEquals("-1", runScriptAsString("Array.lastIndexOf(value, 4)", listI));
    }

    public void testArrayIsArray() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        // CHECKME: what would we expect here?
        assertEquals("false", runScriptAsString("Array.isArray(value)", list));
    }

    public void testInstanceOfArray() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("true", runScriptAsString("(value instanceof Object)", list));
        assertEquals("true", runScriptAsString("(value instanceof JavaObject)", list));
        assertEquals("true", runScriptAsString("(value instanceof JavaList)", list));
        assertEquals("true", runScriptAsString("(value instanceof java.util.List)", list));
        assertEquals("true", runScriptAsString("(value instanceof java.util.ArrayList)", list));
        assertEquals("false", runScriptAsString("(value instanceof Array)", list));
    }

    public void testArrayJoin() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("a-b-c", runScriptAsString("value.join('-')", list));
    }

    public void testArrayPop() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("c", runScriptAsString("value.pop()", list));
        assertEquals("[a, b]", list.toString());
    }

    public void testArrayPush() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("4", runScriptAsString("value.push('d')", list));
        assertEquals("[a, b, c, d]", list.toString());
    }

    public void testArrayReverse() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("c,b,a", runScriptAsString("value.reverse().join()", list));
    }

    public void testArrayShift() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("a", runScriptAsString("value.shift()", list));
        assertEquals("[b, c]", list.toString());
    }

    public void testArraySlice() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        assertEquals("b,c", runScriptAsString("value.slice(1, 3)", list));
    }

    public void testArraySortString() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("d");
        list.add("e");
        list.add("b");
        list.add("c");
        runScriptAsString("Array.sort(value)", list);
        assertEquals("[a, b, c, d, e]", list.toString());
    }

    public void testArraySortStringJavaComp() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("d");
        list.add("e");
        list.add("b");
        list.add("c");
        // java.util.List.sort will override Array.sort
        runScriptAsString("value.sort(java.util.Comparator.naturalOrder())", list);
        assertEquals("[a, b, c, d, e]", list.toString());
    }

    private List<Number> getNumberList() {
        return new ArrayList<>(Arrays.asList(4, 10, 2, 3, 0));
    }

    public void testArraySortNumbers() {
        List<Number> list = getNumberList();
        // NOTE: When Array.sort sorts on string representation!
        assertEquals(Integer.class, list.get(0).getClass());
        runScriptAsString("Array.sort(value);", list);
        assertEquals("[0, 10, 2, 3, 4]", list.toString());
        assertEquals(Integer.class, list.get(0).getClass());
    }

    public void testArraySortNumbersNoWrap() {
        List<Number> list = getNumberList();
        assertEquals(Integer.class, list.get(0).getClass());
        // NOTE: When we do not wrap primitives, the type of values can change
        runScriptDontWrapPrimitive("Array.sort(value);", list);
        assertEquals("[0.0, 10.0, 2.0, 3.0, 4.0]", list.toString());
        assertEquals(Double.class, list.get(0).getClass()); // Attention: Type has changed!
    }

    public void testArraySortNumberJavaComp() {
        List<Number> list = getNumberList();
        runScriptAsString("value.sort(java.util.Comparator.naturalOrder())", list);
        assertEquals("[0, 2, 3, 4, 10]", list.toString());
        assertEquals(Integer.class, list.get(0).getClass());
    }

    public void testArraySortNumberJavaCompNoWrap() {
        List<Number> list = getNumberList();
        runScriptDontWrapPrimitive("value.sort(java.util.Comparator.naturalOrder())", list);
        assertEquals("[0, 2, 3, 4, 10]", list.toString());
        assertEquals(Integer.class, list.get(0).getClass());
    }

    public void testArraySortNumberJsComp() {
        List<Number> list = getNumberList();
        runScriptAsString("Array.sort(value, (a,b) => a-b);", list);
        assertEquals("[0, 2, 3, 4, 10]", list.toString());
        assertEquals(Integer.class, list.get(0).getClass());
    }

    public void testArraySortNumberJsCompNoWrap() {
        List<Number> list = getNumberList();
        runScriptDontWrapPrimitive("Array.sort(value, (a,b) => a-b);", list);
        assertEquals("[0.0, 2.0, 3.0, 4.0, 10.0]", list.toString());
        assertEquals(Double.class, list.get(0).getClass());
    }

    public void testArraySplice() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        // java.util.List.sort will override Array.sort
        runScriptAsString("value.splice(1, 0, 'x')", list);
        assertEquals("[a, x, b, c]", list.toString());
    }

    public void testArrayUnshift() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals(4, runScriptAsInt("value.unshift('x')", list));
        assertEquals("[x, a, b, c]", list.toString());
    }

    public void testKeys() {
        List<String> list = new ArrayList<>();
        NativeArray resEmpty =
                (NativeArray) runScript("Object.keys(value)", list, Function.identity());
        assertEquals(0, resEmpty.size());

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
                                    context.evaluateString(
                                            scope,
                                            "Object.getOwnPropertyNames(Array.prototype).forEach(function(x) { JavaList.prototype[x] = Array.prototype[x]});"
                                                    + scriptSourceText,
                                            "",
                                            1,
                                            null));
                        });
    }

    private void runScriptDontWrapPrimitive(String scriptSourceText, Object value) {
        ContextFactory.getGlobal()
                .call(
                        context -> {
                            context.getWrapFactory().setJavaPrimitiveWrap(false);
                            Scriptable scope = context.newObject(global);
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return context.evaluateString(scope, scriptSourceText, "", 1, null);
                        });
    }
}
