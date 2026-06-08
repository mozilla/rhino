/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SourceMapJsonParserTest {

    @Test
    void parsesEmptyObject() {
        Object v = SourceMapJsonParser.parse("{}");
        assertInstanceOf(Map.class, v);
        assertTrue(((Map<?, ?>) v).isEmpty());
    }

    @Test
    void parsesEmptyArray() {
        Object v = SourceMapJsonParser.parse("[]");
        assertInstanceOf(List.class, v);
        assertTrue(((List<?>) v).isEmpty());
    }

    @Test
    void parsesScalars() {
        assertEquals(Long.valueOf(0L), SourceMapJsonParser.parse("0"));
        assertEquals(Long.valueOf(3L), SourceMapJsonParser.parse("3"));
        assertEquals(Long.valueOf(-42L), SourceMapJsonParser.parse("-42"));
        assertEquals(Double.valueOf(3.0), SourceMapJsonParser.parse("3.0"));
        assertEquals(Double.valueOf(1.5e2), SourceMapJsonParser.parse("1.5e2"));
        assertEquals(Boolean.TRUE, SourceMapJsonParser.parse("true"));
        assertEquals(Boolean.FALSE, SourceMapJsonParser.parse("false"));
        assertNull(SourceMapJsonParser.parse("null"));
        assertEquals("hello", SourceMapJsonParser.parse("\"hello\""));
    }

    @Test
    void parsesStringEscapes() {
        assertEquals("a\nb", SourceMapJsonParser.parse("\"a\\nb\""));
        assertEquals("a\tb", SourceMapJsonParser.parse("\"a\\tb\""));
        assertEquals("a\"b", SourceMapJsonParser.parse("\"a\\\"b\""));
        assertEquals("a\\b", SourceMapJsonParser.parse("\"a\\\\b\""));
        assertEquals("a/b", SourceMapJsonParser.parse("\"a\\/b\""));
        assertEquals("é", SourceMapJsonParser.parse("\"\\u00e9\""));
    }

    @Test
    void parsesObjectWithMixedFields() {
        @SuppressWarnings("unchecked")
        Map<String, Object> m =
                (Map<String, Object>)
                        SourceMapJsonParser.parse(
                                "{\"version\":3,\"sources\":[\"a.js\",\"b.js\"],\"mappings\":\"AAAA\"}");
        assertEquals(Long.valueOf(3L), m.get("version"));
        assertEquals(List.of("a.js", "b.js"), m.get("sources"));
        assertEquals("AAAA", m.get("mappings"));
    }

    @Test
    void parsesNestedArrayAndObject() {
        @SuppressWarnings("unchecked")
        Map<String, Object> m =
                (Map<String, Object>) SourceMapJsonParser.parse("{\"a\":[1,2,{\"b\":\"c\"}]}");
        @SuppressWarnings("unchecked")
        List<Object> a = (List<Object>) m.get("a");
        assertEquals(Long.valueOf(1L), a.get(0));
        assertEquals(Long.valueOf(2L), a.get(1));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) a.get(2);
        assertEquals("c", nested.get("b"));
    }

    @Test
    void stripsXssiPrefix() {
        Object v = SourceMapJsonParser.parse(")]}'\n{\"version\":3}");
        assertInstanceOf(Map.class, v);
        assertEquals(Long.valueOf(3L), ((Map<?, ?>) v).get("version"));
    }

    @Test
    void stripsXssiPrefixWithoutNewline() {
        Object v = SourceMapJsonParser.parse(")]}'{\"version\":3}");
        assertInstanceOf(Map.class, v);
    }

    @Test
    void rejectsTrailingComma() {
        SourceMapException ex =
                assertThrows(
                        SourceMapException.class, () -> SourceMapJsonParser.parse("{\"a\":1,}"));
        assertTrue(ex.getMessage().contains("invalid JSON"));
    }

    @Test
    void rejectsUnquotedKey() {
        assertThrows(SourceMapException.class, () -> SourceMapJsonParser.parse("{a:1}"));
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThrows(SourceMapException.class, () -> SourceMapJsonParser.parse("{}garbage"));
    }

    @Test
    void rejectsTruncatedString() {
        assertThrows(SourceMapException.class, () -> SourceMapJsonParser.parse("\"abc"));
    }

    @Test
    void rejectsEmptyInput() {
        assertThrows(SourceMapException.class, () -> SourceMapJsonParser.parse(""));
    }

    @Test
    void parsesEmptyString() {
        assertEquals("", SourceMapJsonParser.parse("\"\""));
    }

    @Test
    void parsesNumberEdgeCases() {
        assertEquals(Long.valueOf(0L), SourceMapJsonParser.parse("0"));
        assertEquals(Long.valueOf(-0L), SourceMapJsonParser.parse("-0"));
        // Beyond long range falls through to double.
        Object big = SourceMapJsonParser.parse("99999999999999999999");
        assertInstanceOf(Double.class, big);
    }
}
