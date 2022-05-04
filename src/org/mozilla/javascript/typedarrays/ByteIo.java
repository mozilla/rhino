/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

public class ByteIo {
    public static Byte readInt8(byte[] buf, int offset) {
        return Byte.valueOf(buf[offset]);
    }

    public static void writeInt8(byte[] buf, int offset, int val) {
        buf[offset] = (byte) val;
    }

    public static Integer readUint8(byte[] buf, int offset) {
        return Integer.valueOf(buf[offset] & 0xff);
    }

    public static void writeUint8(byte[] buf, int offset, int val) {
        buf[offset] = (byte) (val & 0xff);
    }

    private static short doReadInt16(byte[] buf, int offset, boolean littleEndian) {
        // Need to coalesce to short here so that we stay in range
        if (littleEndian) {
            return (short) ((buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8));
        }
        return (short) (((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff));
    }

    private static void doWriteInt16(byte[] buf, int offset, int val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xff);
            buf[offset + 1] = (byte) ((val >>> 8) & 0xff);
        } else {
            buf[offset] = (byte) ((val >>> 8) & 0xff);
            buf[offset + 1] = (byte) (val & 0xff);
        }
    }

    public static Short readInt16(byte[] buf, int offset, boolean littleEndian) {
        return Short.valueOf(doReadInt16(buf, offset, littleEndian));
    }

    public static void writeInt16(byte[] buf, int offset, int val, boolean littleEndian) {
        doWriteInt16(buf, offset, val, littleEndian);
    }

    public static Integer readUint16(byte[] buf, int offset, boolean littleEndian) {
        return Integer.valueOf(doReadInt16(buf, offset, littleEndian) & 0xffff);
    }

    public static void writeUint16(byte[] buf, int offset, int val, boolean littleEndian) {
        doWriteInt16(buf, offset, val & 0xffff, littleEndian);
    }

    public static Integer readInt32(byte[] buf, int offset, boolean littleEndian) {
        if (littleEndian) {
            return Integer.valueOf(
                    (buf[offset] & 0xff)
                            | ((buf[offset + 1] & 0xff) << 8)
                            | ((buf[offset + 2] & 0xff) << 16)
                            | ((buf[offset + 3] & 0xff) << 24));
        }
        return Integer.valueOf(
                ((buf[offset] & 0xff) << 24)
                        | ((buf[offset + 1] & 0xff) << 16)
                        | ((buf[offset + 2] & 0xff) << 8)
                        | (buf[offset + 3] & 0xff));
    }

    public static void writeInt32(byte[] buf, int offset, int val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xff);
            buf[offset + 1] = (byte) ((val >>> 8) & 0xff);
            buf[offset + 2] = (byte) ((val >>> 16) & 0xff);
            buf[offset + 3] = (byte) ((val >>> 24) & 0xff);
        } else {
            buf[offset] = (byte) ((val >>> 24) & 0xff);
            buf[offset + 1] = (byte) ((val >>> 16) & 0xff);
            buf[offset + 2] = (byte) ((val >>> 8) & 0xff);
            buf[offset + 3] = (byte) (val & 0xff);
        }
    }

    public static long readUint32Primitive(byte[] buf, int offset, boolean littleEndian) {
        if (littleEndian) {
            return ((buf[offset] & 0xffL)
                            | ((buf[offset + 1] & 0xffL) << 8L)
                            | ((buf[offset + 2] & 0xffL) << 16L)
                            | ((buf[offset + 3] & 0xffL) << 24L))
                    & 0xffffffffL;
        }
        return (((buf[offset] & 0xffL) << 24L)
                        | ((buf[offset + 1] & 0xffL) << 16L)
                        | ((buf[offset + 2] & 0xffL) << 8L)
                        | (buf[offset + 3] & 0xffL))
                & 0xffffffffL;
    }

    public static void writeUint32(byte[] buf, int offset, long val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 3] = (byte) ((val >>> 24L) & 0xffL);
        } else {
            buf[offset] = (byte) ((val >>> 24L) & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 3] = (byte) (val & 0xffL);
        }
    }

    public static Object readUint32(byte[] buf, int offset, boolean littleEndian) {
        return Long.valueOf(readUint32Primitive(buf, offset, littleEndian));
    }

    public static long readUint64Primitive(byte[] buf, int offset, boolean littleEndian) {
        if (littleEndian) {
            return ((buf[offset] & 0xffL)
                    | ((buf[offset + 1] & 0xffL) << 8L)
                    | ((buf[offset + 2] & 0xffL) << 16L)
                    | ((buf[offset + 3] & 0xffL) << 24L)
                    | ((buf[offset + 4] & 0xffL) << 32L)
                    | ((buf[offset + 5] & 0xffL) << 40L)
                    | ((buf[offset + 6] & 0xffL) << 48L)
                    | ((buf[offset + 7] & 0xffL) << 56L));
        }
        return (((buf[offset] & 0xffL) << 56L)
                | ((buf[offset + 1] & 0xffL) << 48L)
                | ((buf[offset + 2] & 0xffL) << 40L)
                | ((buf[offset + 3] & 0xffL) << 32L)
                | ((buf[offset + 4] & 0xffL) << 24L)
                | ((buf[offset + 5] & 0xffL) << 16L)
                | ((buf[offset + 6] & 0xffL) << 8L)
                | ((buf[offset + 7] & 0xffL) << 0L));
    }

    public static void writeUint64(byte[] buf, int offset, long val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 3] = (byte) ((val >>> 24L) & 0xffL);
            buf[offset + 4] = (byte) ((val >>> 32L) & 0xffL);
            buf[offset + 5] = (byte) ((val >>> 40L) & 0xffL);
            buf[offset + 6] = (byte) ((val >>> 48L) & 0xffL);
            buf[offset + 7] = (byte) ((val >>> 56L) & 0xffL);
        } else {
            buf[offset] = (byte) ((val >>> 56L) & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 48L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 40L) & 0xffL);
            buf[offset + 3] = (byte) ((val >>> 32L) & 0xffL);
            buf[offset + 4] = (byte) ((val >>> 24L) & 0xffL);
            buf[offset + 5] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 6] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 7] = (byte) (val & 0xffL);
        }
    }

    public static Float readFloat32(byte[] buf, int offset, boolean littleEndian) {
        long base = readUint32Primitive(buf, offset, littleEndian);
        return Float.valueOf(Float.intBitsToFloat((int) base));
    }

    public static void writeFloat32(byte[] buf, int offset, double val, boolean littleEndian) {
        long base = Float.floatToIntBits((float) val);
        writeUint32(buf, offset, base, littleEndian);
    }

    public static Double readFloat64(byte[] buf, int offset, boolean littleEndian) {
        long base = readUint64Primitive(buf, offset, littleEndian);
        return Double.valueOf(Double.longBitsToDouble(base));
    }

    public static void writeFloat64(byte[] buf, int offset, double val, boolean littleEndian) {
        long base = Double.doubleToLongBits(val);
        writeUint64(buf, offset, base, littleEndian);
    }
}
