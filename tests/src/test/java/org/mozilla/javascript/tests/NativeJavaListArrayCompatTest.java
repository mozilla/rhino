/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Test all Array-prototype function against NativeJavaList
 *
 * @author Roland Praml, FOCONIS AG
 */
public class NativeJavaListArrayCompatTest extends TestCase {

    public void testArrayConcat() {
        List<String> list = abcList();
        assertEquals(
                Arrays.asList("x", "a", "b", "c"),
                runScript("['x'].concat(value)", list, Function.identity()));
        assertEquals(
                Arrays.asList("a", "b", "c", "x"),
                runScript("Array.prototype.concat.call(value, ['x'])", list, Function.identity()));
    }

    public void testArrayCopyWithin() {
        List<String> list = abcList();
        list.add("d");
        list.add("e");
        // copy to index 0 the element at index 3
        runScriptAsString("Array.prototype.copyWithin.call(value, 0, 3, 4)", list);
        assertEquals(Arrays.asList("d", "b", "c", "d", "e"), list);
    }

    public void testArrayEntries() {
        List<String> list = abcList();

        assertEquals(
                "0,a,1,b,2,c,",
                runScriptAsString(
                        "var iter = Array.prototype.entries.call(value);\n"
                                + "var ret = '';\n"
                                + "for (e of iter) { ret += e + ','; }\n"
                                + "ret;",
                        list));
    }

    public void testArrayEvery() {
        List<String> list = abcList();

        assertEquals(1, runScriptAsInt("Array.prototype.every.call(value, e => e < 'd')", list));
        assertEquals(0, runScriptAsInt("Array.prototype.every.call(value, e => e < 'b')", list));
    }

    public void testArrayFill() {
        List<String> list = abcList();
        runScriptAsString("Array.prototype.fill.call(value, 'd', 1, 4)", list);
        assertEquals("[a, d, d]", list.toString());
    }

    public void testArrayFilter() {
        List<String> list = abcList();
        assertEquals(
                "a,b", runScriptAsString("Array.prototype.filter.call(value, e => e < 'c')", list));
    }

    public void testArrayFind() {
        List<String> list = abcList();
        assertEquals(
                "c", runScriptAsString("Array.prototype.find.call(value, e => e > 'b')", list));
    }

    public void testArrayFindIndex() {
        List<String> list = abcList();
        assertEquals(
                2, runScriptAsInt("Array.prototype.findIndex.call(value, e => e > 'b')", list));
    }

    public void testArrayForEach() {
        List<String> list = abcList();
        assertEquals(
                "a@0,b@1,c@2,",
                runScriptAsString(
                        "var ret='';\n"
                                + "Array.prototype.forEach.call(value, (e,i) => ret += e + '@' + i + ',');\n"
                                + "ret",
                        list));
        // note: forEach is also implemented in java.util.Iterable, but does not support second
        // parameter
        list = abcList();
        assertEquals(
                "a@undefined,b@undefined,c@undefined,",
                runScriptAsString(
                        "var ret='';\n"
                                + "value.forEach((e,i) => ret += e + '@' + i + ',');\n"
                                + "ret",
                        list));
    }

    public void testArrayFrom() {
        List<String> list = abcList();
        Object from = runScript("Array.from(value)", list, Function.identity());
        assertTrue(from instanceof NativeArray);
        assertEquals(list, from);
    }

    public void testArrayIncludes() {

        List<String> list = abcList();
        assertEquals("true", runScriptAsString("Array.prototype.includes.call(value, 'b')", list));
        assertEquals("false", runScriptAsString("Array.prototype.includes.call(value, 'd')", list));

        List<Double> listD = new ArrayList<>();
        listD.add(1.0);
        listD.add(2.0);
        listD.add(3.0);
        assertEquals("true", runScriptAsString("Array.prototype.includes.call(value, 2)", listD));
        assertEquals("false", runScriptAsString("Array.prototype.includes.call(value, 4)", listD));

        List<Integer> listI = new ArrayList<>();
        listI.add(1);
        listI.add(2);
        listI.add(3);
        assertEquals("true", runScriptAsString("Array.prototype.includes.call(value, 2)", listI));
        assertEquals("false", runScriptAsString("Array.prototype.includes.call(value, 4)", listD));
    }

