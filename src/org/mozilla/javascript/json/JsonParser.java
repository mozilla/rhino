/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.json;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * This class converts a stream of JSON tokens into a JSON value.
 *
 * See ECMA 15.12.
 * @author Raphael Speyer
 * @author Hannes Wallnoefer
 */
public class JsonParser {

    private Context cx;
    private Scriptable scope;

    private int pos;
    private int length;
    private String src;

    public JsonParser(Context cx, Scriptable scope) {
        this.cx = cx;
        this.scope = scope;
    }

    public synchronized Object parseValue(String json) throws ParseException {
        if (json == null) {
            throw new ParseException("Input string may not be null");
        }
        pos = 0;
        length = json.length();
        src = json;
        Object value = readValue();
        consumeWhitespace();
        if (pos < length) {
            throw new ParseException("Expected end of stream at char " + pos);
        }
        return value;
    }

    private Object readValue() throws ParseException {
        consumeWhitespace();
        while (pos < length) {
            char c = src.charAt(pos++);
            switch (c) {
                case '{':
                    return readObject();
                case '[':
                    return readArray();
                case 't':
                    return readTrue();
                case 'f':
                    return readFalse();
                case '"':
                    return readString();
                case 'n':
                    return readNull();
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                case '-':
                    return readNumber(c);
                default:
                    throw new ParseException("Unexpected token: " + c);
            }
        }
        throw new ParseException("Empty JSON string");
    }

    private Object readObject() throws ParseException {
        Scriptable object = cx.newObject(scope);
        String id;
        Object value;
        boolean needsComma = false;
        consumeWhitespace();
        while (pos < length) {
            char c = src.charAt(pos++);
            switch(c) {
                case '}':
                    return object;
                case ',':
                    if (!needsComma) {
                        throw new ParseException("Unexpected comma in object literal");
                    }
                    needsComma = false;
                    break;
                case '"':
                    if (needsComma) {
                        throw new ParseException("Missing comma in object literal");
                    }
                    id = readString();
                    consume(':');
                    value = readValue();

                    long index = ScriptRuntime.indexFromString(id);
                    if (index < 0) {
                      object.put(id, object, value);
                    } else {
                      object.put((int)index, object, value);
                    }

                    needsComma = true;
                    break;
                default:
                    throw new ParseException("Unexpected token in object literal");
            }
            consumeWhitespace();
        }
        throw new ParseException("Unterminated object literal");
    }

    private Object readArray() throws ParseException {
        List<Object> list = new ArrayList<Object>();
        boolean needsComma = false;
        consumeWhitespace();
        while (pos < length) {
            char c = src.charAt(pos);
            switch(c) {
                case ']':
                    pos += 1;
                    return cx.newArray(scope, list.toArray());
                case ',':
                    if (!needsComma) {
                        throw new ParseException("Unexpected comma in array literal");
                    }
                    needsComma = false;
                    pos += 1;
                    break;
                default:
                    if (needsComma) {
                        throw new ParseException("Missing comma in array literal");
                    }
                    list.add(readValue());
                    needsComma = true;
            }
            consumeWhitespace();
        }
        throw new ParseException("Unterminated array literal");
    }

    private String readString() throws ParseException {
        StringBuilder b = new StringBuilder();
        while (pos < length) {
            char c = src.charAt(pos++);
            if (c <= '\u001F') {
                throw new ParseException("String contains control character");
            }
            switch(c) {
                case '\\':
                    if (pos >= length) {
                        throw new ParseException("Unterminated string");
                    }
                    c = src.charAt(pos++);
                    switch (c) {
                        case '"':
                            b.append('"');
                            break;
                        case '\\':
                            b.append('\\');
                            break;
                        case '/':
                            b.append('/');
                            break;
                        case 'b':
                            b.append('\b');
                            break;
                        case 'f':
                            b.append('\f');
                            break;
                        case 'n':
                            b.append('\n');
                            break;
                        case 'r':
                            b.append('\r');
                            break;
                        case 't':
                            b.append('\t');
                            break;
                        case 'u':
                            if (length - pos < 5) {
                                throw new ParseException("Invalid character code: \\u" + src.substring(pos));
                            }
                            try {
                                b.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                                pos += 4;
                            } catch (NumberFormatException nfx) {
                                throw new ParseException("Invalid character code: " + src.substring(pos, pos + 4));
                            }
                            break;
                        default:
                            throw new ParseException("Unexcpected character in string: '\\" + c + "'");
                    }
                    break;
                case '"':
                    return b.toString();
                default:
                    b.append(c);
                    break;
            }
        }
        throw new ParseException("Unterminated string literal");
    }

    private Number readNumber(char first) throws ParseException {
        StringBuilder b = new StringBuilder();
        b.append(first);
        while (pos < length) {
            char c = src.charAt(pos);
            if (!Character.isDigit(c)
                    && c != '-'
                    && c != '+'
                    && c != '.'
                    && c != 'e'
                    && c != 'E') {
                break;
            }
            pos += 1;
            b.append(c);
        }
        String num = b.toString();
        int numLength = num.length();
        try {
            // check for leading zeroes
            for (int i = 0; i < numLength; i++) {
                char c = num.charAt(i);
                if (Character.isDigit(c)) {
                    if (c == '0'
                            && numLength > i + 1
                            && Character.isDigit(num.charAt(i + 1))) {
                        throw new ParseException("Unsupported number format: " + num);
                    }
                    break;
                }
            }
            final double dval = Double.parseDouble(num);
            final int ival = (int)dval;
            if (ival == dval) {
                return Integer.valueOf(ival);
            } else {
                return Double.valueOf(dval);
            }
        } catch (NumberFormatException nfe) {
            throw new ParseException("Unsupported number format: " + num);
        }
    }

    private Boolean readTrue() throws ParseException {
        if (length - pos < 3
                || src.charAt(pos) != 'r'
                || src.charAt(pos + 1) != 'u'
                || src.charAt(pos + 2) != 'e') {
            throw new ParseException("Unexpected token: t");
        }
        pos += 3;
        return Boolean.TRUE;
    }

    private Boolean readFalse() throws ParseException {
        if (length - pos < 4
                || src.charAt(pos) != 'a'
                || src.charAt(pos + 1) != 'l'
                || src.charAt(pos + 2) != 's'
                || src.charAt(pos + 3) != 'e') {
            throw new ParseException("Unexpected token: f");
        }
        pos += 4;
        return Boolean.FALSE;
    }

    private Object readNull() throws ParseException {
        if (length - pos < 3
                || src.charAt(pos) != 'u'
                || src.charAt(pos + 1) != 'l'
                || src.charAt(pos + 2) != 'l') {
            throw new ParseException("Unexpected token: n");
        }
        pos += 3;
        return null;
    }

    private void consumeWhitespace() {
        while (pos < length) {
            char c = src.charAt(pos);
            switch (c) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    pos += 1;
                    break;
                default:
                    return;
            }
        }
    }

    private void consume(char token) throws ParseException {
        consumeWhitespace();
        if (pos >= length) {
            throw new ParseException("Expected " + token + " but reached end of stream");
        }
        char c = src.charAt(pos++);
        if (c == token) {
            return;
        } else {
            throw new ParseException("Expected " + token + " found " + c);
        }
    }

    public static class ParseException extends Exception {
        
        static final long serialVersionUID = 4804542791749920772L;
        
        ParseException(String message) {
            super(message);
        }

        ParseException(Exception cause) {
            super(cause);
        }
    }

}
