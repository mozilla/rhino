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
}
