/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements a lambda function which is known to the runtime system and which may be
 * treated specially by the interpreter or runtime.
 */
public class KnownBuiltInFunction extends LambdaFunction {

    private static final long serialVersionUID = -8388132362854748293L;

    private final Object tag;

    /**
     * Create a new function. The new object will have the Function prototype and no parent. The
     * caller is responsible for binding this object to the appropriate scope.
     *
     * @param tag an object used by the system to identify this function
     * @param scope scope of the calling context
     * @param name name of the function
     * @param length the arity of the function
     * @param target an object that implements the function in Java. Since Callable is a
     *     single-function interface this will typically be implemented as a lambda.
     */
    public KnownBuiltInFunction(
            Object tag,
            Scriptable scope,
            String name,
            int length,
            Object prototype,
            SerializableCallable target) {
        super(scope, name, length, prototype, target);
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }
}
