/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.ScriptRuntime;

/** Numeric conversions from section 7 of the ECMAScript 6 standard. */
public class Conversions {
    public static int toInt8(Object arg) {
        return (byte) ScriptRuntime.toInt32(arg);
    }

    public static int toUint8(Object arg) {
        return ScriptRuntime.toInt32(arg) & 0xff;
    }

    public static int toUint8Clamp(Object arg) {
        double d = ScriptRuntime.toNumber(arg);
        if (d <= 0.0) {
            return 0;
        }
        if (d >= 255.0) {
            return 255;
        }

        // Complex rounding behavior -- see 7.1.11
        double f = Math.floor(d);
        if ((f + 0.5) < d) {
            return (int) (f + 1.0);
        }
        if (d < (f + 0.5)) {
            return (int) f;
        }
        if (((int) f % 2) != 0) {
            return (int) f + 1;
        }
        return (int) f;
    }

    public static int toInt16(Object arg) {
        return (short) ScriptRuntime.toInt32(arg);
    }

    public static int toUint16(Object arg) {
        return ScriptRuntime.toInt32(arg) & 0xffff;
    }

    public static int toInt32(Object arg) {
        return ScriptRuntime.toInt32(arg);
    }

    public static long toUint32(Object arg) {
        return ScriptRuntime.toUint32(arg);
    }
}
