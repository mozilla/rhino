/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

/*
 * This testcase tests the 'toJson' functionality
 */
public class JsonTest extends TestCase {

    @Test
    public void testInteger() {
        String js = "JSON.stringify(obj)\n";
        testIt(js, new Integer(3), "3");
    }

    @Test
    public void testDouble() {
        String js = "JSON.stringify(obj)\n";
        testIt(js, new Double(3), "3");
    }

    @Test
    public void testString() {
        String js = "JSON.stringify(obj)\n";
        testIt(js, "3", "\"3\"");
    }

    @Test
    public void testJavaUtilDate() {
        String js = "JSON.stringify(obj)\n";
        Instant i = Instant.parse("2019-12-12T15:21:11Z");
        testIt(js, java.util.Date.from(i), "\"2019-12-12T15:21:11Z\"");
    }

    @Test
    public void testJavaUtilCalendar() {
        String js = "JSON.stringify(obj)\n";
        Instant i = Instant.parse("2019-12-12T15:21:11Z");

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(i.toEpochMilli());
        testIt(js, c, "\"2019-12-12T15:21:11Z\"");
    }

    @Test
    public void testJavaSqlDate() {
        String js = "JSON.stringify(obj)\n";
        java.sql.Date date = java.sql.Date.valueOf("2019-12-12");
        testIt(js, date, "\"2019-12-12\"");
    }

    @Test
    public void testJavaSqlTime() {
        String js = "JSON.stringify(obj)\n";
        java.sql.Time time = java.sql.Time.valueOf("15:21:11");
        testIt(js, time, "\"15:21:11\"");
    }

    @Test
    public void testJavaSqlTimestamp() {
        String js = "JSON.stringify(obj)\n";
        Instant i = Instant.parse("2019-12-12T15:21:11Z");
        java.sql.Timestamp ts = java.sql.Timestamp.from(i);
        testIt(js, ts, "\"2019-12-12T15:21:11Z\"");
    }

    @Test
    public void testJavaUuid() {
        String js = "JSON.stringify(obj)\n";
        UUID uuid = UUID.fromString("73ede6e0-958e-44a0-a673-52564b6dca34");
        testIt(js, uuid, "\"73ede6e0-958e-44a0-a673-52564b6dca34\"");
    }

    @Test
    public void testNestedSimple() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Object> list1 = new ArrayList<>();
        list1.add(3);
        list1.add(42.5);
        list1.add(true);
        list1.add("test\nstring");
        Instant i = Instant.parse("2019-12-12T15:21:11Z");
        list1.add(new Object[] {java.util.Date.from(i)});
        list1.add(null);

        List<Object> list2 = new ArrayList<>();
        map.put("list1", list1);
        map.put("list2", list2);

        String js = "JSON.stringify(obj)\n";
        testIt(
                js,
                map,
                "{\"list1\":[3,42.5,true,\"test\\nstring\",[\"2019-12-12T15:21:11Z\"],null],\"list2\":[]}");
    }

    @Test
    public void testNestedRecursive1() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Object> list1 = new ArrayList<>();
        list1.add(3);
        list1.add(42.5);
        list1.add(true);
        list1.add("test\nstring");
        Instant i = Instant.parse("2019-12-12T15:21:11Z");
        list1.add(new Object[] {java.util.Date.from(i)});
        list1.add(null);

        List<Object> list2 = new ArrayList<>();
        map.put("list1", list1);
        map.put("list2", list2);
        list2.add(map);

        String js =
                "var ret = ''; try { JSON.stringify(obj) } catch (e) { ret = e.message }; ret\n";
        testIt(js, map, "Cyclic java.util.LinkedHashMap value not allowed.");
    }

    @Test
    public void testNestedRecursive2() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("map", map);
        String js =
                "var ret = ''; try { JSON.stringify(obj) } catch (e) { ret = e.message }; ret\n";
        testIt(js, map, "Cyclic java.util.LinkedHashMap value not allowed.");
    }

    @Test
    public void testNestedRecursive3() {
        List<Object> list = new ArrayList<>();
        list.add(list);
        String js =
                "var ret = ''; try { JSON.stringify(obj) } catch (e) { ret = e.message }; ret\n";
        testIt(js, list, "Cyclic java.util.ArrayList value not allowed.");
    }

    @Test
    public void testNativeMap() {
        String js = "JSON.stringify(obj)\n";
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key1", "one");
        map.put("key2", 2);
        map.put("key3", null);
        testIt(js, map, "{\"key1\":\"one\",\"key2\":2,\"key3\":null}");
    }

    @Test
    public void testNativeList() {
        String js = "JSON.stringify(obj)\n";
        List<Object> list = new ArrayList<>();
        list.add("one");
        list.add(2);
        list.add(null);
        testIt(js, list, "[\"one\",2,null]");
    }

    private static class MyObj {
        public String toString() {
            return "this is my toString method";
        }
    }

    @Test
    public void testJavaToString1() {
        String js =
                "var ret = ''; try { JSON.stringify(obj) } catch (e) { ret = e.message }; ret\n";
        testIt(
                js,
                new MyObj(),
                "Do not know how to serialize a org.mozilla.javascript.tests.JsonTest$MyObj");
    }

    @Test
    public void testJavaToString2() {
        String js =
                "JSON.javaConverter = x => x.toString();\n"
                        + "var ret = ''; try { ret = JSON.stringify(obj) } catch (e) { ret = e.message }; ret\n";
        testIt(js, new MyObj(), "\"this is my toString method\"");
    }

    private void testIt(String script, Object obj, String expected) {
        Utils.runWithAllOptimizationLevels(
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        return super.hasFeature(cx, featureIndex)
                                || featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS;
                    }
                },
                cx -> {
                    final ScriptableObject scope = cx.initStandardObjects();
                    scope.put("obj", scope, cx.getWrapFactory().wrap(cx, scope, obj, null));
                    Object o = cx.evaluateString(scope, script, "testJavaArrayIterate.js", 1, null);
                    assertEquals(expected, o);

                    return null;
                });
    }
}
