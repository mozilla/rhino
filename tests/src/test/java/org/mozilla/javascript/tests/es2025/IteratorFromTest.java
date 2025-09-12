/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeIteratorConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;

/**
 * Tests for ES2025 Iterator infrastructure.
 * Note: Since Iterator is not globally defined (to avoid conflict with legacy Iterator),
 * these tests verify the underlying infrastructure is properly set up.
 */
public class IteratorFromTest {

    @Test
    public void testIteratorPrototypeExists() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject scope = cx.initStandardObjects();
            
            // Iterator.prototype should be created and stored
            Scriptable iteratorProto = NativeIteratorConstructor.getIteratorPrototype(scope);
            assertNotNull("Iterator.prototype should exist", iteratorProto);
            
            // Check that Iterator.prototype has Symbol.iterator
            Object iteratorMethod = ScriptableObject.getProperty(iteratorProto, SymbolKey.ITERATOR);
            assertTrue("Iterator.prototype should have Symbol.iterator method", 
                iteratorMethod != Scriptable.NOT_FOUND);
        }
    }

    @Test
    public void testIteratorPrototypeHasToStringTag() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            ScriptableObject scope = cx.initStandardObjects();
            
            Scriptable iteratorProto = NativeIteratorConstructor.getIteratorPrototype(scope);
            assertNotNull("Iterator.prototype should exist", iteratorProto);
            
            // Check Symbol.toStringTag
            Object toStringTag = ScriptableObject.getProperty(iteratorProto, SymbolKey.TO_STRING_TAG);
            assertTrue("Iterator.prototype should have Symbol.toStringTag", 
                toStringTag != Scriptable.NOT_FOUND);
        }
    }
}