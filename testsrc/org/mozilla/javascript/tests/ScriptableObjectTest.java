/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.HashSlotMap;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SlotMap;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class ScriptableObjectTest {
    private ScriptableObject obj;

    @Before
    public void init() {
        obj = new ScriptableObject() {
            @Override
            public String getClassName() {
                return "TestClass";
            }
        };
    }

    @Test
    public void testCRUDName() {
        assertTrue(obj.isEmpty());
        assertEquals(0, obj.size());
        assertFalse(obj.has("foo", obj));
        assertEquals(ScriptableObject.NOT_FOUND, obj.get("foo", obj));
        assertEquals(-1, obj.getMapping("foo"));

        obj.put("foo", obj, 1);
        assertTrue(obj.has("foo", obj));
        assertEquals(1, obj.size());
        assertFalse(obj.isEmpty());
        assertEquals(1, obj.get("foo", obj));
        int mapping = obj.getMapping("foo");
        assertTrue(mapping >= 0);
        assertEquals(1, obj.getMappedSlot(mapping, obj));

        obj.put("foo", obj, 2);
        assertEquals(2, obj.get("foo", obj));
        assertEquals(2, obj.getMappedSlot(mapping, obj));

        obj.delete("foo");
        assertFalse(obj.has("foo", obj));
        assertTrue(obj.isEmpty());
        assertEquals(0, obj.size());
        assertEquals(ScriptableObject.NOT_FOUND, obj.get("foo", obj));
        assertEquals(ScriptableObject.NOT_FOUND, obj.getMappedSlot(mapping, obj));

        obj.put("foo", obj, 3);
        assertTrue(obj.has("foo", obj));
        assertEquals(1, obj.size());
        assertFalse(obj.isEmpty());
        assertEquals(3, obj.get("foo", obj));
        assertEquals(mapping, obj.getMapping("foo"));
        assertEquals(3, obj.getMappedSlot(mapping, obj));
    }
}
