/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.Symbol.Kind.REGULAR;

/**
 * This class implements generator objects. See <a
 * href="http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Generators">Generators</a>
 *
 * @author Norris Boyd
 */
public final class NativeGenerator extends ScriptableObject {
    private static final long serialVersionUID = 1645892441041347273L;

    private static final SymbolKey GENERATOR_TAG = new SymbolKey("GeneratorPrototype", REGULAR);
    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(GENERATOR_TAG)
                        .withMethod(CTOR, "close", 1, NativeGenerator::js_close)
                        .withMethod(CTOR, "next", 1, NativeGenerator::js_next)
                        .withMethod(CTOR, "send", 0, NativeGenerator::js_send)
                        .withMethod(CTOR, "throw", 0, NativeGenerator::js_throw)
                        .withMethod(CTOR, "__iterator__", 1, NativeGenerator::js_iterator)
                        .build();
    }

    static NativeGenerator init(Context cx, TopLevel scope, boolean sealed) {
        var prototype = new NativeGenerator();
        DESCRIPTOR.populateGlobal(cx, scope, prototype, sealed);
        scope.associateValue(GENERATOR_TAG, prototype);
        return prototype;
    }

    /** Only for constructing the prototype object. */
    private NativeGenerator() {}

    public NativeGenerator(VarScope scope, JSFunction function, Object savedState) {
        this.function = function;
        this.savedState = savedState;
        // Set parent and prototype properties. Since we don't have a
        // "Generator" constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        TopLevel top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        NativeGenerator prototype =
                (NativeGenerator) ScriptableObject.getTopScopeValue(top, GENERATOR_TAG);
        this.setPrototype(prototype);
    }

    public static final int GENERATOR_SEND = 0, GENERATOR_THROW = 1, GENERATOR_CLOSE = 2;

    @Override
    public String getClassName() {
        return "Generator";
    }

    private static Object js_close(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        // need to run any pending finally clauses
        var generator = realThis(thisObj);
        return generator.resume(cx, s, GENERATOR_CLOSE, new GeneratorClosedException());
    }

    private static Object js_next(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        // arguments to next() are ignored
        var generator = realThis(thisObj);
        generator.firstTime = false;
        return generator.resume(cx, s, GENERATOR_SEND, Undefined.instance);
    }

    private static Object js_send(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        {
            var generator = realThis(thisObj);
            Object arg = args.length > 0 ? args[0] : Undefined.instance;
            if (generator.firstTime && !arg.equals(Undefined.instance)) {
                throw ScriptRuntime.typeErrorById("msg.send.newborn");
            }
            return generator.resume(cx, s, GENERATOR_SEND, arg);
        }
    }

    private static Object js_throw(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var generator = realThis(thisObj);
        return generator.resume(
                cx, s, GENERATOR_THROW, args.length > 0 ? args[0] : Undefined.instance);
    }

    private static Object js_iterator(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return thisObj;
    }

    private static NativeGenerator realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeGenerator.class);
    }

    private Object resume(Context cx, VarScope scope, int operation, Object value) {
        if (savedState == null) {
            if (operation == GENERATOR_CLOSE) return Undefined.instance;
            Object thrown;
            if (operation == GENERATOR_THROW) {
                thrown = value;
            } else {
                thrown = NativeIterator.getStopIterationObject(scope);
            }
            throw new JavaScriptException(thrown, lineSource, lineNumber);
        }
        try {
            synchronized (this) {
                // generator execution is necessarily single-threaded and
                // non-reentrant.
                // See https://bugzilla.mozilla.org/show_bug.cgi?id=349263
                if (locked) throw ScriptRuntime.typeErrorById("msg.already.exec.gen");
                locked = true;
            }
            return function.resumeGenerator(cx, scope, operation, savedState, value);
        } catch (GeneratorClosedException e) {
            // On closing a generator in the compile path, the generator
            // throws a special exception. This ensures execution of all pending
            // finalizers and will not get caught by user code.
            return Undefined.instance;
        } catch (RhinoException e) {
            lineNumber = e.lineNumber();
            lineSource = e.lineSource();
            savedState = null;
            throw e;
        } finally {
            synchronized (this) {
                locked = false;
            }
            if (operation == GENERATOR_CLOSE) savedState = null;
        }
    }

    private JSFunction function;
    private Object savedState;
    private String lineSource;
    private int lineNumber;
    private boolean firstTime = true;
    private boolean locked;

    public static class GeneratorClosedException extends RuntimeException {
        private static final long serialVersionUID = 2561315658662379681L;
        private final Object value;

        public GeneratorClosedException() {
            this(Undefined.instance);
        }

        public GeneratorClosedException(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }
}
