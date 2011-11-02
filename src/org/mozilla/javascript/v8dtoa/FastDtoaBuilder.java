package org.mozilla.javascript.v8dtoa;

import java.util.Arrays;

public class FastDtoaBuilder {

    // allocate buffer for generated digits + sign, decimal point, exp notation
    final char[] chars = new char[FastDtoa.kFastDtoaMaximalLength + 7];
    int end = 0;
    int point;
    boolean formatted = false;

    void append(char c) {
        chars[end++] = c;
    }

    void decreaseLast() {
        chars[end - 1]--;
    }

    public void reset() {
        end = 0;
        formatted = false;
    }

    @Override
    public String toString() {
        return "[chars:" + new String(chars, 0, end) + ", point:" + point + "]";
    }

    public String format() {
        if (!formatted) {
            // check for minus sign
            int firstDigit = chars[0] == '-' ? 1 : 0;
            int decPoint = point - firstDigit;
            if (decPoint < -5 || decPoint > 21) {
                toExponentialFormat(firstDigit);
            } else {
                toFixedFormat();
            }
            formatted = true;
        }
        return new String(chars, 0, end);

    }

    private void toFixedFormat() {
        if (point < end) {
            if (point > 0) {
                System.arraycopy(chars, point, chars, point + 1, end - point);
                chars[point] = '.';
                end++;
            } else {
                int shift = 2 - point;
                System.arraycopy(chars, 0, chars, shift, end);
                chars[0] = '0';
                chars[1] = '.';
                if (point < 0) {
                    Arrays.fill(chars, 2, shift, '0');
                }
                end += shift;
            }
        } else if (point > end) {
            Arrays.fill(chars, end, point, '0');
            end += point - end;
        }
    }

    private void toExponentialFormat(int firstDigit) {
        if (end - firstDigit > 1) {
            // insert decimal point if more than one digit was produced
            int dot = firstDigit + 1;
            System.arraycopy(chars, dot, chars, dot + 1, end - dot);
            chars[dot] = '.';
            end++;
        }
        chars[end++] = 'e';
        char sign = '+';
        int exp = point - 1;
        if (exp < 0) {
            sign = '-';
            exp = -exp;
        }
        chars[end++] = sign;

        int charPos = exp > 99 ? end + 2 : exp > 9 ? end + 1 : end;
        end = charPos + 1;

        // code below is needed because Integer.getChars() is not public
        for (;;) {
            int r = exp % 10;
            chars[charPos--] = digits[r];
            exp = exp / 10;
            if (exp == 0) break;
        }
    }

    final static char[] digits = {
        '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' , '8' , '9'
    };
}
