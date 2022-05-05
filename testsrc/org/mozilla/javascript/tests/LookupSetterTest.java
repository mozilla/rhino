/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class LookupSetterTest {
    private final String defineSetterAndGetterX =
            "Foo.__defineSetter__('x', function(val) { Foo.value = val; }); \n"
                    + "Foo.__defineGetter__('x', function() {return 'hello' + value; });\n";

    @Test
    public void typeof() throws Exception {
        test("function", "typeof Foo.__lookupGetter__('x');");
        test("function", "typeof Foo.__lookupSetter__('x');");

        test("function", "typeof (new Foo()).__lookupGetter__('s')");
        test("function", "typeof (new Foo()).__lookupSetter__('s')");

        test(
                "true",
                "Object.getPrototypeOf((new Foo()).__lookupGetter__('s')) === Function.prototype");
        test(
                "true",
                "Object.getPrototypeOf((new Foo()).__lookupSetter__('s')) === Function.prototype");

        test("true", "Object.getPrototypeOf(Foo.__lookupGetter__('x')) === Function.prototype");
        test("true", "Object.getPrototypeOf(Foo.__lookupSetter__('x')) === Function.prototype");
    }

    @Test
    public void callLookedUpGetter() throws Exception {
        test("hello", "new Foo().s;");
        test("hello", "var f = new Foo(); f.__lookupGetter__('s').call(f);");
    }

    @Test
    public void lookedUpGetter_toString() throws Exception {
        test(
                "function s() {\n\t[native code, arity=0]\n}\n",
                "new Foo().__lookupGetter__('s').toString()");
    }

    @Test
    public void lookedUpGetter_equals() throws Exception {
        test("true", "new Foo().__lookupGetter__('s') == new Foo().__lookupGetter__('s')");
    }

    @Test
    public void getOwnPropertyDescriptor_equals() throws Exception {
        test(
                "true",
                "Object.getOwnPropertyDescriptor(Foo, 'x').get ==="
                        + " Object.getOwnPropertyDescriptor(Foo, 'x').get");
        test(
                "true",
                "Object.getOwnPropertyDescriptor(Foo, 'x').set ==="
                        + " Object.getOwnPropertyDescriptor(Foo, 'x').set");

        test(
                "true",
                "Object.getOwnPropertyDescriptor(Object.getPrototypeOf(new Foo()), 's').get ==="
                        + " Object.getOwnPropertyDescriptor(Object.getPrototypeOf(new Foo()),"
                        + " 's').get");
        test(
                "true",
                "Object.getOwnPropertyDescriptor(Object.getPrototypeOf(new Foo()), 's').set ==="
                        + " Object.getOwnPropertyDescriptor(Object.getPrototypeOf(new Foo()),"
                        + " 's').set");
    }

    @Test
    public void callLookedUpSetter() throws Exception {
        test("newValue", "var f = new Foo(); f.s = 'newValue'; f.s");
        test("newValue", "var f = new Foo(); f.__lookupSetter__('s').call(f, 'newValue'); f.s");
    }

    @Test
    public void lookedUpSetter_toString() throws Exception {
        test(
                "function s() {\n\t[native code, arity=0]\n}\n",
                "new Foo().__lookupSetter__('s').toString()");
    }

    @Test
    public void lookedUpSetter_equals() throws Exception {
        test("true", "new Foo().__lookupSetter__('s') == new Foo().__lookupSetter__('s')");
    }

    @Test
    public void shadowGetterTest() throws Exception {
        test(
                "true",
                "function f() { var a = { }; var b = Object.create(a);"
                        + "b.foo = 1;"
                        + "a.__defineSetter__('foo', function () {});"
                        + "return b.__lookupSetter__('foo') === undefined;}"
                        + "f();");
    }

    private void test(final String expected, final String src) throws Exception {
        final ContextAction<String> action =
                new ContextAction<String>() {
                    @Override
                    public String run(final Context cx) {
                        try {
                            final Scriptable scope = cx.initStandardObjects(new TopScope());
                            ScriptableObject.defineClass(scope, Foo.class);
                            cx.evaluateString(scope, defineSetterAndGetterX, "initX", 1, null);
                            Object result =
                                    String.valueOf(cx.evaluateString(scope, src, "test", 1, null));
                            assertEquals(expected, result);
                            return null;
                        } catch (final Exception e) {
                            if (e instanceof RuntimeException) {
                                throw (RuntimeException) e;
                            }
                            throw new RuntimeException(e);
                        }
                    }
                };

        Utils.runWithAllOptimizationLevels(action);
    }

    public static class Foo extends ScriptableObject {
        private String s = "hello";

        public Foo() {
            /* Empty. */
        }

        public String jsGet_s() {
            return this.s;
        }

        public void jsSet_s(String string) {
            this.s = string;
        }

        @Override
        public String getClassName() {
            return "Foo";
        }
    }

    public static class TopScope extends ScriptableObject {
        @Override
        public String getClassName() {
            return "TopScope";
        }
    }
}
