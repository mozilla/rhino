/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.Symbol.Kind.REGULAR;

public final class ES6Generator extends ScriptableObject {
    private static final long serialVersionUID = -1617667918827493330L;

    static final SymbolKey GENERATOR_TAG = new SymbolKey("GeneratorPrototype", REGULAR);

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(GENERATOR_TAG)
                        .withMethod(CTOR, "next", 1, ES6Generator::js_next)
                        .withMethod(CTOR, "return", 1, ES6Generator::js_return)
                        .withMethod(CTOR, "throw", 1, ES6Generator::js_throw)
                        .withMethod(CTOR, SymbolKey.ITERATOR, 0, ES6Generator::js_iterator)
                        .withProp(
                                CTOR,
                                SymbolKey.TO_STRING_TAG,
                                value("Generator", DONTENUM | READONLY))
                        .build();
    }

    private JSFunction function;
    private Object savedState;
    private String lineSource;
    private int lineNumber;
    private State state = State.SUSPENDED_START;
    private Object delegee;
    private boolean delegeeIsAsync;
    // When the async driver is awaiting a Promise returned by the async delegee (from
    // next/throw/return), the next resumeLocal/resumeAbruptLocal call provides the awaited
    // IteratorResult (or rejection reason) and must be routed back into the delegee state
    // machine instead of into the inner generator body.
    private boolean awaitingDelegeeStep;
    // Op used to resume the inner generator once the delegee signals done=true while awaiting a
    // step. GENERATOR_SEND for next()/throw(); GENERATOR_CLOSE for return().
    private int delegeeDoneOp = NativeGenerator.GENERATOR_SEND;

    static ScriptableObject init(Context cx, TopLevel scope, boolean sealed) {

        NativeObject prototype = new NativeObject();
        DESCRIPTOR.populateGlobal(cx, scope, prototype, sealed);

        var iterCtor = (JSFunction) scope.get("Iterator", scope);
        prototype.setPrototype((Scriptable) iterCtor.getPrototypeProperty());

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

    public ES6Generator(VarScope scope, JSFunction function, Object savedState) {
        this.function = function;
        this.savedState = savedState;
        // Set parent and prototype properties.
        TopLevel top = ScriptableObject.getTopLevelScope(scope);
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
            ScriptableObject prototype =
                    (ScriptableObject) ScriptableObject.getTopScopeValue(top, GENERATOR_TAG);
            this.setPrototype(prototype);
        }
    }

    @Override
    public String getClassName() {
        return "Generator";
    }

    private static ES6Generator realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, ES6Generator.class);
    }

    private static Object js_return(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ES6Generator generator = realThis(thisObj);
        Object value = args.length >= 1 ? args[0] : Undefined.instance;
        if (generator.delegee == null) {
            return generator.resumeAbruptLocal(cx, s, NativeGenerator.GENERATOR_CLOSE, value);
        }
        return generator.resumeDelegeeReturn(cx, s, value);
    }

    private static Object js_next(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ES6Generator generator = realThis(thisObj);
        Object value = args.length >= 1 ? args[0] : Undefined.instance;
        if (generator.delegee == null) {
            return generator.resumeLocal(cx, s, value);
        }
        return generator.resumeDelegee(cx, s, value);
    }

    private static Object js_throw(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ES6Generator generator = realThis(thisObj);
        Object value = args.length >= 1 ? args[0] : Undefined.instance;
        if (generator.delegee == null) {
            return generator.resumeAbruptLocal(cx, s, NativeGenerator.GENERATOR_THROW, value);
        }
        return generator.resumeDelegeeThrow(cx, s, value);
    }

    private static Object js_iterator(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return thisObj;
    }

    private Scriptable resumeDelegee(Context cx, VarScope scope, Object value) {
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

    private Scriptable resumeDelegeeThrow(Context cx, VarScope scope, Object value) {
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

    private Scriptable resumeDelegeeReturn(Context cx, VarScope scope, Object value) {
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

    /**
     * Call {@code next} on an async delegee. The delegee returns a Promise that the async driver
     * awaits; control flow resumes in {@link #processAsyncDelegeeResult} with the resolved
     * IteratorResult (or in {@link #resumeAbruptLocal} on rejection).
     */
    private Scriptable resumeAsyncDelegee(Context cx, VarScope scope, Object value) {
        try {
            Object[] nextArgs =
                    Undefined.isUndefined(value) ? ScriptRuntime.emptyArgs : new Object[] {value};
            var nextFn = ScriptRuntime.getPropAndThis(delegee, ES6Iterator.NEXT_METHOD, cx, scope);
            Object promise = nextFn.call(cx, scope, nextArgs);
            return awaitDelegeeStep(cx, scope, promise, NativeGenerator.GENERATOR_SEND);
        } catch (RhinoException re) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
    }

    private Scriptable resumeAsyncDelegeeThrow(Context cx, VarScope scope, Object value) {
        Object throwFn;
        try {
            throwFn = ScriptableObject.getProperty((Scriptable) delegee, "throw");
        } catch (RhinoException re) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
        if (throwFn == Scriptable.NOT_FOUND || throwFn == null || Undefined.isUndefined(throwFn)) {
            // No throw method: call return optionally, then throw original value into inner.
            try {
                callReturnOptionally(cx, scope, Undefined.instance);
            } catch (RhinoException re2) {
                delegee = null;
                delegeeIsAsync = false;
                return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re2);
            }
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, value);
        }
        if (!(throwFn instanceof Callable)) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(
                    cx,
                    scope,
                    NativeGenerator.GENERATOR_THROW,
                    ScriptRuntime.notFunctionError((Scriptable) delegee, "throw"));
        }
        try {
            Object promise =
                    ((Callable) throwFn)
                            .call(cx, scope, (Scriptable) delegee, new Object[] {value});
            return awaitDelegeeStep(cx, scope, promise, NativeGenerator.GENERATOR_SEND);
        } catch (RhinoException re) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
    }

    private Scriptable resumeAsyncDelegeeReturn(Context cx, VarScope scope, Object value) {
        Object retFn;
        try {
            retFn =
                    ScriptRuntime.getObjectPropNoWarn(
                            delegee, ES6Iterator.RETURN_METHOD, cx, scope);
        } catch (RhinoException re) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
        if (retFn == null || Undefined.isUndefined(retFn)) {
            // No "return" -- close the inner with the caller's value.
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_CLOSE, value);
        }
        if (!(retFn instanceof Callable)) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(
                    cx,
                    scope,
                    NativeGenerator.GENERATOR_THROW,
                    ScriptRuntime.notFunctionError(
                            (Scriptable) delegee, ES6Iterator.RETURN_METHOD));
        }
        Object[] retArgs =
                Undefined.isUndefined(value) ? ScriptRuntime.emptyArgs : new Object[] {value};
        try {
            Object promise = ((Callable) retFn).call(cx, scope, (Scriptable) delegee, retArgs);
            return awaitDelegeeStep(cx, scope, promise, NativeGenerator.GENERATOR_CLOSE);
        } catch (RhinoException re) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
        }
    }

    /**
     * Wrap a Promise returned by an async delegee method in an {@link ScriptRuntime.AwaitMarker} so
     * the async driver awaits it. When the await settles the driver will call back into resumeLocal
     * (on fulfilment) or resumeAbruptLocal (on rejection).
     */
    private Scriptable awaitDelegeeStep(Context cx, VarScope scope, Object promise, int doneOp) {
        awaitingDelegeeStep = true;
        delegeeDoneOp = doneOp;
        Scriptable result = ES6Iterator.makeIteratorResult(cx, scope, Boolean.FALSE);
        ScriptableObject.putProperty(
                result, ES6Iterator.VALUE_PROPERTY, new ScriptRuntime.AwaitMarker(promise));
        return result;
    }

    /**
     * Process the IteratorResult that the driver produced by awaiting the Promise from an async
     * delegee method. If the result signals {@code done}, resume the inner generator with the
     * result's value (using SEND or CLOSE as recorded in {@link #delegeeDoneOp}); otherwise yield
     * the value to the consumer.
     */
    private Scriptable processAsyncDelegeeResult(Context cx, VarScope scope, Object awaited) {
        if (!(awaited instanceof Scriptable)) {
            delegee = null;
            delegeeIsAsync = false;
            return resumeAbruptLocal(
                    cx,
                    scope,
                    NativeGenerator.GENERATOR_THROW,
                    ScriptRuntime.typeErrorById("msg.invalid.iterator"));
        }
        Scriptable ir = (Scriptable) awaited;
        boolean done = ScriptRuntime.isIteratorDone(cx, ir);
        Object value = ScriptableObject.getProperty(ir, ES6Iterator.VALUE_PROPERTY);
        if (done) {
            int op = delegeeDoneOp;
            delegee = null;
            delegeeIsAsync = false;
            if (op == NativeGenerator.GENERATOR_CLOSE) {
                return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_CLOSE, value);
            }
            return resumeLocal(cx, scope, value);
        }
        return ir;
    }

    Scriptable resumeLocal(Context cx, VarScope scope, Object value) {
        if (delegee != null) {
            if (awaitingDelegeeStep) {
                awaitingDelegeeStep = false;
                return processAsyncDelegeeResult(cx, scope, value);
            }
            if (delegeeIsAsync) {
                return resumeAsyncDelegee(cx, scope, value);
            }
            return resumeDelegee(cx, scope, value);
        }
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
                    if (function.isAsync() && function.isGeneratorFunction()) {
                        // In async generators, yield* first tries Symbol.asyncIterator and only
                        // falls back to Symbol.iterator when that lookup does not yield a method.
                        ScriptRuntime.AsyncIteratorResult ar =
                                ScriptRuntime.callAsyncIterator(ysResult.getResult(), cx, scope);
                        delegee = ar.getIterator();
                        delegeeIsAsync = ar.isAsync();
                    } else {
                        delegee = ScriptRuntime.callIterator(ysResult.getResult(), cx, scope);
                        delegeeIsAsync = false;
                    }
                } catch (RhinoException re) {
                    // Need to handle exceptions if the iterator cannot be called.
                    return resumeAbruptLocal(cx, scope, NativeGenerator.GENERATOR_THROW, re);
                }

                Scriptable delResult;
                try {
                    // Re-execute but update state in case we end up back here
                    // Value shall be Undefined based on the very complex spec!
                    if (delegeeIsAsync) {
                        delResult = resumeAsyncDelegee(cx, scope, Undefined.instance);
                    } else {
                        delResult = resumeDelegee(cx, scope, Undefined.instance);
                    }
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

    Scriptable resumeAbruptLocal(Context cx, VarScope scope, int op, Object value) {
        if (delegee != null) {
            if (awaitingDelegeeStep) {
                // We were awaiting a Promise from the delegee when the driver delivered a
                // rejection (op==THROW) as part of that same request. Tear down the delegee and
                // fall through to throw the rejection into the inner generator at the yield*.
                awaitingDelegeeStep = false;
                delegee = null;
                delegeeIsAsync = false;
            } else if (delegeeIsAsync) {
                if (op == NativeGenerator.GENERATOR_CLOSE) {
                    return resumeAsyncDelegeeReturn(cx, scope, value);
                }
                return resumeAsyncDelegeeThrow(cx, scope, value);
            } else {
                if (op == NativeGenerator.GENERATOR_THROW) {
                    return resumeDelegeeThrow(cx, scope, value);
                }
                if (op == NativeGenerator.GENERATOR_CLOSE) {
                    return resumeDelegeeReturn(cx, scope, value);
                }
            }
        }
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

    private Object callReturnOptionally(Context cx, VarScope scope, Object value) {
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
