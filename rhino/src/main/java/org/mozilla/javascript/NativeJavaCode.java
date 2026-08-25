/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.concurrent.CopyOnWriteArrayList;
import org.mozilla.javascript.lc.member.ExecutableBox;

/**
 * JSCode implementation for Java methods and constructors exposed to JavaScript.
 *
 * <p>Handles reflective method invocation with overload resolution and caching.
 *
 * @see NativeJavaOverloadResolver for overload resolution logic
 */
public class NativeJavaCode extends JSCode<JSFunction> {

    private final ExecutableBox[] methods;
    private final CopyOnWriteArrayList<ResolvedOverload> overloadCache =
            new CopyOnWriteArrayList<>();

    NativeJavaCode(ExecutableBox[] methods) {
        this.methods = methods;
    }

    /** Access to the method array for overload resolution. */
    public ExecutableBox[] getMethods() {
        return methods;
    }

    @Override
    public Object execute(
            Context cx,
            JSFunction executableObject,
            Object newTarget,
            VarScope scope,
            Object thisObj,
            Object[] args) {
        return NativeJavaOverloadResolver.invoke(
                cx,
                scope,
                thisObj,
                args,
                methods,
                overloadCache,
                executableObject.getFunctionName());
    }

    @Override
    public Object resume(
            Context cx,
            JSFunction executableObject,
            Object state,
            VarScope scope,
            int operation,
            Object value) {
        // Java methods are not resumable (not generators). If a method returns a generator,
        // that's handled by wrapping the return value in execute().
        throw Kit.codeBug("NativeJavaCode is not resumable");
    }

    /**
     * Builder for NativeJavaCode. Creates immutable NativeJavaCode instances during descriptor
     * compilation.
     */
    public static class Builder extends JSCode.Builder<JSFunction> {

        private final ExecutableBox[] methods;
        private NativeJavaCode built;

        public Builder(ExecutableBox[] methods) {
            this.methods = methods;
        }

        @Override
        public JSCode<JSFunction> build() {
            if (built == null) {
                built = new NativeJavaCode(methods);
            }
            return built;
        }
    }
}
