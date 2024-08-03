/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Objects;

public final class ES6Generator extends IdScriptableObject {
    private static final long serialVersionUID = 1645892441041347273L;

    private static final Object GENERATOR_TAG = "Generator";

    static ES6Generator init(ScriptableObject scope, boolean sealed) {

        ES6Generator prototype = new ES6Generator();
        if (scope != null) {
            prototype.setParentScope(scope);
            prototype.setPrototype(getObjectPrototype(scope));
        }
        prototype.activatePrototypeMap(MAX_PROTOTYPE_ID);
        if (sealed) {
            prototype.sealObject();
        }

        // Need to access Generator prototype when constructing
        // Generator instances, but don't have a generator constructor
        // to use to find the prototype. Use the "associateValue"
        // approach instead.
        if (scope != null) {
            scope.associateValue(GENERATOR_TAG, prototype);
        }

        return prototype;
    }

    /** Only for constructing the prototype object. */
    private ES6Generator() {}

    public ES6Generator(Scriptable scope, NativeFunction function, Object savedState) {
        this.function = function;
        this.savedState = savedState;
        // Set parent and prototype properties. Since we don't have a
        // "Generator" constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        ES6Generator prototype =
                (ES6Generator) ScriptableObject.getTopScopeValue(top, GENERATOR_TAG);
        this.setPrototype(prototype);
    }

