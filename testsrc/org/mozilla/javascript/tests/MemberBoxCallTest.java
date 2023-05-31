/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

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

public class MemberBoxCallTest {

    Scriptable scope;

    @Test
    public void prototypeProperty() {
        try (Context cx = Context.enter()) {
            assertEquals(
                    "SUPERVAL",
                    evaluate(
                            cx,
                            "var hostObj = new AnnotatedHostObject(); "
                                    + "var valueProperty = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(hostObj), \"foo\");"
                                    + "var result = 'failed';"
                                    + "if( valueProperty.get && valueProperty.set ) {"
                                    + "valueProperty.set.call(hostObj, 'superVal');"
                                    + "result = valueProperty.get.call(hostObj);"
                                    + "}"
                                    + "result;"));
        }
    }

    @Test
    public void propertyGetterName() {
        try (Context cx = Context.enter()) {
            assertEquals(
                    "foo",
                    evaluate(
                            cx,
                            "var hostObj = new AnnotatedHostObject(); "
                                    + "var valueProperty = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(hostObj), \"foo\");"
                                    + "var result = 'failed';"
                                    + "if( valueProperty.get && valueProperty.set ) {"
                                    + "result = '' + valueProperty.get.name;"
                                    + "}"
                                    + "result;"));
        }
    }

    @Test
    public void propertySetterName() {
        try (Context cx = Context.enter()) {
            assertEquals(
                    "foo",
                    evaluate(
                            cx,
                            "var hostObj = new AnnotatedHostObject(); "
                                    + "var valueProperty = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(hostObj), \"foo\");"
                                    + "var result = 'failed';"
                                    + "if( valueProperty.get && valueProperty.set ) {"
                                    + "result = '' + valueProperty.set.name;"
                                    + "}"
                                    + "result;"));
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
}
