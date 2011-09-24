/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Hannes Wallnoefer
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

/**
 * <p>A string implementation optimized for concatenation using the "+" operator.
 * Instead of copying characters to a new character array, this class keeps
 * references to the two character sequences. Characters are only converted
 * to a String if either toString() is called or a certain depth level is
 * reached.</p>
 *
 * <p>Note that instances of this class are only immutable if both parts are
 * immutable, i.e. either Strings or ConsStrings that are ultimately composed
 * of Strings.</p>
 *
 * <p></p>Both the name and the concept are borrowed from V8.</p>
 */
public class ConsString implements CharSequence {

    private CharSequence s1, s2;
    private final int length;
    private int depth;

    public ConsString(CharSequence str1, CharSequence str2) {
        s1 = str1;
        s2 = str2;
        length = str1.length() + str2.length();
        // Don't let it grow too deep, can cause stack overflows in flatten()
        depth = 1;
        if (str1 instanceof ConsString) {
            depth += ((ConsString)str1).depth;
        }
        if (str2 instanceof ConsString) {
            depth += ((ConsString)str2).depth;
        }
        if (depth > 100) {
            flatten();
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
        depth = 1;
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

    public char charAt(int index) {
        if (s1 instanceof String && s2 == "") {
            return s1.charAt(index);
        }
        synchronized (this) {
            if ((index < 0) || (index >= length)) {
                throw new StringIndexOutOfBoundsException(index);
            }
            int l1 = s1.length();
            return index >= l1 ? s2.charAt(index - l1) : s1.charAt(index);
        }
    }

    public CharSequence subSequence(int start, int end) {
        if (s1 instanceof String && s2 == "") {
            return s1.subSequence(start, end);
        }
        synchronized (this) {
            if (start == 0 && end == length) {
                return this;
            }
            int l1 = s1.length();
            if (start >= l1) {
                return s2.subSequence(start - l1, end - l1);
            } else if (end <= l1) {
                return s1.subSequence(start, end);
            } else {
                return new ConsString(
                        s1.subSequence(start, l1),
                        s2.subSequence(0, end - l1));
            }
        }
    }
}
