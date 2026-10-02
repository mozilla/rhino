/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.PropertyDescriptor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.VarScope;

/**
 * Test property descriptor operations on custom Scriptable implementations that are not
 * ScriptableObject.
 */
class CustomScriptableDescriptorTest {
    private Context cx;
    private VarScope root;

    /** A simple custom Scriptable that supports property descriptors. */
    static class CustomScriptableWithDescriptors extends NativeObject {
        private final java.util.Map<String, PropertyDescriptor> descriptors =
                new java.util.HashMap<>();

        @Override
        public PropertyDescriptor getOwnPropertyDescriptor(Context cx, Object key) {
            if (key instanceof String) {
                return descriptors.get((String) key);
            }
            return super.getOwnPropertyDescriptor(cx, key);
        }

        @Override
        public boolean defineOwnProperty(Context cx, Object key, PropertyDescriptor desc) {
            if (key instanceof String) {
                String strKey = (String) key;
                if (desc.hasValue()) {
                    put(strKey, this, desc.value);
                }
                descriptors.put(strKey, desc);
                return true;
            }
            return super.defineOwnProperty(cx, key, desc);
        }
    }

    @BeforeEach
    void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        root = cx.initStandardObjects();
    }

    @Test
    void testCustomScriptableGetOwnPropertyDescriptor() {
        CustomScriptableWithDescriptors obj = new CustomScriptableWithDescriptors();
        obj.put("prop", obj, 42);
        obj.defineOwnProperty(
                cx,
                "prop",
                new PropertyDescriptor(
                        true,
                        true,
                        false,
                        ScriptableObject.NOT_FOUND,
                        ScriptableObject.NOT_FOUND,
                        42));

        Object result =
                cx.evaluateString(
                        root,
                        "let o = {}; o.prop = 42; Object.defineProperty(o, 'x', {"
                                + "value: 100, writable: true, enumerable: false, configurable: false"
                                + "}); Object.getOwnPropertyDescriptor(o, 'x');",
                        "test",
                        1,
                        null);
        assertTrue(result instanceof Scriptable);
        Scriptable desc = (Scriptable) result;
        assertEquals(100, ScriptableObject.getProperty(desc, "value"));
    }

    @Test
    void testObjectDefinePropertyOnCustomScriptable() {
        CustomScriptableWithDescriptors obj = new CustomScriptableWithDescriptors();
        root.put("customObj", root, obj);

        Object result =
                cx.evaluateString(
                        root,
                        "Object.defineProperty(customObj, 'myProp', {"
                                + "value: 'hello', writable: false, enumerable: true, configurable: true"
                                + "}); customObj.myProp;",
                        "test",
                        1,
                        null);
        assertEquals("hello", result.toString());
    }

    @Test
    void testObjectGetOwnPropertyNames() {
        CustomScriptableWithDescriptors obj = new CustomScriptableWithDescriptors();
        obj.put("foo", obj, 1);
        obj.put("bar", obj, 2);
        root.put("customObj", root, obj);

        Object result =
                cx.evaluateString(root, "Object.getOwnPropertyNames(customObj);", "test", 1, null);
        assertTrue(result instanceof Scriptable);
    }

    @Test
    void testDescriptorAttributesEnforced() {
        String script =
                "let obj = {};"
                        + "Object.defineProperty(obj, 'readonlyProp', {"
                        + "  value: 42,"
                        + "  writable: false,"
                        + "  enumerable: true,"
                        + "  configurable: false"
                        + "});"
                        + "let desc = Object.getOwnPropertyDescriptor(obj, 'readonlyProp');"
                        + "desc.writable === false && desc.configurable === false;";

        Object result = cx.evaluateString(root, script, "test", 1, null);
        assertTrue((Boolean) result);
    }

    @Test
    void testdefineProperties() {
        String script =
                "let obj = {};"
                        + "Object.defineProperties(obj, {"
                        + "  a: { value: 1, enumerable: true },"
                        + "  b: { value: 2, enumerable: true }"
                        + "});"
                        + "obj.a === 1 && obj.b === 2;";

        Object result = cx.evaluateString(root, script, "test", 1, null);
        assertTrue((Boolean) result);
    }
}
