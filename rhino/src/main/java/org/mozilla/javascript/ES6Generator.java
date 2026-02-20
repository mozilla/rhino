/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public final class ES6Generator extends ScriptableObject {
    private static final long serialVersionUID = 1645892441041347273L;

    static final Object GENERATOR_TAG = "Generator";

    private JSFunction function;
    private Object savedState;
    private String lineSource;
    private int lineNumber;
    private State state = State.SUSPENDED_START;
    private Object delegee;

    static ES6Generator init(ScriptableObject scope, boolean sealed) {

        ES6Generator prototype = new ES6Generator();
        if (scope != null) {
            prototype.setParentScope(scope);
            prototype.setPrototype(getObjectPrototype(scope));
        }

        // Define prototype methods using LambdaFunction
        LambdaFunction next = new LambdaFunction(scope, "next", 1, ES6Generator::js_next);
        ScriptableObject.defineProperty(prototype, "next", next, DONTENUM);

        LambdaFunction returnFunc = new LambdaFunction(scope, "return", 1, ES6Generator::js_return);
        ScriptableObject.defineProperty(prototype, "return", returnFunc, DONTENUM);

        LambdaFunction throwFunc = new LambdaFunction(scope, "throw", 1, ES6Generator::js_throw);
        ScriptableObject.defineProperty(prototype, "throw", throwFunc, DONTENUM);

        LambdaFunction iterator =
                new LambdaFunction(scope, "[Symbol.iterator]", 0, ES6Generator::js_iterator);
        prototype.defineProperty(SymbolKey.ITERATOR, iterator, DONTENUM);

        prototype.defineProperty(SymbolKey.TO_STRING_TAG, "Generator", DONTENUM | READONLY);

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

    public ES6Generator(Scriptable scope, JSFunction function, Object savedState) {
        this.function = function;
        this.savedState = savedState;
        // Set parent and prototype properties.
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        // Per ES6 spec, generator instance's [[Prototype]] should be
        // the generator function's .prototype property.
        Object functionPrototype = ScriptableObject.getProperty(function, "prototype");
        if (functionPrototype instanceof Scriptable) {
            this.setPrototype((Scriptable) functionPrototype);
        } else {
            // If function.prototype is not an Object, use the intrinsic default prototype
            // Ref: Ecma 2026, 10.1.14 GetPrototypeFromConstructor step 4.
            // See test262: language/statements/generators/default-proto.js
            ES6Generator prototype =
                    (ES6Generator) ScriptableObject.getTopScopeValue(top, GENERATOR_TAG);
            this.setPrototype(prototype);
        }
    }

    @Override
    public String getClassName() {
        return "Generator";
    }

    private static ES6Generator realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, ES6Generator.class);
    }

    private static Object js_return(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ES6Generator generator = realThis(thisObj);
        Object value = args.length >= 1 ? args[0] : Undefined.instance;
        if (generator.delegee == null) {
            return generator.resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_CLOSE, value);
        }
        return generator.resumeDelegeeReturn(cx, scope, value);
    }

    private static Object js_next(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ES6Generator generator = realThis(thisObj);
        Object value = args.length >= 1 ? args[0] : Undefined.instance;
        if (generator.delegee == null) {
            return generator.resumeLocal(cx, scope, value);
        }
        return generator.resumeDelegee(cx, scope, value);
    }

    private static Object js_throw(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        ES6Generator generator = realThis(thisObj);
        Object value = args.length >= 1 ? args[0] : Undefined.instance;
        if (generator.delegee == null) {
            return generator.resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, value);
        }
        return generator.resumeDelegeeThrow(cx, scope, value);
    }

    private static Object js_iterator(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return thisObj;
    }

    private Scriptable resumeDelegee(Context cx, Scriptable scope, Object value) {
        try {
            // Be super-careful and only pass an arg to next if it expects one
            Object[] nextArgs =
                    Undefined.isUndefined(value) ? ScriptRuntime.emptyArgs : new Object[] {value};

            var nextFn = ScriptRuntime.getPropAndThis(delegee, ES6Iterator.NEXT_METHOD, cx, scope);
            Object nr = nextFn.call(cx, scope, nextArgs);

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
            var throwFn = ScriptRuntime.getPropAndThis(delegee, "throw", cx, scope);
            Object throwResult = throwFn.call(cx, scope, new Object[] {value});

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
            // See https://tc39.es/ecma262/#sec-iteratorclose (2026, 7.4.11, 4b)
            // This calls GetMethod: https://tc39.es/ecma262/#sec-getmethod (2026, 7.3.10, 2)
            // We need to check if the return value is present and after the call
            // if return value is not undefined treat it same as null
            if (retResult != null && !Undefined.isUndefined(retResult)) {
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
            ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, value);
            ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, Boolean.TRUE);
            return result;
        }

        state = State.EXECUTING;

        Object throwValue = value;
        if (op == NativeGenerator.GENERATOR_CLOSE) {
            if (!(value instanceof NativeGenerator.GeneratorClosedException)) {
                throwValue = new NativeGenerator.GeneratorClosedException(value);
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
            ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, gce.getValue());
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
                Undefined.isUndefined(value) ? ScriptRuntime.emptyArgs : new Object[] {value};
        // Delegate to "return" method. If it's not defined we ignore it
        Object retFnObj =
                ScriptRuntime.getObjectPropNoWarn(delegee, ES6Iterator.RETURN_METHOD, cx, scope);
        // Treat a return method that's null or undefined as if it doesn't exist
        // See https://tc39.es/ecma262/#sec-getmethod (2026, 7.3.10, 2)
        if (retFnObj != null && !Undefined.isUndefined(retFnObj)) {
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
