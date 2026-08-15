/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.PropertyDescriptor;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.VarScope;

/** Test the PropertyDescriptor class. */
class PropertyDescriptorTest {

    @Test
    void testDataDescriptorFromValues() {
        PropertyDescriptor desc = new PropertyDescriptor(true, false, true, "test value");
        assertTrue(desc.isEnumerable());
        assertFalse(desc.isWritable());
        assertTrue(desc.isConfigurable());
        assertEquals("test value", desc.value);
        assertTrue(desc.hasValue());
        assertTrue(desc.hasWritable());
        assertTrue(desc.hasEnumerable());
        assertTrue(desc.hasConfigurable());
        assertTrue(desc.isDataDescriptor());
        assertFalse(desc.isAccessorDescriptor());
        assertFalse(desc.isGenericDescriptor());
    }

    @Test
    void testAccessorDescriptor() {
        Object getter = new Object();
        Object setter = new Object();
        PropertyDescriptor desc =
                new PropertyDescriptor(
                        true,
                        ScriptableObject.NOT_FOUND,
                        true,
                        getter,
                        setter,
                        ScriptableObject.NOT_FOUND);

        assertTrue(desc.isEnumerable());
        assertFalse(desc.hasWritable());
        assertTrue(desc.hasGetter());
        assertTrue(desc.hasSetter());
        assertTrue(desc.isAccessorDescriptor());
        assertFalse(desc.isDataDescriptor());
    }

    @Test
    void testGenericDescriptor() {
        PropertyDescriptor desc =
                new PropertyDescriptor(
                        true,
                        ScriptableObject.NOT_FOUND,
                        false,
                        ScriptableObject.NOT_FOUND,
                        ScriptableObject.NOT_FOUND,
                        ScriptableObject.NOT_FOUND);

        assertTrue(desc.isGenericDescriptor());
        assertFalse(desc.isDataDescriptor());
        assertFalse(desc.isAccessorDescriptor());
    }

    @Test
    void testFromScriptableObject() {
        try (Context cx = Context.enter()) {
            ScriptableObject desc = new NativeObject();
            desc.defineProperty("value", "test", ScriptableObject.EMPTY);
            desc.defineProperty("writable", true, ScriptableObject.EMPTY);
            desc.defineProperty("enumerable", false, ScriptableObject.EMPTY);
            desc.defineProperty("configurable", true, ScriptableObject.EMPTY);

            PropertyDescriptor pd = new PropertyDescriptor(desc);
            assertEquals("test", pd.value);
            assertTrue(pd.isWritable());
            assertFalse(pd.isEnumerable());
            assertTrue(pd.isConfigurable());
        }
    }

    @Test
    void testFromAttributeFlags() {
        PropertyDescriptor desc =
                new PropertyDescriptor(
                        "value", ScriptableObject.READONLY | ScriptableObject.PERMANENT, true);

        assertEquals("value", desc.value);
        assertFalse(desc.isWritable());
        assertTrue(desc.isConfigurable() == false);
    }

    @Test
    void testToObject() {
        try (Context cx = Context.enter()) {
            VarScope scope = cx.initStandardObjects();
            PropertyDescriptor desc = new PropertyDescriptor(true, false, true, "test");

            Object obj = desc.toObject(scope);
            assertTrue(obj instanceof ScriptableObject);

            ScriptableObject so = (ScriptableObject) obj;
            assertEquals("test", ScriptableObject.getProperty(so, "value"));
            assertEquals(false, ScriptableObject.getProperty(so, "writable"));
            assertEquals(true, ScriptableObject.getProperty(so, "enumerable"));
            assertEquals(true, ScriptableObject.getProperty(so, "configurable"));
        }
    }
}
