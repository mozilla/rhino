package org.mozilla.javascript.dtoa;

/** This class formats a decimal number into a string representation in various ways. */
public class Decimal {
    private final String digits;
    private final int decimalPoint;
    private final boolean negative;

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
        if (decimalPoint > -6 && decimalPoint <= 21) {
            return toStringFixed();
        }
        return toStringExponential();
    }

    private String toStringFixed() {
        // Performance notes: as of Java 21, since this method is sensitive:
        //   * Doing string concatenation right in the code is generally faster if
        //     all the strings are constants or can be easily calculated (i.e. substring)
        //   * Using StringBuilder is generally faster if we have to pad
        //     with an arbitrary number of zeroes.
        int numDigits = digits.length();
        if (numDigits == decimalPoint) {
            if (negative) {
                return "-" + digits;
            }
            return digits;
        } else if (decimalPoint <= 0) {
            // 0.[000]digits
            StringBuilder s = new StringBuilder(numDigits - decimalPoint + 3);
            if (negative) {
                s.append('-');
            }
            s.append("0.");
            makeZeroes(s, -decimalPoint);
            s.append(digits);
            trimZeroes(s);
            return s.toString();
        } else if (decimalPoint < numDigits) {
            // dig.its
            String s;
            if (negative) {
                s = "-" + digits.substring(0, decimalPoint) + '.' + digits.substring(decimalPoint);
            } else {
                s = digits.substring(0, decimalPoint) + '.' + digits.substring(decimalPoint);
            }
            return trimZeroes(s);
        } else {
            // digits[000]
            StringBuilder s = new StringBuilder(numDigits + decimalPoint + 1);
            if (negative) {
                s.append('-');
            }
            s.append(digits);
            makeZeroes(s, decimalPoint - numDigits);
            return s.toString();
        }
    }

    private String toStringExponential() {
        int scientificExponent = decimalPoint - 1;
        String exp = scientificExponent >= 0 ? "e+" + scientificExponent : "e" + scientificExponent;
        String fraction = trimZeroes(digits.substring(1));
        if (negative) {
            if (fraction.isEmpty()) {
                return "-" + digits.charAt(0) + exp;
            }
            return "-" + digits.charAt(0) + "." + fraction + exp;
        }
        if (fraction.isEmpty()) {
            return digits.charAt(0) + exp;
        }
        return digits.charAt(0) + "." + fraction + exp;
    }

    private static void makeZeroes(StringBuilder s, int count) {
        for (int i = 0; i < count; i++) {
            s.append('0');
        }
    }

    private static String trimZeroes(String s) {
        int last = s.length() - 1;
        while (last >= 0 && s.charAt(last) == '0') {
            last--;
        }
        if (last != s.length() - 1) {
            return s.substring(0, last + 1);
        }
        return s;
    }

    private static void trimZeroes(StringBuilder s) {
        int last = s.length() - 1;
        while (last >= 0 && s.charAt(last) == '0') {
            last--;
        }
        if (last != s.length() - 1) {
            s.setLength(last + 1);
        }
    }
}