    @Override
    public String getClassName() {
        return "Generator";
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_iterator) {
            initPrototypeMethod(GENERATOR_TAG, id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
            return;
        }

        String s;
        int arity;
        switch (id) {
            case Id_next:
                arity = 1;
                s = "next";
                break;
            case Id_return:
                arity = 1;
                s = "return";
                break;
            case Id_throw:
                arity = 1;
                s = "throw";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(GENERATOR_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(GENERATOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        ES6Generator generator = ensureType(thisObj, ES6Generator.class, f);
        Object value = args.length >= 1 ? args[0] : Undefined.instance;

        switch (id) {
            case Id_return:
                if (generator.delegee == null) {
                    return generator.resumeAbruptLocal(
                            cx, scope, NativeGenerator.GENERATOR_CLOSE, value);
                }
                return generator.resumeDelegeeReturn(cx, scope, value);
            case Id_next:
                if (generator.delegee == null) {
                    return generator.resumeLocal(cx, scope, value);
                }
                return generator.resumeDelegee(cx, scope, value);
            case Id_throw:
                if (generator.delegee == null) {
                    return generator.resumeAbruptLocal(
                            cx, scope, NativeGenerator.GENERATOR_THROW, value);
                }
                return generator.resumeDelegeeThrow(cx, scope, value);
            case SymbolId_iterator:
                return thisObj;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    private Scriptable resumeDelegee(Context cx, Scriptable scope, Object value) {
        try {
            // Be super-careful and only pass an arg to next if it expects one
            Object[] nextArgs =
                    Undefined.instance.equals(value)
                            ? ScriptRuntime.emptyArgs
                            : new Object[] {value};

            Callable nextFn =
                    ScriptRuntime.getPropFunctionAndThis(
                            delegee, ES6Iterator.NEXT_METHOD, cx, scope);
            Scriptable nextThis = ScriptRuntime.lastStoredScriptable(cx);
            Object nr = nextFn.call(cx, scope, nextThis, nextArgs);

            Scriptable nextResult = ScriptableObject.ensureScriptable(nr);
            if (ScriptRuntime.isIteratorDone(cx, nextResult)) {
                // Iterator is "done".
                delegee = null;
                // Return a result to the original generator
                return resumeLocal(
                        cx,
                        scope,
                        ScriptableObject.getProperty(nextResult, ES6Iterator.VALUE_PROPERTY));
            }
            // Otherwise, we have a normal result and should continue
            return nextResult;

        } catch (RhinoException re) {
            // Exceptions from the delegee should be handled by the enclosing
            // generator, including if they're because functions can't be found.
            delegee = null;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
    }

    private Scriptable resumeDelegeeThrow(Context cx, Scriptable scope, Object value) {
        boolean returnCalled = false;
        try {
            // Delegate to "throw" method. If it's not defined we'll get an error here.
            Callable throwFn = ScriptRuntime.getPropFunctionAndThis(delegee, "throw", cx, scope);
            Scriptable nextThis = ScriptRuntime.lastStoredScriptable(cx);
            Object throwResult = throwFn.call(cx, scope, nextThis, new Object[] {value});

            if (ScriptRuntime.isIteratorDone(cx, throwResult)) {
                // Iterator is "done".
                try {
                    // Return a result to the original generator, but first optionally call "return"
                    returnCalled = true;
                    callReturnOptionally(cx, scope, Undefined.instance);
                } finally {
                    delegee = null;
                }
                return resumeLocal(
                        cx,
                        scope,
                        ScriptRuntime.getObjectProp(
                                throwResult, ES6Iterator.VALUE_PROPERTY, cx, scope));
            }
            // Otherwise, we have a normal result and should continue
            return ensureScriptable(throwResult);

        } catch (RhinoException re) {
            // Handle all exceptions, including missing methods, by delegating to original.
            try {
                if (!returnCalled) {
                    try {
                        callReturnOptionally(cx, scope, Undefined.instance);
                    } catch (RhinoException re2) {
                        return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re2);
                    }
                }
            } finally {
                delegee = null;
            }
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
    }

    private Scriptable resumeDelegeeReturn(Context cx, Scriptable scope, Object value) {
        try {
            // Call "return" but don't throw if it can't be found
            Object retResult = callReturnOptionally(cx, scope, value);
            if (retResult != null) {
                if (ScriptRuntime.isIteratorDone(cx, retResult)) {
                    // Iterator is "done".
                    delegee = null;
                    // Return a result to the original generator
                    return resumeAbruptLocal(
                            cx,
                            scope,
                            NativeGenerator.GENERATOR_CLOSE,
                            ScriptRuntime.getObjectPropNoWarn(
                                    retResult, ES6Iterator.VALUE_PROPERTY, cx, scope));
                } else {
                    // Not actually done yet!
                    return ensureScriptable(retResult);
                }
            }

            // No "return" -- let the original iterator return the value.
            delegee = null;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_CLOSE, value);

        } catch (RhinoException re) {
            // Exceptions from the delegee should be handled by the enclosing
            // generator, including if they're because functions can't be found.
            delegee = null;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
    }

    private Scriptable resumeLocal(Context cx, Scriptable scope, Object value) {
        if (state == State.COMPLETED) {
            return ES6Iterator.makeIteratorResult(cx, scope, Boolean.TRUE);
        }
        if (state == State.EXECUTING) {
            throw ScriptRuntime.typeErrorById("msg.generator.executing");
        }

        Scriptable result = ES6Iterator.makeIteratorResult(cx, scope, Boolean.FALSE);
        state = State.EXECUTING;

        try {
            Object r =
                    function.resumeGenerator(
                            cx, scope, NativeGenerator.GENERATOR_SEND, savedState, value);

            if (r instanceof YieldStarResult) {
                // This special result tells us that we are executing a "yield *"
                state = State.SUSPENDED_YIELD;
                YieldStarResult ysResult = (YieldStarResult) r;
                try {
                    delegee = ScriptRuntime.callIterator(ysResult.getResult(), cx, scope);
                } catch (RhinoException re) {
                    // Need to handle exceptions if the iterator cannot be called.
                    return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
                }

                Scriptable delResult;
                try {
                    // Re-execute but update state in case we end up back here
                    // Value shall be Undefined based on the very complex spec!
                    delResult = resumeDelegee(cx, scope, Undefined.instance);
                } finally {
                    state = State.EXECUTING;
                }
                if (ScriptRuntime.isIteratorDone(cx, delResult)) {
                    state = State.COMPLETED;
                }
                return delResult;
            }

            ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, r);

        } catch (NativeGenerator.GeneratorClosedException gce) {
            state = State.COMPLETED;
        } catch (JavaScriptException jse) {
            state = State.COMPLETED;
            if (jse.getValue() instanceof NativeIterator.StopIteration) {
                ScriptableObject.putProperty(
                        result,
                        ES6Iterator.VALUE_PROPERTY,
                        ((NativeIterator.StopIteration) jse.getValue()).getValue());
            } else {
                lineNumber = jse.lineNumber();
                lineSource = jse.lineSource();
                if (jse.getValue() instanceof RhinoException) {
                    throw (RhinoException) jse.getValue();
                }
                throw jse;
            }
        } catch (RhinoException re) {
            lineNumber = re.lineNumber();
            lineSource = re.lineSource();
            throw re;
        } finally {
            if (state == State.COMPLETED) {
                ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, Boolean.TRUE);
            } else {
                state = State.SUSPENDED_YIELD;
            }
        }
        return result;
    }

    private Scriptable resumeAbruptLocal(Context cx, Scriptable scope, int op, Object value) {
        if (state == State.EXECUTING) {
            throw ScriptRuntime.typeErrorById("msg.generator.executing");
        }
        if (state == State.SUSPENDED_START) {
            // Throw right away if we never started
            state = State.COMPLETED;
        }

        Scriptable result = ES6Iterator.makeIteratorResult(cx, scope, Boolean.FALSE);
        if (state == State.COMPLETED) {
            if (op == NativeGenerator.GENERATOR_THROW) {
                throw new JavaScriptException(value, lineSource, lineNumber);
            }
            ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, Boolean.TRUE);
            return result;
        }

        state = State.EXECUTING;

        Object throwValue = value;
        if (op == NativeGenerator.GENERATOR_CLOSE) {
            if (!(value instanceof NativeGenerator.GeneratorClosedException)) {
                throwValue = new NativeGenerator.GeneratorClosedException();
            }
        } else {
            if (value instanceof JavaScriptException) {
                throwValue = ((JavaScriptException) value).getValue();
            } else if (value instanceof RhinoException) {
                throwValue = ScriptRuntime.wrapException((Throwable) value, scope, cx);
            }
        }

        try {
            Object r = function.resumeGenerator(cx, scope, op, savedState, throwValue);
            ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, r);
            // If we get here without an exception we can still run.
            state = State.SUSPENDED_YIELD;

        } catch (NativeGenerator.GeneratorClosedException gce) {
            state = State.COMPLETED;
        } catch (JavaScriptException jse) {
            state = State.COMPLETED;
            if (jse.getValue() instanceof NativeIterator.StopIteration) {
                ScriptableObject.putProperty(
                        result,
                        ES6Iterator.VALUE_PROPERTY,
                        ((NativeIterator.StopIteration) jse.getValue()).getValue());
            } else {
                lineNumber = jse.lineNumber();
                lineSource = jse.lineSource();
                if (jse.getValue() instanceof RhinoException) {
                    throw (RhinoException) jse.getValue();
                }
                throw jse;
            }
        } catch (RhinoException re) {
            state = State.COMPLETED;
            lineNumber = re.lineNumber();
            lineSource = re.lineSource();
            throw re;
        } finally {
            // After an abrupt completion we are always, umm, complete,
            // and we will never delegate to the delegee again
            if (state == State.COMPLETED) {
                delegee = null;
                ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, Boolean.TRUE);
            }
        }
        return result;
    }

    private Object callReturnOptionally(Context cx, Scriptable scope, Object value) {
        Object[] retArgs =
                Undefined.instance.equals(value) ? ScriptRuntime.emptyArgs : new Object[] {value};
        // Delegate to "return" method. If it's not defined we ignore it
        Object retFnObj =
                ScriptRuntime.getObjectPropNoWarn(delegee, ES6Iterator.RETURN_METHOD, cx, scope);
        if (!Undefined.instance.equals(retFnObj)) {
            if (!(retFnObj instanceof Callable)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.isnt.function",
                        ES6Iterator.RETURN_METHOD,
                        ScriptRuntime.typeof(retFnObj));
            }
            return ((Callable) retFnObj).call(cx, scope, ensureScriptable(delegee), retArgs);
        }
        return null;
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.ITERATOR.equals(k)) {
            return SymbolId_iterator;
        }
        return 0;
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        L0:
        {
            id = 0;
            String X = null;
            int s_length = s.length();
            if (s_length == 4) {
                X = "next";
                id = Id_next;
            } else if (s_length == 5) {
                X = "throw";
                id = Id_throw;
            } else if (s_length == 6) {
                X = "return";
                id = Id_return;
            }
            if (!Objects.equals(X, s)) id = 0;
            break L0;
        }
        return id;
    }

    private static final int Id_next = 1,
            Id_return = 2,
            Id_throw = 3,
            SymbolId_iterator = 4,
            MAX_PROTOTYPE_ID = SymbolId_iterator;

    private NativeFunction function;
    private Object savedState;
    private String lineSource;
    private int lineNumber;
    private State state = State.SUSPENDED_START;
    private Object delegee;

    enum State {
        SUSPENDED_START,
        SUSPENDED_YIELD,
        EXECUTING,
        COMPLETED
    }

    public static final class YieldStarResult {
        private Object result;

        public YieldStarResult(Object result) {
            this.result = result;
        }

        Object getResult() {
            return result;
        }
    }
}
