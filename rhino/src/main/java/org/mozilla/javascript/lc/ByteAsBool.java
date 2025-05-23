package org.mozilla.javascript.lc;

/**
 * @author ZZZank
 */
public final class ByteAsBool {
    private ByteAsBool() {}

    public static final byte UNKNOWN = -1;
    public static final byte FALSE = 0;
    public static final byte TRUE = 1;

    public static byte fromBool(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static boolean isUnknown(byte b) {
        return b < 0;
    }

    public static boolean isKnown(byte b) {
        return b >= 0;
    }

    public static boolean isFalse(byte b) {
        return b == FALSE;
    }

    public static boolean isTrue(byte b) {
        return b == TRUE;
    }
}
