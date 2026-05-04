/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SourceMapV3Test {

    @Test
    void parsesMinimalValidMap() {
        SourceMapV3 m =
                SourceMapV3.parse("{\"version\":3,\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("a.js"), m.sources());
        assertTrue(m.ignoreList().isEmpty());
        assertNull(m.file());
    }

    @Test
    void rejectsMissingVersion() {
        SourceMapException ex =
                assertThrows(
                        SourceMapException.class,
                        () -> SourceMapV3.parse("{\"sources\":[\"a\"],\"mappings\":\"\"}"));
        assertTrue(ex.getMessage().toLowerCase().contains("version"));
    }

    @Test
    void rejectsWrongVersion() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse("{\"version\":2,\"sources\":[\"a\"],\"mappings\":\"\"}"));
    }

    @Test
    void rejectsNonNumericVersion() {
        assertThrows(
                SourceMapException.class,
                () ->
                        SourceMapV3.parse(
                                "{\"version\":\"3\",\"sources\":[\"a\"],\"mappings\":\"\"}"));
    }

    @Test
    void acceptsVersionAsFloat() {
        SourceMapV3 m =
                SourceMapV3.parse("{\"version\":3.0,\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("a.js"), m.sources());
    }

    @Test
    void rejectsIndexedMap() {
        SourceMapException ex =
                assertThrows(
                        SourceMapException.class,
                        () -> SourceMapV3.parse("{\"version\":3,\"sections\":[]}"));
        assertTrue(ex.getMessage().toLowerCase().contains("indexed"));
    }

    @Test
    void rejectsMissingMappings() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse("{\"version\":3,\"sources\":[\"a\"]}"));
    }

    @Test
    void rejectsMissingSources() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse("{\"version\":3,\"mappings\":\"\"}"));
    }

    @Test
    void rejectsWrongSourcesType() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse("{\"version\":3,\"sources\":\"a\",\"mappings\":\"\"}"));
    }

    @Test
    void rejectsSourcesContentLengthMismatch() {
        assertThrows(
                SourceMapException.class,
                () ->
                        SourceMapV3.parse(
                                "{\"version\":3,\"sources\":[\"a\",\"b\"],"
                                        + "\"sourcesContent\":[\"x\"],\"mappings\":\"\"}"));
    }

    @Test
    void resolvesSourceRoot() {
        SourceMapV3 m =
                SourceMapV3.parse(
                        "{\"version\":3,\"sourceRoot\":\"/root\","
                                + "\"sources\":[\"a.js\",\"sub/b.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("/root/a.js", "/root/sub/b.js"), m.sources());
    }

    @Test
    void sourceRootWithTrailingSlash() {
        SourceMapV3 m =
                SourceMapV3.parse(
                        "{\"version\":3,\"sourceRoot\":\"/root/\","
                                + "\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("/root/a.js"), m.sources());
    }

    @Test
    void emptySourceRootIsNoOp() {
        SourceMapV3 m =
                SourceMapV3.parse(
                        "{\"version\":3,\"sourceRoot\":\"\","
                                + "\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("a.js"), m.sources());
    }

    @Test
    void resolvesIgnoreListIndices() {
        SourceMapV3 m =
                SourceMapV3.parse(
                        "{\"version\":3,\"sourceRoot\":\"/r\","
                                + "\"sources\":[\"a.js\",\"b.js\",\"c.js\"],"
                                + "\"ignoreList\":[0,2],\"mappings\":\"\"}");
        assertEquals(List.of("/r/a.js", "/r/c.js"), m.ignoreList());
    }

    @Test
    void rejectsIgnoreListOutOfRange() {
        assertThrows(
                SourceMapException.class,
                () ->
                        SourceMapV3.parse(
                                "{\"version\":3,\"sources\":[\"a\"],"
                                        + "\"ignoreList\":[5],\"mappings\":\"\"}"));
    }

    @Test
    void exposesFile() {
        SourceMapV3 m =
                SourceMapV3.parse(
                        "{\"version\":3,\"file\":\"out.js\","
                                + "\"sources\":[\"a\"],\"mappings\":\"\"}");
        assertEquals("out.js", m.file());
    }

    // mappings "AAAA;AACC" decoded:
    //   line 0: [seg(genCol=0, srcIdx=0, srcLine=0, srcCol=0)]
    //   line 1: [seg(genCol=0, srcIdx=0, srcLine=1, srcCol=1)]
    // (srcIdx/srcLine/srcCol persist across line breaks; only genCol resets.)
    private static final String SIMPLE_MAP =
            "{\"version\":3,\"sources\":[\"orig.js\"],"
                    + "\"sourcesContent\":[\"line one\\nline two\\n\"],"
                    + "\"mappings\":\"AAAA;AACC\"}";

    @Test
    void mapPositionReturnsExpectedSegment() {
        SourceMapV3 m = SourceMapV3.parse(SIMPLE_MAP);
        Position p = m.mapPosition(1, 1);
        assertEquals(new Position("orig.js", 1, 1), p);
    }

    @Test
    void mapPositionLine2() {
        SourceMapV3 m = SourceMapV3.parse(SIMPLE_MAP);
        Position p = m.mapPosition(2, 1);
        assertEquals(new Position("orig.js", 2, 2), p);
    }

    @Test
    void mapPositionUsesLargestGenColLessThanOrEqual() {
        // Two segments on line 1: at genCol 0 and 5.
        // mappings "AAAA,KAAA" → segment 1 genCol=5 (K=10, with sign bit → 5).
        SourceMapV3 m =
                SourceMapV3.parse(
                        "{\"version\":3,\"sources\":[\"o.js\"],\"mappings\":\"AAAA,KAAA\"}");
        // genCol 5 → second segment
        Position p1 = m.mapPosition(1, 6); // 1-indexed col 6 → 0-indexed col 5
        assertEquals(new Position("o.js", 1, 1), p1);
        // genCol 4 (1-indexed col 5) → first segment
        Position p2 = m.mapPosition(1, 5);
        assertEquals(new Position("o.js", 1, 1), p2);
        // genCol 0 (1-indexed col 1) → first segment
        Position p3 = m.mapPosition(1, 1);
        assertEquals(new Position("o.js", 1, 1), p3);
    }

    @Test
    void mapPositionReturnsNullForOutOfRangeLine() {
        SourceMapV3 m = SourceMapV3.parse(SIMPLE_MAP);
        assertNull(m.mapPosition(99, 1));
        assertNull(m.mapPosition(0, 1));
    }

    @Test
    void mapPositionReturnsNullForColumnBeforeFirstSegment() {
        // Single-segment map with first segment at genCol=2 (mapping "EAAA": genCol delta = 2).
        SourceMapV3 m =
                SourceMapV3.parse("{\"version\":3,\"sources\":[\"o.js\"],\"mappings\":\"EAAA\"}");
        // 1-indexed col 1 → 0-indexed col 0, before first segment at genCol 2 → null.
        assertNull(m.mapPosition(1, 1));
        assertNull(m.mapPosition(1, 2)); // 0-indexed col 1 still before
        // 1-indexed col 3 → 0-indexed col 2 → matches.
        Position p = m.mapPosition(1, 3);
        assertEquals(new Position("o.js", 1, 1), p);
    }

    @Test
    void mapPositionReturnsNullForOneFieldSegment() {
        // mappings "A" is a single 1-field segment at genCol=0 with no source.
        SourceMapV3 m = SourceMapV3.parse("{\"version\":3,\"sources\":[],\"mappings\":\"A\"}");
        assertNull(m.mapPosition(1, 1));
    }

    @Test
    void mapPositionReturnsNullForEmptyLine() {
        // Two lines: first has segments, second is empty (";").
        SourceMapV3 m =
                SourceMapV3.parse("{\"version\":3,\"sources\":[\"o.js\"],\"mappings\":\"AAAA;\"}");
        assertNull(m.mapPosition(2, 1));
    }
}
