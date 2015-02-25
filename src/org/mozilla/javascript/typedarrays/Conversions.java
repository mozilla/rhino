 /* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.ScriptRuntime;

/**
 * Numeric conversions from section 7 of the ECMAScript 6 standard.
 */

public class Conversions
{
    public static final int EIGHT_BIT = 1 << 8;
    public static final int SIXTEEN_BIT = 1 << 16;
    public static final long THIRTYTWO_BIT = 1L << 32L;

    public static int toInt8(Object arg)
    {
        int iv;
        if (arg instanceof Integer) {
            iv = (Integer)arg;
        } else {
            iv = ScriptRuntime.toInt32(arg);
        }

        int int8Bit = iv % EIGHT_BIT;
        return (int8Bit >= (1 << 7)) ? (int8Bit - EIGHT_BIT) : int8Bit;
    }

    public static int toUint8(Object arg)
    {
        int iv;
        if (arg instanceof Integer) {
            iv = ((Integer)arg);
        } else {
            iv = ScriptRuntime.toInt32(arg);
        }

        return iv % EIGHT_BIT;
    }

    public static int toUint8Clamp(Object arg)
    {
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
            return (int)(f + 1.0);
        }
        if (d < (f + 0.5)) {
            return (int)f;
        }
        if (((int)f % 2) != 0) {
            return (int)f + 1;
        }
        return (int)f;
    }

    public static int toInt16(Object arg)
    {
        int iv;
        if (arg instanceof Integer) {
            iv = ((Integer)arg);
        } else {
            iv = ScriptRuntime.toInt32(arg);
        }

        int int16Bit = iv % SIXTEEN_BIT;
        return (int16Bit >= (1 << 15)) ? (int16Bit - SIXTEEN_BIT) : int16Bit;
    }

    public static int toUint16(Object arg)
    {
        int iv;
        if (arg instanceof Integer) {
            iv = ((Integer)arg);
        } else {
            iv = ScriptRuntime.toInt32(arg);
        }

        return iv % SIXTEEN_BIT;
    }

    public static int toInt32(Object arg)
    {
        long lv = (long)ScriptRuntime.toNumber(arg);
        long int32Bit = lv % THIRTYTWO_BIT;
        return (int)((int32Bit >= (1L << 31L)) ? (int32Bit - THIRTYTWO_BIT) : int32Bit);
    }

    public static long toUint32(Object arg)
    {
        long lv = (long)ScriptRuntime.toNumber(arg);
        return lv % THIRTYTWO_BIT;
    }
}
