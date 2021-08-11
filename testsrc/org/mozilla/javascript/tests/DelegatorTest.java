/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Delegator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

/** Unit tests for Delegator. */
public class DelegatorTest {

    private Context cx;
    private Scriptable root;

    @Before
    public void init() throws Exception {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        root = cx.initStandardObjects();

        Scriptable delegee = ScriptRuntime.toObject(root, "Rhino");
        ScriptableObject.defineProperty(root, "TestDelegee", delegee, 0);

        Delegator delegator = new Delegator(delegee);
        ScriptableObject.defineProperty(root, "TestDelegator", delegator, 0);

        delegator = new Delegator(delegee);
        ScriptableObject.defineProperty(root, "TestDelegator2", delegator, 0);

        ScriptableObject.defineClass(root, AnnotatedHostObject.class);
        delegee = ScriptRuntime.newObject(cx, root, "AnnotatedHostObject", null);
        delegator = new Delegator(delegee);
        ScriptableObject.defineProperty(root, "TestDelegator3", delegator, 0);
    }

    @After
    public void cleanup() {
        Context.exit();
    }

    public static void init(Scriptable scope) {}

    private Object eval(String source) {
        return cx.evaluateString(root, source, "test.js", 1, null);
    }

    @Test
    public void typeof() {
        assertEquals(eval("typeof TestDelegee"), eval("typeof TestDelegator"));
    }

    @Test
    public void equals() {
        assertEquals(Boolean.TRUE, eval("TestDelegee == TestDelegator"));
        assertEquals(Boolean.TRUE, eval("TestDelegator == TestDelegee"));
        assertEquals(Boolean.TRUE, eval("TestDelegator == TestDelegator2"));

        assertEquals(Boolean.TRUE, eval("TestDelegee === TestDelegator"));
        assertEquals(Boolean.TRUE, eval("TestDelegator === TestDelegee"));
        assertEquals(Boolean.TRUE, eval("TestDelegator === TestDelegator2"));
    }

    @Test
    public void call() {
        assertEquals("instanceFunction", eval("TestDelegator3.instanceFunction();"));
    }

    @Test
    public void getter() {
        assertEquals("Foo", eval("TestDelegator3.foo;"));
    }

    @Test
    public void setter() {
        assertEquals(
                "Rhino",
                eval("TestDelegator3.foo = TestDelegator; TestDelegator3.foo;").toString());
    }

    @Test
    public void defineProperty() {
        assertEquals(
                42,
                eval(
                        "Object.defineProperty(TestDelegator3, 'testProp', {"
                                + "value: 42,"
                                + "writable: true,"
                                + "enumerable: true,"
                                + "configurable: true});"
                                + "TestDelegator3.testProp;"));
    }

    @Test
    public void setPrototypeOf() {
        assertEquals(5, eval("a = {}; Object.setPrototypeOf(a, TestDelegator); a.length;"));
    }

    @Test
    public void map() {
        String script =
                "var result = '';\n"
                        + "var map = new Map();\n"
                        + "map.set(TestDelegator, 'test');\n"
                        + "result += map.has(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result += map.get(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result += map.delete(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#test#true#", eval(script));

        script =
                "var result = '';\n"
                        + "var map = new Map();\n"
                        + "map.set(TestDelegator, 'test');\n"
                        + "result += map.has(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result += map.get(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result += map.delete(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#test#true#", eval(script));
    }

    @Test
    public void weakMap() {
        String script =
                "var result = '';\n"
                        + "var map = new WeakMap();\n"
                        + "map.set(TestDelegator, 'test');\n"
                        + "result += map.has(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result += map.get(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result += map.delete(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#test#true#", eval(script));

        script =
                "var result = '';\n"
                        + "var map = new WeakMap();\n"
                        + "map.set(TestDelegator, 'test');\n"
                        + "result += map.has(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result += map.get(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result += map.delete(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#test#true#", eval(script));
    }

    @Test
    public void set() {
        String script =
                "var result = '';\n"
                        + "var set = new Set();\n"
                        + "set.add(TestDelegator);\n"
                        + "result += set.has(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result += set.delete(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#true#", eval(script));

        script =
                "var result = '';\n"
                        + "var set = new Set();\n"
                        + "set.add(TestDelegator);\n"
                        + "result += set.has(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result += set.delete(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#true#", eval(script));
    }

    @Test
    public void weakSet() {
        String script =
                "var result = '';\n"
                        + "var set = new WeakSet();\n"
                        + "set.add(TestDelegator);\n"
                        + "result += set.has(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result += set.delete(TestDelegator);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#true#", eval(script));

        script =
                "var result = '';\n"
                        + "var set = new WeakSet();\n"
                        + "set.add(TestDelegator);\n"
                        + "result += set.has(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result += set.delete(TestDelegee);\n"
                        + "result += '#';\n"
                        + "result;";
        assertEquals("true#true#", eval(script));
    }

    public static class AnnotatedHostObject extends ScriptableObject {

        Object foo = "Foo";

        public AnnotatedHostObject() {}

        @Override
        public String getClassName() {
            return "AnnotatedHostObject";
        }

        @JSConstructor
        public void jsConstructorMethod() {}

        @JSGetter
        public Object getFoo() {
            return foo;
        }

        @JSSetter
        public void setFoo(Object foo) {
            this.foo = foo;
        }

        @JSFunction
        public Object instanceFunction() {
            return "instanceFunction";
        }
    }
}
