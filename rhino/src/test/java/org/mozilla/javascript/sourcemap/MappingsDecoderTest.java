/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MappingsDecoderTest {

    @Test
    void emptyMappings() {
        List<List<Segment>> r = MappingsDecoder.decode("", 0, 0);
        assertTrue(r.isEmpty());
    }

    @Test
    void singleSemicolonProducesTwoEmptyLines() {
        List<List<Segment>> r = MappingsDecoder.decode(";", 0, 0);
        assertEquals(2, r.size());
        assertTrue(r.get(0).isEmpty());
        assertTrue(r.get(1).isEmpty());
    }

    @Test
    void doubleSemicolonProducesThreeEmptyLines() {
        List<List<Segment>> r = MappingsDecoder.decode(";;", 0, 0);
        assertEquals(3, r.size());
        for (List<Segment> line : r) assertTrue(line.isEmpty());
    }

    @Test
    void oneFieldSegment() {
        // "A" decodes to 0; one-field segment is a gap marker (no source info).
        List<List<Segment>> r = MappingsDecoder.decode("A", 0, 0);
        assertEquals(1, r.size());
        assertEquals(1, r.get(0).size());
        Segment s = r.get(0).get(0);
        assertEquals(0, s.genCol());
        assertEquals(Segment.ABSENT, s.sourceIndex());
    }

    @Test
    void fourFieldSegment() {
        // "AAAA" — genCol=0, sourceIndex=0, srcLine=0, srcCol=0.
        List<List<Segment>> r = MappingsDecoder.decode("AAAA", 1, 0);
        Segment s = r.get(0).get(0);
        assertEquals(0, s.genCol());
        assertEquals(0, s.sourceIndex());
        assertEquals(0, s.srcLine());
        assertEquals(0, s.srcCol());
        assertEquals(Segment.ABSENT, s.nameIndex());
    }

    @Test
    void fiveFieldSegment() {
        // "AAAAA" — five zeros.
        List<List<Segment>> r = MappingsDecoder.decode("AAAAA", 1, 1);
        Segment s = r.get(0).get(0);
        assertEquals(0, s.nameIndex());
    }

    @Test
    void twoSegmentsOnOneLine() {
        // "AAAA,CAEA": first (0,0,0,0), second deltas (+1, 0, +2, 0) → (1,0,2,0)
        List<List<Segment>> r = MappingsDecoder.decode("AAAA,CAEA", 1, 0);
        assertEquals(1, r.size());
        assertEquals(2, r.get(0).size());
        Segment a = r.get(0).get(0);
        Segment b = r.get(0).get(1);
        assertEquals(0, a.genCol());
        assertEquals(1, b.genCol());
        assertEquals(2, b.srcLine());
    }

    @Test
    void deltasResetOnNewLineForGenColOnly() {
        // Line 0: "AAAA" → (genCol=0, sourceIndex=0, srcLine=0, srcCol=0).
        // Line 1: "AACA" → genCol resets to 0; deltas (0,0,+1,0) accumulate so
        //   sourceIndex=0, srcLine=0+1=1, srcCol=0.
        List<List<Segment>> r = MappingsDecoder.decode("AAAA;AACA", 1, 0);
        assertEquals(2, r.size());
        Segment l1 = r.get(1).get(0);
        assertEquals(0, l1.genCol());
        assertEquals(1, l1.srcLine());
    }

    @Test
    void negativeDelta() {
        // First segment "AACA": (genCol=0, srcIdx=0, srcLine=1, srcCol=0).
        //   ('A'=0, 'A'=0, 'C'=2 → magnitude 1 sign 0 → +1, 'A'=0)
        // Second segment "CADA": (genCol=1, srcIdx=0, srcLine=0, srcCol=0).
        //   ('C'=2 → +1 delta, 'A'=0, 'D'=3 → magnitude 1 sign 1 → -1 delta, 'A'=0)
        List<List<Segment>> r = MappingsDecoder.decode("AACA,CADA", 1, 0);
        Segment s = r.get(0).get(1);
        assertEquals(0, s.srcLine());
    }

    @Test
    void rejectsInvalidSegmentLength_TwoFields() {
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("AA", 1, 0));
    }

    @Test
    void rejectsInvalidSegmentLength_ThreeFields() {
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("AAA", 1, 0));
    }

    @Test
    void rejectsInvalidSegmentLength_SixFields() {
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("AAAAAA", 1, 1));
    }

    @Test
    void rejectsSourceIndexOutOfRange() {
        // sourcesCount is 1 → only index 0 valid. "AACA" would use sourceIndex=0 (first segment)
        // and then "CCAA" deltas srcIndex by +1 → index 1 → invalid.
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("AAAA,CCAA", 1, 0));
    }

    @Test
    void rejectsNameIndexOutOfRange() {
        // sourcesCount=1, namesCount=0 → any nameIndex is invalid.
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("AAAAA", 1, 0));
    }

    @Test
    void outOfOrderSegmentsAreSortedByGenCol() {
        // "CAAA,DAAA": first genCol=+1, second delta=-1 → genCol=0. ECMA-426 allows out-of-order
        // segments; decoder must sort them ascending.
        List<List<Segment>> r = MappingsDecoder.decode("CAAA,DAAA", 1, 0);
        assertEquals(2, r.get(0).size());
        assertEquals(0, r.get(0).get(0).genCol());
        assertEquals(1, r.get(0).get(1).genCol());
    }

    @Test
    void rejectsZeroFieldSegment() {
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode(",", 0, 0));
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode(",,,,", 0, 0));
    }

    @Test
    void rejectsVlqOverflow() {
        // "ggggggE" decodes to a magnitude of 2^31 which exceeds Integer.MAX_VALUE.
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("ggggggE", 0, 0));
    }

    @Test
    void rejectsInvalidBase64Char() {
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("@", 0, 0));
    }
}
