/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * Checks that {@code __proto__} and {@code Object.setPrototypeOf} work on objects which implement
 * {@link org.mozilla.javascript.Scriptable} but are not {@link
 * org.mozilla.javascript.ScriptableObject}s, such as wrapped Java objects and arrays.
 */
public class JavaObjectProtoTest {

    private static final String JAVA_OBJECT = "var o = new java.util.ArrayList();\n";
    private static final String JAVA_ARRAY =
            "var o = java.lang.reflect.Array.newInstance(java.lang.String, 2);\n";

    private static void assertES6(String setup, Object expected, String script) {
        Utils.assertWithAllModes_ES6(expected, setup + script);
    }

    private static void assert18(String setup, Object expected, String script) {
        Utils.assertWithAllModes_1_8(expected, setup + script);
    }

    private static final String SET_DOT =
            "var p = { foo: 'bar' };\n"
                    + "o.__proto__ = p;\n"
                    + "Object.getPrototypeOf(o) === p && o.__proto__ === p && o.foo === 'bar'";

    private static final String SET_ELEM =
            "var p = { foo: 'bar' };\n"
                    + "o['__proto__'] = p;\n"
                    + "Object.getPrototypeOf(o) === p && o.__proto__ === p && o.foo === 'bar'";

    private static final String SET_PROTOTYPE_OF =
            "var p = { foo: 'bar' };\n"
                    + "var r = Object.setPrototypeOf(o, p);\n"
                    + "r === o && Object.getPrototypeOf(o) === p && o.foo === 'bar'";

    private static final String SETTER_CALL =
            "var p = { foo: 'bar' };\n"
                    + "var setter = Object.getOwnPropertyDescriptor(Object.prototype,"
                    + " '__proto__').set;\n"
                    + "setter.call(o, p);\n"
                    + "Object.getPrototypeOf(o) === p && o.foo === 'bar'";

    private static final String SET_TWICE =
            "var p1 = { foo: 'one' };\n"
                    + "var p2 = { foo: 'two' };\n"
                    + "o.__proto__ = p1;\n"
                    + "o.__proto__ = p2;\n"
                    + "Object.getPrototypeOf(o) === p2 && Object.getPrototypeOf(p1) =="
                    + " Object.prototype && o.foo === 'two'";

    private static final String SET_NULL =
            "o.__proto__ = { foo: 'bar' };\n"
                    + "o.__proto__ = null;\n"
                    + "Object.getPrototypeOf(o) === null";

    private static final String CYCLE_VIA_PROTO =
            "var p = {};\n"
                    + "o.__proto__ = p;\n"
                    + "try { p.__proto__ = o; 'no error' } catch (e) { e instanceof TypeError }";

    private static final String CYCLE_VIA_SET_PROTOTYPE_OF =
            "var p = {};\n"
                    + "Object.setPrototypeOf(o, p);\n"
                    + "try { Object.setPrototypeOf(p, o); 'no error' }"
                    + " catch (e) { e instanceof TypeError }";

    private static final String JAVA_AS_PROTO =
            "var c = { own: 1 };\n" + "c.__proto__ = o;\n" + "Object.getPrototypeOf(c) === o";

    @Test
    void javaObjectSetProtoDotES6() {
        assertES6(JAVA_OBJECT, true, SET_DOT);
    }

    @Test
    void javaArraySetProtoDotES6() {
        assertES6(JAVA_ARRAY, true, SET_DOT);
    }

    @Test
    void javaObjectSetProtoElemES6() {
        assertES6(JAVA_OBJECT, true, SET_ELEM);
    }

    @Test
    void javaArraySetProtoElemES6() {
        assertES6(JAVA_ARRAY, true, SET_ELEM);
    }

    @Test
    void javaObjectSetPrototypeOf() {
        assertES6(JAVA_OBJECT, true, SET_PROTOTYPE_OF);
    }

    @Test
    void javaArraySetPrototypeOf() {
        assertES6(JAVA_ARRAY, true, SET_PROTOTYPE_OF);
    }

    @Test
    void javaObjectProtoSetterCall() {
        assertES6(JAVA_OBJECT, true, SETTER_CALL);
    }

    @Test
    void javaArrayProtoSetterCall() {
        assertES6(JAVA_ARRAY, true, SETTER_CALL);
    }

