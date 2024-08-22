/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.keys(obj) method
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;
import static org.mozilla.javascript.tests.Evaluator.eval;

import org.junit.Test;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;

public class ObjectKeysTest {

    @Test
    public void shouldReturnOnlyEnumerablePropertiesOfArg() {
        NativeObject object = new NativeObject();
        object.defineProperty("a", "1", ScriptableObject.EMPTY);
        object.defineProperty("b", "2", ScriptableObject.EMPTY);
        object.defineProperty("c", "3", ScriptableObject.DONTENUM);

        Object result = eval("Object.keys(obj)", "obj", object);

        NativeArray keys = (NativeArray) result;

        assertEquals(2, keys.getLength());
        assertEquals("a", keys.get(0, keys));
        assertEquals("b", keys.get(1, keys));
    }
}
