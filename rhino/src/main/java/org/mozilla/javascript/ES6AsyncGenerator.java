/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.Symbol.Kind.REGULAR;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Async generator object returned by invoking an {@code async function*}. Drives the underlying
 * {@link ES6Generator} through a FIFO queue of pending {@code next}/{@code throw}/{@code return}
 * requests. Each request returns a Promise that resolves to an IteratorResult.
 */
public final class ES6AsyncGenerator extends ScriptableObject {
    private static final long serialVersionUID = 1L;

    static final SymbolKey ASYNC_GENERATOR_TAG = new SymbolKey("AsyncGeneratorPrototype", REGULAR);

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(ASYNC_GENERATOR_TAG)
                        .withMethod(CTOR, "next", 1, ES6AsyncGenerator::js_next)
                        .withMethod(CTOR, "return", 1, ES6AsyncGenerator::js_return)
                        .withMethod(CTOR, "throw", 1, ES6AsyncGenerator::js_throw)
                        .withMethod(
                                CTOR,
                                SymbolKey.ASYNC_ITERATOR,
                                0,
                                ES6AsyncGenerator::js_asyncIterator)
                        .withProp(
                                CTOR,
                                SymbolKey.TO_STRING_TAG,
                                value("AsyncGenerator", DONTENUM | READONLY))
                        .build();
    }

    private final ES6Generator inner;
    private final VarScope homeScope;
    private final Deque<Request> queue = new ArrayDeque<>();
    private boolean draining = false;
    private boolean completed = false;

    static ScriptableObject init(Context cx, TopLevel scope, boolean sealed) {
        NativeObject prototype = new NativeObject();
        DESCRIPTOR.populateGlobal(cx, scope, prototype, sealed);
        if (scope != null) {
            scope.associateValue(ASYNC_GENERATOR_TAG, prototype);
        }
        return prototype;
    }

    /** Only for constructing the prototype. */
    private ES6AsyncGenerator() {
        this.inner = null;
        this.homeScope = null;
    }

    public ES6AsyncGenerator(VarScope scope, ES6Generator inner) {
        this.inner = inner;
        this.homeScope = scope;
        VarScope top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        ScriptableObject prototype =
                (ScriptableObject) ScriptableObject.getTopScopeValue(top, ASYNC_GENERATOR_TAG);
        if (prototype != null) {
            this.setPrototype(prototype);
        }
    }

    @Override
    public String getClassName() {
        return "AsyncGenerator";
    }

    private static ES6AsyncGenerator realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, ES6AsyncGenerator.class);
    }

    private static Object js_next(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj)
                .enqueue(cx, s, RequestKind.NEXT, args.length > 0 ? args[0] : Undefined.instance);
    }

    private static Object js_return(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj)
                .enqueue(cx, s, RequestKind.RETURN, args.length > 0 ? args[0] : Undefined.instance);
    }

    private static Object js_throw(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj)
                .enqueue(cx, s, RequestKind.THROW, args.length > 0 ? args[0] : Undefined.instance);
    }

    private static Object js_asyncIterator(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return thisObj;
    }

    private Object enqueue(Context cx, VarScope scope, RequestKind kind, Object value) {
        VarScope topScope = ScriptableObject.getTopLevelScope(scope);
        Object promiseCtor = TopLevel.getBuiltinCtor(cx, topScope, TopLevel.Builtins.Promise);
        NativePromise.Capability cap = new NativePromise.Capability(cx, scope, promiseCtor);
        queue.addLast(new Request(kind, value, cap));
        // Only start a drain if nothing is already in progress. draining stays true while a
        // step is suspended on an awaited Promise so new enqueues don't race the in-flight step.
        if (!draining) {
            drain(cx, scope);
        }
        return cap.promise;
    }

    private void drain(Context cx, VarScope scope) {
        draining = true;
        while (!queue.isEmpty()) {
            Request req = queue.peekFirst();
            if (completed) {
                resolveCompleted(cx, scope, req);
                queue.pollFirst();
                continue;
            }
            if (!step(cx, scope, req)) {
                // step() suspended on an external Promise; resume() will clear draining
                // and call drain() again when the microtask fires.
                return;
            }
        }
        draining = false;
    }

    /**
     * Run the generator once for the given request. Returns {@code true} if the request was fully
     * resolved synchronously (and can be dequeued immediately), {@code false} if the flow is
     * suspended on an awaited Promise and will continue via its then-reactions.
     */
    private boolean step(Context cx, VarScope scope, Request req) {
        Scriptable result;
        try {
            switch (req.kind) {
                case NEXT:
                    result = inner.resumeLocal(cx, scope, req.value);
                    break;
                case THROW:
                    result =
                            inner.resumeAbruptLocal(
                                    cx, scope, NativeGenerator.GENERATOR_THROW, req.value);
                    break;
                case RETURN:
                default:
                    result =
                            inner.resumeAbruptLocal(
                                    cx, scope, NativeGenerator.GENERATOR_CLOSE, req.value);
                    break;
            }
        } catch (JavaScriptException jse) {
            completed = true;
            req.capability.reject.call(
                    cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] {jse.getValue()});
            queue.pollFirst();
            return true;
        } catch (RhinoException re) {
            completed = true;
            req.capability.reject.call(
                    cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] {re.getMessage()});
            queue.pollFirst();
            return true;
        }

        boolean done =
                Boolean.TRUE.equals(
                        ScriptableObject.getProperty(result, ES6Iterator.DONE_PROPERTY));
        Object value = ScriptableObject.getProperty(result, ES6Iterator.VALUE_PROPERTY);

        if (done) {
            completed = true;
            req.capability.resolve.call(
                    cx,
                    scope,
                    Undefined.SCRIPTABLE_UNDEFINED,
                    new Object[] {makeIteratorResult(cx, scope, value, true)});
            queue.pollFirst();
            return true;
        }

        if (value instanceof ScriptRuntime.AwaitMarker) {
            // Await point: resolve the inner value, resume the generator with the result, and
            // stay on the same request.
            awaitAndContinue(cx, scope, ((ScriptRuntime.AwaitMarker) value).getValue(), req);
            return false;
        }

        // Yield point: await the yielded value, then resolve the consumer's pending promise.
        awaitAndYield(cx, scope, value, req);
        return false;
    }

    /** Resolve a request when the generator is already completed. */
    private void resolveCompleted(Context cx, VarScope scope, Request req) {
        switch (req.kind) {
            case THROW:
                req.capability.reject.call(
                        cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] {req.value});
                break;
            case RETURN:
                req.capability.resolve.call(
                        cx,
                        scope,
                        Undefined.SCRIPTABLE_UNDEFINED,
                        new Object[] {makeIteratorResult(cx, scope, req.value, true)});
                break;
            case NEXT:
            default:
                req.capability.resolve.call(
                        cx,
                        scope,
                        Undefined.SCRIPTABLE_UNDEFINED,
                        new Object[] {makeIteratorResult(cx, scope, Undefined.instance, true)});
                break;
        }
    }

    private void awaitAndContinue(Context cx, VarScope scope, Object awaited, Request req) {
        VarScope topScope = ScriptableObject.getTopLevelScope(scope);
        Object promiseCtor = TopLevel.getBuiltinCtor(cx, topScope, TopLevel.Builtins.Promise);
        NativePromise p =
                (NativePromise) NativePromise.resolveInternal(cx, scope, promiseCtor, awaited);
        VarScope driverScope = homeScope != null ? homeScope : scope;
        p.then(
                cx,
                scope,
                new Object[] {
                    new LambdaFunction(
                            topScope,
                            1,
                            (cx2, s, thisObj, args) -> {
                                Object v = args.length > 0 ? args[0] : Undefined.instance;
                                // Resume the same request with the awaited value.
                                req.value = v;
                                req.kind = RequestKind.NEXT;
                                resumeFromMicrotask(cx2, driverScope);
                                return Undefined.instance;
                            }),
                    new LambdaFunction(
                            topScope,
                            1,
                            (cx2, s, thisObj, args) -> {
                                Object r = args.length > 0 ? args[0] : Undefined.instance;
                                // Resume the same request by throwing the rejection.
                                req.value = r;
                                req.kind = RequestKind.THROW;
                                resumeFromMicrotask(cx2, driverScope);
                                return Undefined.instance;
                            })
                });
    }

    private void awaitAndYield(Context cx, VarScope scope, Object yielded, Request req) {
        VarScope topScope = ScriptableObject.getTopLevelScope(scope);
        Object promiseCtor = TopLevel.getBuiltinCtor(cx, topScope, TopLevel.Builtins.Promise);
        NativePromise p =
                (NativePromise) NativePromise.resolveInternal(cx, scope, promiseCtor, yielded);
        VarScope driverScope = homeScope != null ? homeScope : scope;
        p.then(
                cx,
                scope,
                new Object[] {
                    new LambdaFunction(
                            topScope,
                            1,
                            (cx2, s, thisObj, args) -> {
                                Object v = args.length > 0 ? args[0] : Undefined.instance;
                                req.capability.resolve.call(
                                        cx2,
                                        s,
                                        Undefined.SCRIPTABLE_UNDEFINED,
                                        new Object[] {makeIteratorResult(cx2, s, v, false)});
                                // This request is done; move on to the next.
                                if (queue.peekFirst() == req) {
                                    queue.pollFirst();
                                }
                                resumeFromMicrotask(cx2, driverScope);
                                return Undefined.instance;
                            }),
                    new LambdaFunction(
                            topScope,
                            1,
                            (cx2, s, thisObj, args) -> {
                                Object r = args.length > 0 ? args[0] : Undefined.instance;
                                // A rejected yielded value: forward as a throw into the generator
                                // on the same request.
                                req.value = r;
                                req.kind = RequestKind.THROW;
                                resumeFromMicrotask(cx2, driverScope);
                                return Undefined.instance;
                            })
                });
    }

    /** Re-enter the drain loop from a microtask callback. */
    private void resumeFromMicrotask(Context cx, VarScope scope) {
        boolean needTopCall = !ScriptRuntime.hasTopCall(cx);
        if (needTopCall) {
            cx.topCallScope = ScriptableObject.getTopLevelScope(scope);
            cx.useDynamicScope = cx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE);
        }
        try {
            // The suspended step has completed; release the draining flag before re-entering.
            draining = false;
            drain(cx, scope);
        } finally {
            if (needTopCall) {
                cx.topCallScope = null;
            }
        }
    }

    private static Scriptable makeIteratorResult(
            Context cx, VarScope scope, Object value, boolean done) {
        Scriptable obj = cx.newObject(scope);
        ScriptableObject.putProperty(obj, ES6Iterator.VALUE_PROPERTY, value);
        ScriptableObject.putProperty(obj, ES6Iterator.DONE_PROPERTY, Boolean.valueOf(done));
        return obj;
    }

    private enum RequestKind {
        NEXT,
        THROW,
        RETURN
    }

    private static final class Request {
        RequestKind kind;
        Object value;
        final NativePromise.Capability capability;

        Request(RequestKind kind, Object value, NativePromise.Capability capability) {
            this.kind = kind;
            this.value = value;
            this.capability = capability;
        }
    }
}
