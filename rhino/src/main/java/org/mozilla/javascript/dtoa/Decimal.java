package org.mozilla.javascript.dtoa;

/** This class formats a decimal number into a string representation in various ways. */
public class Decimal {
    private final String digits;
    private final int decimalPoint;
    private final boolean negative;
    private int length;
    private char[] buf;

    Decimal(String d, int p, boolean n) {
        this.digits = d;
        this.decimalPoint = p;
        this.negative = n;
    }

    /**
     * Format the number according to the formatting rules in EcmaScript chapter 6, as defined for
     * the "Number::toString" operation, for radix 10.
     */
    @Override
    public String toString() {
        if (decimalPoint >= -5 && decimalPoint <= 21) {
            return toStringFixed();
        }
        return toStringExponential();
    }

    private String toStringFixed() {
        int numDigits = digits.length();
        if (numDigits == decimalPoint) {
            if (negative) {
                return "-" + digits;
            }
            return digits;
        }
        if (decimalPoint <= 0) {
            return toStringSmall(numDigits);
        }
        if (decimalPoint < numDigits) {
            return toStringMedium(numDigits);
        }
        return toStringBig(numDigits);
    }

    private String toStringSmall(int numDigits) {
        // Length for -, 0., extra zeroes, digits
        int extraZeroes = -decimalPoint;
        buf = new char[numDigits + extraZeroes + 4];
        if (negative) {
            append('-');
        }
        append('0');
        append('.');
        appendZeroes(extraZeroes);
        append(digits);
        trimZeroes();
        return makeString();
    }

    private String toStringMedium(int numDigits) {
        // Length for -, ., digits
        buf = new char[numDigits + 2];
        if (negative) {
            append('-');
        }
        append(digits.substring(0, decimalPoint));
        append('.');
        append(digits.substring(decimalPoint));
        trimZeroes();
        return makeString();
    }

    private String toStringBig(int numDigits) {
        int extraZeroes = decimalPoint - numDigits;
        // Room for -, zeroes, digits
        buf = new char[numDigits + extraZeroes + 2];
        if (negative) {
            append('-');
        }
        append(digits);
        appendZeroes(extraZeroes);
        return makeString();
    }

    private String toStringExponential() {
        // Length for -, ., e+, three digits
        buf = new char[digits.length() + 8];
        int scientificExponent = decimalPoint - 1;
        if (negative) {
            append('-');
        }
        append(digits.charAt(0));
        append('.');
        append(digits.substring(1));
        trimZeroes();
        append('e');
        if (scientificExponent >= 0) {
            append('+');
        }
        append(Integer.toString(scientificExponent));
        return makeString();
    }

    private void appendZeroes(int count) {
        for (int i = 0; i < count; i++) {
            append('0');
        }
    }

    private void trimZeroes() {
        while (length > 0 && buf[length - 1] == '0') {
            length--;
        }
        if (length > 0 && buf[length - 1] == '.') {
            length--;
        }
    }

    private void append(char ch) {
        buf[length++] = ch;
    }

    private void append(String s) {
        s.getChars(0, s.length(), buf, length);
        length += s.length();
    }

    private String makeString() {
        return new String(buf, 0, length);
    }
}
