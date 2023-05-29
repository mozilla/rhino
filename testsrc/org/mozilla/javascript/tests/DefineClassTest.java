/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;
import org.mozilla.javascript.annotations.JSStaticFunction;

public class DefineClassTest {

    Scriptable scope;

    @Test
    public void annotatedHostObject() {
        try (Context cx = Context.enter()) {
            Object result = evaluate(cx, "a = new AnnotatedHostObject(); a.initialized;");
            assertEquals(result, Boolean.TRUE);
            assertEquals(evaluate(cx, "a.instanceFunction();"), "instanceFunction");
            assertEquals(evaluate(cx, "a.namedFunction();"), "namedFunction");
            assertEquals(evaluate(cx, "AnnotatedHostObject.staticFunction();"), "staticFunction");
            assertEquals(
                    evaluate(cx, "AnnotatedHostObject.namedStaticFunction();"),
                    "namedStaticFunction");
            assertNull(evaluate(cx, "a.foo;"));
            assertEquals(evaluate(cx, "a.foo = 'foo'; a.foo;"), "FOO");
            assertEquals(evaluate(cx, "a.bar;"), "bar");

            // Setting a property with no setting should be silently
            // ignored in non-strict mode.
            evaluate(cx, "a.bar = 'new bar'");
            assertEquals("bar", evaluate(cx, "a.bar;"));
        }
    }

    @Test
    public void traditionalHostObject() {
        try (Context cx = Context.enter()) {
            Object result = evaluate(cx, "t = new TraditionalHostObject(); t.initialized;");
            assertEquals(result, Boolean.TRUE);
            assertEquals(evaluate(cx, "t.instanceFunction();"), "instanceFunction");
            assertEquals(evaluate(cx, "TraditionalHostObject.staticFunction();"), "staticFunction");
            assertNull(evaluate(cx, "t.foo;"));
            assertEquals(evaluate(cx, "t.foo = 'foo'; t.foo;"), "FOO");
            assertEquals(evaluate(cx, "t.bar;"), "bar");

            // Setting a property with no setting should be silently
            // ignored in non-strict mode.
            evaluate(cx, "t.bar = 'new bar'");
            assertEquals("bar", evaluate(cx, "t.bar;"));
        }
    }

    private Object evaluate(Context cx, String str) {
        return cx.evaluateString(scope, str, "<testsrc>", 0, null);
    }

    @Before
    public void init() throws Exception {
        try (Context cx = Context.enter()) {
            scope = cx.initStandardObjects();
            ScriptableObject.defineClass(scope, AnnotatedHostObject.class);
            ScriptableObject.defineClass(scope, TraditionalHostObject.class);
        }
    }

    public static class AnnotatedHostObject extends ScriptableObject {

        String foo, bar = "bar";

        public AnnotatedHostObject() {}

        @Override
        public String getClassName() {
            return "AnnotatedHostObject";
        }

        @JSConstructor
        public void jsConstructorMethod() {
            put("initialized", this, Boolean.TRUE);
        }

        @JSFunction
        public Object instanceFunction() {
            return "instanceFunction";
        }

        @JSFunction("namedFunction")
        public Object someFunctionName() {
            return "namedFunction";
        }

        @JSStaticFunction
        public static Object staticFunction() {
            return "staticFunction";
        }

        @JSStaticFunction("namedStaticFunction")
        public static Object someStaticFunctionName() {
            return "namedStaticFunction";
        }

        @JSGetter
        public String getFoo() {
            return foo;
        }

        @JSSetter
        public void setFoo(String foo) {
            this.foo = foo.toUpperCase();
        }

        @JSGetter("bar")
        public String getMyBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar.toUpperCase();
        }
    }

    public static class TraditionalHostObject extends ScriptableObject {

        String foo, bar = "bar";

        public TraditionalHostObject() {}

        @Override
        public String getClassName() {
            return "TraditionalHostObject";
        }

        public void jsConstructor() {
            put("initialized", this, Boolean.TRUE);
        }

        public Object jsFunction_instanceFunction() {
            return "instanceFunction";
        }

        public static Object jsStaticFunction_staticFunction() {
            return "staticFunction";
        }

        public String jsGet_foo() {
            return foo;
        }

        public void jsSet_foo(String fooStr) {
            foo = fooStr.toUpperCase();
        }

        public String jsGet_bar() {
            return bar;
        }

        // not a JS setter
        public void setBar(String bar) {
            this.bar = bar.toUpperCase();
        }
    }
}
