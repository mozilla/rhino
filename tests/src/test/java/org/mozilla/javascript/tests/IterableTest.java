/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
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
 * <p>See https://github.com/mozilla/rhino/pull/599
 *
 * @author Stijn Kliemesch
 */
public class IterableTest {

    public static final class FooWithoutSymbols extends FooBoilerplate {

        public FooWithoutSymbols(final Scriptable scope) {
            super(scope);
        }
    }

    public static final class FooWithSymbols extends SymbolFooBoilerplate {

        public FooWithSymbols(final Scriptable scope) {
            super(scope);
        }

        @Override
        public boolean has(Symbol key, Scriptable start) {
            return false;
        }
    }

    public static final class FooWithArrayIterator extends SymbolFooBoilerplate {

        public FooWithArrayIterator(final Scriptable scope) {
            super(scope);
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
        public Object get(Symbol key, Scriptable start) {
            if (SymbolKey.ITERATOR.equals(key)) {
                return ScriptableObject.getProperty(
                        ScriptableObject.getArrayPrototype(scope), SymbolKey.ITERATOR);
            }
            throw new IllegalStateException();
        }

        @Override
        public boolean has(Symbol key, Scriptable start) {
            return SymbolKey.ITERATOR.equals(key);
        }
    }

    /**
     * Regression test for a Scriptable not implementing SymbolScriptable, used in for a for-of
     * loop.
     *
     * <p>Note: no spec is (knowingly) being adhered to with the "expected" in this test, merely the
     * situation as-is being "noted".
     */
    @Test
    public void forOfUsingNonSymbolScriptable() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Scriptable foo = new FooWithoutSymbols(scope);
                    ScriptableObject.putProperty(scope, "foo", foo);

                    try {
                        cx.evaluateString(
                                scope,
                                "(function(){for(x of foo) { return x; }})();",
                                "<eval>",
                                0,
                                null);

                    } catch (Throwable t) {
                        assertEquals(t.getClass(), EcmaError.class);
                        assertEquals(t.getMessage(), "TypeError: [object Object] is not iterable");
                    }

                    return null;
                });
    }

    /**
     * Regression test for a Scriptable implementing SymbolScriptable that doesn't implement the
     * iterable protocol, used in for a for-of loop.
     *
     * <p>Note: no spec is (knowingly) being adhered to with the "expected" in this test, merely the
     * situation as-is being "noted".
     */
    @Test
    public void forOfUsingNonIterable() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Scriptable foo = new FooWithSymbols(scope);
                    ScriptableObject.putProperty(scope, "foo", foo);

                    try {
                        cx.evaluateString(
                                scope,
                                "(function(){for(x of foo) { return x; }})();",
                                "<eval>",
                                0,
                                null);
                    } catch (Throwable t) {
                        assertEquals(t.getClass(), EcmaError.class);
                        assertEquals(t.getMessage(), "TypeError: [object Object] is not iterable");
                    }

                    return null;
                });
    }

    /**
     * Test for a host object to be able to supply an iterator, specifically
     * Array.prototype[Symbol.iterator], for a for-of loop.
     */
    @Test
    public void forOfUsingArrayIterator() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Scriptable foo = new FooWithArrayIterator(scope);
                    ScriptableObject.putProperty(scope, "foo", foo);

                    assertEquals(
                            true,
                            cx.evaluateString(
                                    scope,
                                    "foo[Symbol.iterator] === Array.prototype[Symbol.iterator]",
                                    "<eval>",
                                    0,
                                    null));

                    assertEquals(
                            123,
                            cx.evaluateString(
                                    scope,
                                    "(function(){for(x of foo) { return x; }})();",
                                    "<eval>",
                                    0,
                                    null));

                    return null;
                });
    }

    // Explicitly not a ScriptableObject
    public static class FooBoilerplate implements Scriptable {

        protected final Scriptable scope;

        public FooBoilerplate(final Scriptable scope) {
            this.scope = scope;
        }

        @Override
        public String getClassName() {
            return this.getClass().getSimpleName();
        }

        @Override
        public Scriptable getParentScope() {
            return scope;
        }

        @Override
        public Object getDefaultValue(Class<?> hint) {
            if (String.class == hint) {
                return "[object Object]";
            }
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Scriptable getPrototype() {
            return TopLevel.getBuiltinPrototype(scope, TopLevel.Builtins.Object);
        }

        @Override
        public Object get(String name, Scriptable start) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object get(int index, Scriptable start) {
            throw new UnsupportedOperationException("Not supported yet.");
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
        public void setPrototype(Scriptable prototype) {
            throw new UnsupportedOperationException("Not supported yet.");
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
        public boolean hasInstance(Scriptable instance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public static class SymbolFooBoilerplate extends FooBoilerplate implements SymbolScriptable {

        public SymbolFooBoilerplate(final Scriptable scope) {
            super(scope);
        }

        @Override
        public Object get(Symbol key, Scriptable start) {
            throw new UnsupportedOperationException(
                    "Not supported yet."); // To change body of generated methods, choose Tools |
            // Templates.
        }

        @Override
        public boolean has(Symbol key, Scriptable start) {
            throw new UnsupportedOperationException(
                    "Not supported yet."); // To change body of generated methods, choose Tools |
            // Templates.
        }

        @Override
        public void put(Symbol key, Scriptable start, Object value) {
            throw new UnsupportedOperationException(
                    "Not supported yet."); // To change body of generated methods, choose Tools |
            // Templates.
        }

        @Override
        public void delete(Symbol key) {
            throw new UnsupportedOperationException(
                    "Not supported yet."); // To change body of generated methods, choose Tools |
            // Templates.
        }
    }
}
