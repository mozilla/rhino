package org.mozilla.javascript.v8dtoa;

public class DToABuffer {

    char[] chars = new char[FastDToA.kFastDtoaMaximalLength];
    int end = 0;
    int point;

    void append(char c) {
        chars[end++] = c;
    }

    void decreaseLast() {
        chars[end - 1]--;
    }
}
