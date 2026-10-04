/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.lc.type.TypeInfo;

/**
 * This is the abstract interface for wrapping Java objects as JavaScript objects using reflection,
 * based mostly on the LiveConnect standard. It is loaded on demand from the rhino-reflect module
 * when it is present.
 */
public interface AutomaticWrapper {
    Object wrap(Context cx, VarScope scope, Object obj, TypeInfo type, boolean wrapPrimitives);

    Scriptable wrapNewObject(Context cx, VarScope scope, Object obj);

    Scriptable wrapAsJavaObject(Context cx, VarScope scope, Object javaObject, TypeInfo type);

    Scriptable wrapJavaClass(Context cx, VarScope scope, Class<?> javaClass);
}