    public void testArrayIndexOf() {

        List<String> list = abcList();
        assertEquals(1, runScriptAsInt("value.indexOf('b')", list));
        assertEquals(-1, runScriptAsInt("value.indexOf('d')", list));
        assertEquals(1, runScriptAsInt("Array.prototype.indexOf.call(value,'b')", list));
        assertEquals(-1, runScriptAsInt("Array.prototype.indexOf.call(value,'d')", list));

        List<Double> listD = new ArrayList<>();
        listD.add(1.0);
        listD.add(2.0);
        listD.add(3.0);
        assertEquals(1, runScriptAsInt("value.indexOf(2)", listD));
        assertEquals(-1, runScriptAsInt("value.indexOf(4)", listD));
        assertEquals(1, runScriptAsInt("Array.prototype.indexOf.call(value, 2)", listD));
        assertEquals(-1, runScriptAsInt("Array.prototype.indexOf.call(value, 4)", listD));

        List<Integer> listI = new ArrayList<>();
        listI.add(1);
        listI.add(2);
        listI.add(3);

        // NOTE: This will invoke the java.util.List.indexOf method, which
        // is not type aware! So the argument is converted to a double and we will not find '2.0'
        // in the integer list
        assertEquals(-1, runScriptAsInt("value.indexOf(2)", listI));
        assertEquals(1, runScriptAsInt("value.indexOf(2)", listD));

        // but we will find it with the Array.indexOf method
        assertEquals(1, runScriptAsInt("Array.prototype.indexOf.call(value, 2)", listI));
        assertEquals(1, runScriptAsInt("Array.prototype.indexOf.call(value, 2)", listD));
    }

    public void testArrayIsArray() {
        List<String> list = abcList(); // list is only array-like, but not instance of Array
        assertEquals("false", runScriptAsString("Array.isArray(value)", list));
    }

    public void testArrayJoin() {
        List<String> list = abcList();
        assertEquals("a-b-c", runScriptAsString("Array.prototype.join.call(value, '-')", list));
    }

    public void testArrayKeys() {
        List<String> list = abcList();
        assertEquals(
                "0,1,2,",
                runScriptAsString(
                        "var ret = '';\n"
                                + "var iter = Array.prototype.keys.call(value);\n"
                                + "for (e of iter) { ret += e + ',' };\n"
                                + "ret",
                        list));
    }

    public void testArrayLastIndexOf() {

        List<String> list = abcList();
        assertEquals(1, runScriptAsInt("Array.prototype.lastIndexOf.call(value,'b')", list));
        assertEquals(-1, runScriptAsInt("Array.prototype.lastIndexOf.call(value,'d')", list));

        List<Double> listD = new ArrayList<>();
        listD.add(1.0);
        listD.add(2.0);
        listD.add(3.0);

        List<Integer> listI = new ArrayList<>();
        listI.add(1);
        listI.add(2);
        listI.add(3);

        // NOTE: This will invoke the java.util.List.indexOf method, which
        // is not type aware! So the argument is converted to a double and we will not find '2.0'
        // in the integer list
        assertEquals(-1, runScriptAsInt("value.lastIndexOf(2)", listI));
        assertEquals(1, runScriptAsInt("value.lastIndexOf(2)", listD));

        // but we will find it with the Array.indexOf method
        assertEquals(1, runScriptAsInt("Array.prototype.indexOf.call(value, 2)", listI));
        assertEquals(1, runScriptAsInt("Array.prototype.indexOf.call(value, 2)", listD));
    }

    public void testInstanceOfArray() {
        List<String> list = abcList();
        assertEquals(0, runScriptAsInt("(value instanceof Object)", list));
        assertEquals(1, runScriptAsInt("(value instanceof java.util.List)", list));
        assertEquals(1, runScriptAsInt("(value instanceof java.util.ArrayList)", list));
        assertEquals(0, runScriptAsInt("(value instanceof Array)", list));
    }

    public void testArrayMap() {
        List<Number> list = numberList();
        assertEquals(
                "8,20,4,6,0", runScriptAsString("Array.prototype.map.call(value, x => x*2)", list));
    }

    public void testArrayPop() {
        List<String> list = abcList();
        assertEquals("c", runScriptAsString("Array.prototype.pop.call(value)", list));
        assertEquals("[a, b]", list.toString());
    }

    public void testArrayPush() {
        List<String> list = abcList();
        assertEquals("4", runScriptAsString("Array.prototype.push.call(value, 'd')", list));
        assertEquals("[a, b, c, d]", list.toString());
    }

    public void testArrayReduce() {
        List<Number> list = numberList();
        assertEquals(19, runScriptAsInt("Array.prototype.reduce.call(value, (a,b) => a+b)", list));
        List<String> abc = abcList();
        assertEquals(
                "abc", runScriptAsString("Array.prototype.reduce.call(value, (a,b) => a+b)", abc));
    }

