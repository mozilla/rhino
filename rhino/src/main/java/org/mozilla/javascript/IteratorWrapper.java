/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Wrapper for iterators returned by Iterator.from(). This class wraps any iterator and ensures it
 * inherits from Iterator.prototype while delegating all operations to the wrapped iterator.
 */
public final class IteratorWrapper extends ScriptableObject {
    private static final long serialVersionUID = 1L;

    private Scriptable wrappedIterator;

    public IteratorWrapper(Scriptable wrappedIterator, Scriptable scope) {
        this.wrappedIterator = wrappedIterator;

        // Set parent scope
        setParentScope(scope);

        // Set prototype to inherit from Iterator.prototype if available
        Scriptable iteratorPrototype = NativeIteratorConstructor.getIteratorPrototype(scope);
        if (iteratorPrototype != null) {
            setPrototype(iteratorPrototype);
        }
    }

    @Override
    public String getClassName() {
        return "Iterator";
    }

    @Override
    public Object get(String name, Scriptable start) {
        // Special handling for 'next' method - create a bound version
        if ("next".equals(name) && wrappedIterator != null) {
            Object nextMethod = ScriptableObject.getProperty(wrappedIterator, "next");
            if (nextMethod instanceof Callable) {
                // Return a function that calls next() with the wrapped iterator as 'this'
                return new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        return ((Callable) nextMethod).call(cx, scope, wrappedIterator, args);
                    }

                    @Override
                    public String getFunctionName() {
                        return "next";
                    }
                };
            }
        }

        // Special handling for 'return' method
        if ("return".equals(name) && wrappedIterator != null) {
            Object returnMethod = ScriptableObject.getProperty(wrappedIterator, "return");
            if (returnMethod instanceof Callable) {
                return new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        return ((Callable) returnMethod).call(cx, scope, wrappedIterator, args);
                    }

                    @Override
                    public String getFunctionName() {
                        return "return";
                    }
                };
            }
        }

        // Delegate other properties to wrapped iterator
        if (wrappedIterator != null) {
            Object value = ScriptableObject.getProperty(wrappedIterator, name);
            if (value != Scriptable.NOT_FOUND) {
                return value;
            }
        }
        // Fall back to our prototype chain
        return super.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (wrappedIterator != null) {
            Object value = wrappedIterator.get(index, wrappedIterator);
            if (value != Scriptable.NOT_FOUND) {
                return value;
            }
        }
        return super.get(index, start);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (wrappedIterator != null) {
            // Check if property exists in wrapped iterator or its prototype chain
            Object value = ScriptableObject.getProperty(wrappedIterator, name);
            if (value != Scriptable.NOT_FOUND) {
                return true;
            }
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        if (wrappedIterator != null && wrappedIterator.has(index, wrappedIterator)) {
            return true;
        }
        return super.has(index, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (wrappedIterator != null) {
            wrappedIterator.put(name, wrappedIterator, value);
        } else {
            super.put(name, start, value);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (wrappedIterator != null) {
            wrappedIterator.put(index, wrappedIterator, value);
        } else {
            super.put(index, start, value);
        }
    }

    @Override
    public void delete(String name) {
        if (wrappedIterator != null) {
            wrappedIterator.delete(name);
        } else {
            super.delete(name);
        }
    }

    @Override
    public void delete(int index) {
        if (wrappedIterator != null) {
            wrappedIterator.delete(index);
        } else {
            super.delete(index);
        }
    }

    @Override
    public Object[] getIds() {
        if (wrappedIterator != null) {
            return wrappedIterator.getIds();
        }
        return super.getIds();
    }
}
