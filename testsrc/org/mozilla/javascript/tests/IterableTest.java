/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import static org.mozilla.javascript.Context.VERSION_ES6;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.SymbolScriptable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;

/**
 * Tests for host objects implementing the iterable protocol.
 *
 * See https://github.com/mozilla/rhino/pull/599
 *
 * @author Stijn Kliemesch
 */
public class IterableTest extends TestCase {

    //Explicitly not a ScriptableObject
    public static final class FooWithArrayIterator implements Scriptable, SymbolScriptable {

        private final Scriptable scope;

        public FooWithArrayIterator(final Scriptable scope) {
            this.scope = scope;
        }

        @Override
        public String getClassName() {
            return this.getClass().getSimpleName();
        }

        @Override
        public Object get(String name, Scriptable start) {
            switch (name) {
                case "length":
                    return 1;
            }
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object get(int index, Scriptable start) {
            switch (index) {
                case 0:
                    return 123;
                default:
                    return Undefined.instance;
            }
        }

        @Override
        public boolean has(String name, Scriptable start) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean has(int index, Scriptable start) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void put(String name, Scriptable start, Object value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void put(int index, Scriptable start, Object value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void delete(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void delete(int index) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Scriptable getPrototype() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPrototype(Scriptable prototype) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Scriptable getParentScope() {
            return scope;
        }

        @Override
        public void setParentScope(Scriptable parent) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object[] getIds() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object getDefaultValue(Class<?> hint) {
            if(String.class == hint) {
                return "[object Object]";
            }
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasInstance(Scriptable instance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object get(Symbol key, Scriptable start) {
            if (SymbolKey.ITERATOR.equals(key)) {
                return ScriptableObject.getProperty(
                        TopLevel.getArrayPrototype(scope),
                        SymbolKey.ITERATOR
                );
            }
            throw new IllegalStateException();
        }

        @Override
        public boolean has(Symbol key, Scriptable start) {
            return SymbolKey.ITERATOR.equals(key);
        }

        @Override
        public void put(Symbol key, Scriptable start, Object value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void delete(Symbol key) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    /**
     * Test for a host object to be able to supply an iterator, specifically
     * Array.prototype[Symbol.iterator], for a for-of loop.
     */
    public void testForOfUsingArrayIterator() {

        Context cx = Context.enter();
        cx.setLanguageVersion(VERSION_ES6);

        final Scriptable top = cx.initSafeStandardObjects();

        Scriptable foo = new FooWithArrayIterator(top);

        ScriptableObject.putProperty(top, "foo", foo);

        assertEquals(
                true,
                cx.evaluateString(top, "foo[Symbol.iterator] === Array.prototype[Symbol.iterator]", "<eval>", 0, null)
        );

        assertEquals(
                123,
                cx.evaluateString(top, "(function(){for(x of foo) { return x; }})()", "<eval>", 0, null)
        );
    }

}