    public void testArrayReduceRight() {
        List<Number> list = numberList();
        assertEquals(
                19, runScriptAsInt("Array.prototype.reduceRight.call(value, (a,b) => a+b)", list));
        List<String> abc = abcList();
        assertEquals(
                "cba",
                runScriptAsString("Array.prototype.reduceRight.call(value, (a,b) => a+b)", abc));
    }

    public void testArrayReverse() {
        List<String> list = abcList();
        runScriptAsString("Array.prototype.reverse.call(value)", list);
        assertEquals(Arrays.asList("c", "b", "a"), list);
    }

    public void testArrayShift() {
        List<String> list = abcList();
        assertEquals("a", runScriptAsString("Array.prototype.shift.call(value)", list));
        assertEquals(Arrays.asList("b", "c"), list);
    }

    public void testArraySlice() {
        List<String> list = abcList();
        list.add("d");
        list.add("e");
        assertEquals("b,c", runScriptAsString("Array.prototype.slice.call(value, 1, 3)", list));
    }

    public void testArraySome() {
        List<String> list = abcList();

        assertEquals(0, runScriptAsInt("Array.prototype.some.call(value, e => e > 'd')", list));
        assertEquals(1, runScriptAsInt("Array.prototype.some.call(value, e => e > 'b')", list));
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

    public void testArraySortNumbers() {
        List<Number> list = numberList();
        // NOTE: When Array.sort sorts on string representation!
        assertEquals(Integer.class, list.get(0).getClass());
        runScriptAsString("Array.prototype.sort.call(value);", list);
        assertEquals("[0, 10, 2, 3, 4]", list.toString());
        assertEquals(Integer.class, list.get(0).getClass());
        runScriptAsString("Array.prototype.sort.call(value, (a,b)=>b-a);", list);
        assertEquals("[10, 4, 3, 2, 0]", list.toString());
    }

    public void testArraySortNumbersNoWrap() {
        List<Number> list = numberList();
        assertEquals(Integer.class, list.get(0).getClass());
        // NOTE: When we do not wrap primitives, the type of values can change
        runScriptDontWrapPrimitive("Array.prototype.sort.call(value);", list);
        assertEquals("[0.0, 10.0, 2.0, 3.0, 4.0]", list.toString());
        assertEquals(Double.class, list.get(0).getClass()); // Attention: Type has changed!
    }

    public void testArraySortNumberJavaComp() {
        List<Number> list = numberList();
        runScriptAsString("value.sort(java.util.Comparator.naturalOrder())", list);
        assertEquals("[0, 2, 3, 4, 10]", list.toString());
        assertEquals(Integer.class, list.get(0).getClass());
    }

    public void testArraySplice() {
        List<String> list = abcList();
        runScriptAsString("Array.prototype.splice.call(value, 1, 0, 'x')", list);
        assertEquals("[a, x, b, c]", list.toString());
    }

    public void testArrayUnshift() {
        List<String> list = abcList();
        assertEquals(4, runScriptAsInt("Array.prototype.unshift.call(value, 'x')", list));
        assertEquals("[x, a, b, c]", list.toString());
    }

    public void testObjectKeys() {
        List<String> list = new ArrayList<>();
        NativeArray arr;

        arr = (NativeArray) runScript("Object.keys(value)", list, Function.identity());
        assertEquals(0, arr.size());

        list = abcList();

        arr = (NativeArray) runScript("Object.keys(value)", list, Function.identity());
        assertEquals(Arrays.asList("0", "1", "2"), arr);
        arr = (NativeArray) runScript("Object.values(value)", list, Function.identity());
        assertEquals(Arrays.asList("a", "b", "c"), arr);
    }

    private List<String> abcList() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        return list;
    }

    private List<Number> numberList() {
        return new ArrayList<>(Arrays.asList(4, 10, 2, 3, 0));
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
                            context.setLanguageVersion(Context.VERSION_ES6);
                            Scriptable scope = context.initStandardObjects(new Global());
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return convert.apply(
                                    context.evaluateString(scope, scriptSourceText, "", 1, null));
                        });
    }

    private void runScriptDontWrapPrimitive(String scriptSourceText, Object value) {
        ContextFactory.getGlobal()
                .call(
                        context -> {
                            context.setLanguageVersion(Context.VERSION_ES6);
                            context.getWrapFactory().setJavaPrimitiveWrap(false);
                            Scriptable scope = context.initStandardObjects(new Global());
                            scope.put("value", scope, Context.javaToJS(value, scope));
                            return context.evaluateString(scope, scriptSourceText, "", 1, null);
                        });
    }
}
