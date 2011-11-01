package org.mozilla.javascript.v8dtoa;

public class FastDtoaBuffer {

    final char[] chars = new char[FastDtoa.kFastDtoaMaximalLength];
    int end = 0;
    int point;

    public char[] getChars() {
        return chars;
    }

    public int getEnd() {
        return end;
    }

    public int getPoint() {
        return point;
    }

    void append(char c) {
        chars[end++] = c;
    }

    void decreaseLast() {
        chars[end - 1]--;
    }

    void reset() {
        end = 0;
    }

    @Override
    public String toString() {
        return "[chars:" + new String(chars, 0, end) + ", point:" + point + "]";
    }
}
