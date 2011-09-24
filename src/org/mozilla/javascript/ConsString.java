package org.mozilla.javascript;

public class ConsString implements CharSequence {

    private CharSequence s1, s2;
    private final int length, depth;

    public ConsString(CharSequence str1, CharSequence str2) {
        s1 = str1;
        s2 = str2;
        length = str1.length() + str2.length();
        int d = 1;
        if (str1 instanceof ConsString) d += ((ConsString)str1).depth;
        if (str2 instanceof ConsString) d += ((ConsString)str2).depth;
        if (d > 100) {
            flatten();
            depth = 1;
        } else {
            depth = d;
        }
    }

    public String toString() {
        if (!(s1 instanceof String) || s2 != "") {
            flatten();
        }
        return (String) s1;
    }

    private synchronized void flatten() {
        StringBuilder b = new StringBuilder(length);
        appendTo(b);
        s1 = b.toString();
        s2 = "";
    }

    private synchronized void appendTo(StringBuilder b) {
        appendFragment(s1, b);
        appendFragment(s2, b);
    }

    private static void appendFragment(CharSequence s, StringBuilder b) {
        if (s instanceof ConsString) {
            ((ConsString)s).appendTo(b);
        } else {
            b.append(s);
        }
    }

    public int length() {
        return length;
    }

    public synchronized char charAt(int index) {
        if ((index < 0) || (index >= length)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        int l1 = s1.length();
        return index >= l1 ? s2.charAt(index - l1) : s1.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }
}
