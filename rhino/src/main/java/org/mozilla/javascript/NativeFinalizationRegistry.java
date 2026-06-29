/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ECMAScript 2021 FinalizationRegistry.
 *
 * <p>Allows registering cleanup callbacks that are invoked when registered objects are garbage
 * collected. Uses {@link PhantomReference} for GC detection, which is available on all platforms
 * including Android.
 *
 * @see <a href="https://tc39.es/ecma262/#sec-finalization-registry-objects">ECMAScript
 *     FinalizationRegistry Objects</a>
 */
public class NativeFinalizationRegistry extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "FinalizationRegistry";

    private static final ClassDescriptor DESCRIPTOR;

    private final Callable cleanupCallback;
    private final VarScope parentScope;

    /**
     * Keep track of registered references, so we only invoke callbacks when they are still
     * registered. Not used unless finalizations are enabled on the context.
     */
    private final Map<Registration, Boolean> activeRegistrations = new ConcurrentHashMap<>();

    /**
     * Keep track of unregistration tokens. Must be a Weak map to prevent memory leaks in case
     * unregistration tokens are GCed, which is consistent with how other engines do it.
     */
    private final Map<Object, Unregistration> tokenIndex =
            Collections.synchronizedMap(new WeakHashMap<>());

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME, 1, NativeFinalizationRegistry::jsConstructor)
                        .withMethod(PROTO, "register", 2, NativeFinalizationRegistry::register)
                        .withMethod(PROTO, "unregister", 1, NativeFinalizationRegistry::unregister)
                        .withProp(
                                PROTO,
                                SymbolKey.TO_STRING_TAG,
                                value(CLASS_NAME, DONTENUM | READONLY))
                        .build();
    }

    /** Initialize FinalizationRegistry constructor and prototype. */
    static JSFunction init(Context cx, VarScope s, boolean sealed) {
        return DESCRIPTOR.buildConstructor(cx, s, new NativeObject(), sealed);
    }

    /** JavaScript constructor implementation. */
    private static Scriptable jsConstructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object to, Object[] args) {
        if (args.length < 1 || !(args[0] instanceof Callable)) {
            throw ScriptRuntime.typeErrorById("msg.finalization.callback.required");
        }

        NativeFinalizationRegistry r = new NativeFinalizationRegistry((Function) args[0], s);
        r.setParentScope(f.getDeclarationScope());
        r.setPrototype((Scriptable) f.getPrototypeProperty());
        return r;
    }

    private NativeFinalizationRegistry(Callable cleanupCallback, VarScope s) {
        this.cleanupCallback = cleanupCallback;
        this.parentScope = s;
    }

    private static Object register(
            Context cx, JSFunction f, Object nt, VarScope s, Object to, Object[] args) {
        var self = LambdaConstructor.convertThisObject(to, NativeFinalizationRegistry.class);
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter", CLASS_NAME + ".register", 1, args.length);
        }

        var target = args[0];
        if (!NativeWeakRef.canBeHeldWeakly(target)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalization.invalid.target", ScriptRuntime.typeof(target));
        }

        var heldValue = args.length > 1 ? args[1] : Undefined.instance;
        if (heldValue == target) {
            throw ScriptRuntime.typeErrorById("msg.finalization.target.same.as.held");
        }

        var unregisterToken = args.length > 2 ? args[2] : Undefined.instance;
        if (!Undefined.isUndefined(unregisterToken)
                && !NativeWeakRef.canBeHeldWeakly(unregisterToken)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalization.invalid.token", ScriptRuntime.typeof(unregisterToken));
        }

        Registration ref;
        if (cx.isFinalizationEnabled()) {
            // Register the reference in the reference queue associated with the current Context.
            ref = new Registration(target, self, heldValue, cx.getReferenceQueue());
            // Track it so that we can be only fire callbacks on registered objects
            self.activeRegistrations.put(ref, true);
        } else {
            ref = null;
        }

        if (!Undefined.isUndefined(unregisterToken)) {
            // Register this callback using the unregister token
            self.tokenIndex.compute(
                    unregisterToken,
                    (k, v) -> {
                        var reg = v;
                        if (reg == null) {
                            reg = new Unregistration();
                        }
                        reg.count++;
                        if (ref != null) {
                            reg.registrations.add(ref);
                        }
                        return reg;
                    });
        }

        return Undefined.instance;
    }

    private static Object unregister(
            Context cx, JSFunction f, Object nt, VarScope s, Object to, Object[] args) {
        var self = LambdaConstructor.convertThisObject(to, NativeFinalizationRegistry.class);

        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    CLASS_NAME + ".unregister",
                    "1",
                    String.valueOf(args.length));
        }

        Object token = args[0];
        if (!NativeWeakRef.canBeHeldWeakly(token)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalization.invalid.token", ScriptRuntime.typeof(token));
        }

        // Count registrations for this token. We won't return a count
        // if the token itself has been GCed.
        var reg = self.tokenIndex.remove(token);
        if (reg != null) {
            for (var ref : reg.registrations) {
                self.activeRegistrations.remove(ref);
                ref.clear();
            }
            return reg.count > 0;
        }
        return false;
    }

    /**
     * Called by Context during microtask processing whenever anything comes off the reference
     * queue.
     */
    void cleanup(Context cx, Registration ref) {
        if (activeRegistrations.remove(ref)) {
            try {
                cleanupCallback.call(cx, parentScope, null, new Object[] {ref.heldValue});
            } catch (RhinoException e) {
                // Per spec, errors in cleanup callbacks don't propagate
                Context.reportWarning(
                        "FinalizationRegistry cleanup callback threw: " + e.getMessage());
            }
        }
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    /** PhantomReference that tracks registered objects for finalization. */
    static class Registration extends PhantomReference<Object> {
        private final NativeFinalizationRegistry registry;
        private final Object heldValue;

        Registration(
                Object target,
                NativeFinalizationRegistry registry,
                Object heldValue,
                ReferenceQueue<Object> queue) {
            super(target, queue);
            this.registry = registry;
            this.heldValue = heldValue;
        }

        NativeFinalizationRegistry getRegistry() {
            return registry;
        }
    }

    /** The record that we register in the unregistration table */
    private static class Unregistration {
        int count;
        final HashSet<Registration> registrations = new HashSet<>();

        Unregistration() {}
    }
}