    @Test
    void javaObjectSetProtoTwiceES6() {
        assertES6(JAVA_OBJECT, true, SET_TWICE);
    }

    @Test
    void javaArraySetProtoTwiceES6() {
        assertES6(JAVA_ARRAY, true, SET_TWICE);
    }

    @Test
    void javaObjectSetProtoNullES6() {
        assertES6(JAVA_OBJECT, true, SET_NULL);
    }

    @Test
    void javaArraySetProtoNullES6() {
        assertES6(JAVA_ARRAY, true, SET_NULL);
    }

    @Test
    void javaObjectCycleViaProtoES6() {
        assertES6(JAVA_OBJECT, true, CYCLE_VIA_PROTO);
    }

    @Test
    void javaArrayCycleViaProtoES6() {
        assertES6(JAVA_ARRAY, true, CYCLE_VIA_PROTO);
    }

    @Test
    void javaObjectCycleViaSetPrototypeOf() {
        assertES6(JAVA_OBJECT, true, CYCLE_VIA_SET_PROTOTYPE_OF);
    }

    @Test
    void javaArrayCycleViaSetPrototypeOf() {
        assertES6(JAVA_ARRAY, true, CYCLE_VIA_SET_PROTOTYPE_OF);
    }

    @Test
    void javaObjectAsProtoES6() {
        assertES6(JAVA_OBJECT, true, JAVA_AS_PROTO);
    }

    @Test
    void javaArrayAsProtoES6() {
        assertES6(JAVA_ARRAY, true, JAVA_AS_PROTO);
    }

    @Test
    void javaObjectSetProtoDot18() {
        assert18(JAVA_OBJECT, true, SET_DOT.replace("Object.getPrototypeOf(o)", "o.__proto__"));
    }

    @Test
    void javaArraySetProtoDot18() {
        assert18(JAVA_ARRAY, true, SET_DOT.replace("Object.getPrototypeOf(o)", "o.__proto__"));
    }

    @Test
    void javaObjectSetProtoTwice18() {
        assert18(
                JAVA_OBJECT,
                true,
                "var p1 = { foo: 'one' };\n"
                        + "var p2 = { foo: 'two' };\n"
                        + "o.__proto__ = p1;\n"
                        + "o.__proto__ = p2;\n"
                        + "o.__proto__ === p2 && p1.__proto__ === Object.prototype"
                        + " && o.foo === 'two'");
    }

    @Test
    void javaObjectCycle18() {
        assert18(
                JAVA_OBJECT,
                true,
                "var p = {};\n"
                        + "o.__proto__ = p;\n"
                        + "try { p.__proto__ = o; 'no error' } catch (e) { e instanceof Error }");
    }

    @Test
    void javaObjectDefaultProtoReadable() {
        Utils.assertWithAllModes(
                Context.VERSION_ES6,
                null,
                true,
                JAVA_OBJECT + "o.__proto__ === Object.getPrototypeOf(o)");
    }

    @Test
    void javaArrayDefaultProtoReadable() {
        Utils.assertWithAllModes(
                Context.VERSION_ES6,
                null,
                true,
                JAVA_ARRAY + "o.__proto__ === Object.getPrototypeOf(o)");
    }

    @Test
    void javaArraySetProtoNull18() {
        assert18(JAVA_ARRAY, true, SET_NULL.replace("Object.getPrototypeOf(o)", "o.__proto__"));
    }

    @Test
    void javaObjectWithPrototypeSetProtoES6() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    TopLevel scope = cx.initStandardObjects();
                    Scriptable p1 = cx.newObject(scope);
                    Scriptable o = (Scriptable) Context.javaToJS(new ArrayList<>(), scope);
                    o.setPrototype(p1);
                    ScriptableObject.putProperty(scope, "o", o);
                    ScriptableObject.putProperty(scope, "p1", p1);
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var p2 = { foo: 'two' };\n"
                                            + "o.__proto__ = p2;\n"
                                            + "(Object.getPrototypeOf(o) === p2) + ' '"
                                            + " + (Object.getPrototypeOf(p1) === Object.prototype) + ' '"
                                            + " + Object.prototype.hasOwnProperty.call(p1,"
                                            + " '__proto__')",
                                    "test.js",
                                    1,
                                    null);
                    assertEquals("true true false", result);
                    return null;
                });
    }
}
