/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;
import static org.mozilla.javascript.tests.Evaluator.eval;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

public class ObjectGetOwnPropertyDescriptorTest {

    @Test
    public void contentsOfPropertyDescriptorShouldReflectAttributesOfProperty() {
        NativeObject descriptor;
        NativeObject object = new NativeObject();
        object.defineProperty("a", "1", ScriptableObject.EMPTY);
        object.defineProperty(
                "b",
                "2",
                ScriptableObject.DONTENUM | ScriptableObject.READONLY | ScriptableObject.PERMANENT);

        descriptor =
                (NativeObject) eval("Object.getOwnPropertyDescriptor(obj, 'a')", "obj", object);
        assertEquals("1", descriptor.get("value"));
        assertEquals(true, descriptor.get("enumerable"));
        assertEquals(true, descriptor.get("writable"));
        assertEquals(true, descriptor.get("configurable"));

        descriptor =
                (NativeObject) eval("Object.getOwnPropertyDescriptor(obj, 'b')", "obj", object);
        assertEquals("2", descriptor.get("value"));
        assertEquals(false, descriptor.get("enumerable"));
        assertEquals(false, descriptor.get("writable"));
        assertEquals(false, descriptor.get("configurable"));
    }

    @Test
    public void callPropertyDescriptorSetterWithConsString() throws Exception {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();
            ScriptableObject.defineClass(scope, AnnotatedHostObject.class);

            final String script =
                    "var hostObj = new AnnotatedHostObject();\n"
                            + "var valueProperty = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(hostObj), 'myProp');\n"
                            + "var consString = 'a';\n"
                            + "consString = consString + 'bc';\n"
                            + "valueProperty.set.call(hostObj, consString);\n"
                            + "var result = '' + valueProperty.get.call(hostObj);\n"
                            + "result;\n";
            String result = (String) cx.evaluateString(scope, script, "<testsrc>", 0, null);
            assertEquals("abc", result);
        }
    }

    public static class AnnotatedHostObject extends ScriptableObject {

        String myProp;

        public AnnotatedHostObject() {}

        @Override
        public String getClassName() {
            return "AnnotatedHostObject";
        }

        @JSConstructor
        public void jsConstructorMethod() {}

        @JSGetter
        public String getMyProp() {
            return myProp;
        }

        @JSSetter
        public void setMyProp(String prop) {
            myProp = prop;
        }
    }
}
