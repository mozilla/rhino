/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.lc.type.TypeInfo;

public class LiveConnectSupport {
    private static final LiveConnectSupport myself = new LiveConnectSupport();

    private final LiveConnect liveConnect;

    private LiveConnectSupport() {
        // This will be set to null if LC is not loaded
        liveConnect = ScriptRuntime.loadOneServiceImplementation(LiveConnect.class);
    }

    public static LiveConnectSupport get() {
        return myself;
    }

    public boolean isAvailable() {
        return liveConnect != null;
    }

    public WrapProcessor getWrapProcessor() {
        return liveConnect == null ? null : liveConnect.getWrapProcessor();
    }

    /**
     * Convert "value" to the requested Java class. If LiveConnect is available, follow the LC spec.
     * If it is not available, then try to convert to a primitive, or throw an Error.
     */
    public Object coerceType(TypeInfo type, Object value) {
        if (liveConnect == null) {
            return coercePrimitiveType(type, value);
        }
        return liveConnect.coerceType(type, value);
    }

    /**
     * Implement a simplified version of the type conversion logic implemented by LiveConnect purely
     * for the purpose of converting primitive types.
     */
    private Object coercePrimitiveType(TypeInfo type, Object value) {
        if (type.isBoolean()) {
            return ScriptRuntime.toBoolean(value);
        }
        if (type.isInt()) {
            return ScriptRuntime.toInt32(value);
        }
        if (type.isNumber()) {
            return ScriptRuntime.toNumber(value);
        }
        if (type.isString()) {
            return ScriptRuntime.toString(value);
        }
        if (type.isObjectExact()) {
            return value;
        }
        throw ScriptRuntime.typeErrorById("msg.conversion.not.allowed", String.valueOf(value));
    }
}
