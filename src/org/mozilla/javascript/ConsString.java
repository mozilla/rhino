/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * <p>This class represents a string composed of two components, each of which
 * may be a <code>java.lang.String</code> or another ConsString.</p>
 *
 * <p>This string representation is optimized for concatenation using the "+"
 * operator. Instead of immediately copying both components to a new character
 * array, ConsString keeps references to the original components and only
 * converts them to a String if either toString() is called or a certain depth
 * level is reached.</p>
 *
 * <p>Note that instances of this class are only immutable if both parts are
 * immutable, i.e. either Strings or ConsStrings that are ultimately composed
 * of Strings.</p>
 *
 * <p>Both the name and the concept are borrowed from V8.</p>
 */
public class ConsString implements CharSequence, Serializable {

    private static final long serialVersionUID = -8432806714471372570L;

    private CharSequence first, second;
    private final int length;
    private boolean isFlat;

    public ConsString(CharSequence str1, CharSequence str2) {
        first = str1;
        second = str2;
        length = str1.length() + str2.length();
        isFlat = false;
    }

    // Replace with string representation when serializing
    private Object writeReplace() {
        return this.toString();
    }
    
    @Override
    public String toString() {
        return isFlat ? (String)first : flatten();
    }

    private synchronized String flatten() {
        if (!isFlat) {
            final char[] chars = new char[length];
            int charPos = length;

            ArrayList<CharSequence> stack = new ArrayList<CharSequence>();
            stack.add(first);

            CharSequence next = second;
            do {
                if (next instanceof ConsString) {
                    ConsString casted = (ConsString) next;
                    if (casted.isFlat) {
                        next = casted.first;
                    } else {
                        stack.add(casted.first);
                        next = casted.second;
                        continue;
                    }
                }

                final String str = (String) next;
                charPos -= str.length();
                str.getChars(0, str.length(), chars, charPos);
                next = stack.isEmpty() ? null : stack.remove(stack.size() - 1);
            } while (next != null);

            first = new String(chars);
            second = "";
            isFlat = true;
        }
        return (String)first;
    }

    public int length() {
        return length;
    }

    public char charAt(int index) {
        String str = isFlat ? (String)first : flatten();
        return str.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        String str = isFlat ? (String)first : flatten();
        return str.substring(start, end);
    }
}
