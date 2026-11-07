/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.config.RhinoConfig;

/**
 * Strict JSON parser tailored to the source-map shape. Returns generic Java values: {@link Map},
 * {@link List}, {@link String}, {@link Long} or {@link Double}, {@link Boolean}, or {@code null}.
 *
 * <p>Strips the XSSI prefix {@code )]}'} per ECMA-426 §3.2 if present. Throws {@link
 * SourceMapException} on any spec violation.
 */
final class SourceMapJsonParser {

    /** Maximum nesting of objects and arrays, set via {@code rhino.sourcemap.maxJsonDepth}. */
    private static final int MAX_DEPTH = RhinoConfig.get("rhino.sourcemap.maxJsonDepth", 512);

    private final String src;
    private final int maxDepth;
    private int pos;
    private int depth;

    private SourceMapJsonParser(String src, int maxDepth) {
        this.src = src;
        this.maxDepth = maxDepth;
        this.pos = 0;
    }

    static Object parse(String json) {
        return parse(json, MAX_DEPTH);
    }

    static Object parse(String json, int maxDepth) {
        if (json == null) throw new SourceMapException("invalid JSON: input is null");
        String stripped = stripXssiPrefix(json);
        SourceMapJsonParser p = new SourceMapJsonParser(stripped, maxDepth);
        p.skipWhitespace();
        if (p.pos >= p.src.length()) {
            throw new SourceMapException("invalid JSON: empty input");
        }
        Object value = p.readValue();
        p.skipWhitespace();
        if (p.pos < p.src.length()) {
            throw p.error("trailing garbage");
        }
        return value;
    }

    private static String stripXssiPrefix(String s) {
        if (s.startsWith(")]}'")) {
            int i = 4;
            if (i < s.length() && (s.charAt(i) == '\n' || s.charAt(i) == '\r')) i++;
            return s.substring(i);
        }
        return s;
    }

    private Object readValue() {
        skipWhitespace();
        if (pos >= src.length()) throw error("unexpected end of input");
        char c = src.charAt(pos);
        switch (c) {
            case '{':
            case '[':
                {
                    if (++depth > maxDepth) throw error("nesting too deep");
                    Object value = c == '{' ? readObject() : readArray();
                    depth--;
                    return value;
                }
            case '"':
                return readString();
            case 't':
            case 'f':
                return readBoolean();
            case 'n':
                return readNull();
            default:
                if (c == '-' || (c >= '0' && c <= '9')) return readNumber();
                throw error("unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') throw error("expected string key");
            String key = readString();
            skipWhitespace();
            expect(':');
            Object value = readValue();
            result.put(key, value);
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
                skipWhitespace();
                if (peek() == '}') throw error("trailing comma");
            } else if (c == '}') {
                pos++;
                return result;
            } else {
                throw error("expected ',' or '}'");
            }
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            Object value = readValue();
            result.add(value);
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
                skipWhitespace();
                if (peek() == ']') throw error("trailing comma");
            } else if (c == ']') {
                pos++;
                return result;
            } else {
                throw error("expected ',' or ']'");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos >= src.length()) throw error("unterminated escape");
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 > src.length()) throw error("truncated \\u escape");
                        int code = 0;
                        for (int k = 0; k < 4; k++) {
                            int h = hexValue(src.charAt(pos + k));
                            if (h < 0) throw error("invalid \\u escape");
                            code = (code << 4) | h;
                        }
                        sb.append((char) code);
                        pos += 4;
                        break;
                    default:
                        throw error("invalid escape '\\" + esc + "'");
                }
            } else if (c < 0x20) {
                throw error("control character in string");
            } else {
                sb.append(c);
            }
        }
        throw error("unterminated string");
    }

    private Object readNumber() {
        int start = pos;
        boolean isFloat = false;
        if (peek() == '-') pos++;
        if (pos < src.length() && src.charAt(pos) == '0') {
            pos++;
        } else if (skipDigits() == 0) {
            throw error("invalid number");
        }
        if (pos < src.length() && src.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            if (skipDigits() == 0) throw error("invalid number");
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
            if (skipDigits() == 0) throw error("invalid number");
        }
        String text = src.substring(start, pos);
        if (isFloat) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                throw error("invalid number '" + text + "'");
            }
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e2) {
                throw error("invalid number '" + text + "'");
            }
        }
    }

    private int skipDigits() {
        int start = pos;
        while (pos < src.length() && src.charAt(pos) >= '0' && src.charAt(pos) <= '9') pos++;
        return pos - start;
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    private Boolean readBoolean() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw error("expected boolean");
    }

    private Object readNull() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw error("expected null");
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) throw error("expected '" + c + "'");
        pos++;
    }

    private char peek() {
        if (pos >= src.length()) throw error("unexpected end of input");
        return src.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
            else break;
        }
    }

    private SourceMapException error(String reason) {
        return new SourceMapException("invalid JSON: " + reason + " at offset " + pos);
    }
}
