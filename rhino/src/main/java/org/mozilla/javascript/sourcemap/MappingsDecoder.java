/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes the Base64-VLQ {@code mappings} string from an ECMA-426 source map into segments grouped
 * by 0-indexed generated line.
 */
final class MappingsDecoder {

    private static final int VLQ_BASE_SHIFT = 5;
    private static final int VLQ_BASE = 1 << VLQ_BASE_SHIFT; // 32
    private static final int VLQ_BASE_MASK = VLQ_BASE - 1;
    private static final int VLQ_CONTINUATION_BIT = VLQ_BASE;

    private static final int[] BASE64;

    static {
        BASE64 = new int[128];
        for (int i = 0; i < 128; i++) BASE64[i] = -1;
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int i = 0; i < alphabet.length(); i++) BASE64[alphabet.charAt(i)] = i;
    }

    private final String src;
    private int pos;
    private int line;

    private MappingsDecoder(String src) {
        this.src = src;
        this.pos = 0;
        this.line = 0;
    }

    static List<List<Segment>> decode(String mappings, int sourcesCount, int namesCount) {
        if (mappings == null) throw new SourceMapException("mappings is null");
        if (mappings.isEmpty()) return new ArrayList<>();

        MappingsDecoder d = new MappingsDecoder(mappings);
        List<List<Segment>> result = new ArrayList<>();
        List<Segment> currentLine = new ArrayList<>();

        int sourceIndex = 0;
        int srcLine = 0;
        int srcCol = 0;
        int nameIndex = 0;

        int genCol = 0;

        // Track comma-separated structure to detect zero-field segments.
        boolean lineStart = true;
        boolean commaJustSeen = false;

        while (d.pos < d.src.length()) {
            char c = d.src.charAt(d.pos);
            if (c == ';') {
                if (commaJustSeen) {
                    throw new SourceMapException(
                            "zero-field segment (trailing comma) at line " + d.line);
                }
                currentLine.sort((a, b) -> Integer.compare(a.getGenCol(), b.getGenCol()));
                result.add(currentLine);
                currentLine = new ArrayList<>();
                d.pos++;
                d.line++;
                genCol = 0;
                lineStart = true;
                commaJustSeen = false;
                continue;
            }
            if (c == ',') {
                if (lineStart || commaJustSeen) {
                    throw new SourceMapException("zero-field segment at line " + d.line);
                }
                d.pos++;
                commaJustSeen = true;
                lineStart = false;
                continue;
            }

            lineStart = false;
            commaJustSeen = false;

            int[] fields = new int[5];
            int count = 0;
            while (count < 5 && d.pos < d.src.length()) {
                char nc = d.src.charAt(d.pos);
                if (nc == ',' || nc == ';') break;
                fields[count++] = d.readVlq();
            }
            // Detect a 6th-field overflow: if we filled 5 but the next char is still a Base64
            // digit, the segment exceeds the spec maximum.
            if (count == 5
                    && d.pos < d.src.length()
                    && d.src.charAt(d.pos) != ','
                    && d.src.charAt(d.pos) != ';') {
                throw new SourceMapException(
                        "invalid segment length (more than 5 fields) at line " + d.line);
            }
            if (count != 1 && count != 4 && count != 5) {
                throw new SourceMapException(
                        "invalid segment length " + count + " at line " + d.line);
            }

            genCol += fields[0];
            if (genCol < 0) {
                throw new SourceMapException("negative generated column at line " + d.line);
            }

            Segment seg;
            if (count == 1) {
                seg =
                        new Segment(
                                genCol,
                                Segment.ABSENT,
                                Segment.ABSENT,
                                Segment.ABSENT,
                                Segment.ABSENT);
            } else {
                sourceIndex += fields[1];
                srcLine += fields[2];
                srcCol += fields[3];
                if (sourceIndex < 0 || sourceIndex >= sourcesCount) {
                    throw new SourceMapException(
                            "source index " + sourceIndex + " out of range at line " + d.line);
                }
                if (srcLine < 0 || srcCol < 0) {
                    throw new SourceMapException("negative source line/column at line " + d.line);
                }
                int ni = Segment.ABSENT;
                if (count == 5) {
                    nameIndex += fields[4];
                    if (nameIndex < 0 || nameIndex >= namesCount) {
                        throw new SourceMapException(
                                "name index " + nameIndex + " out of range at line " + d.line);
                    }
                    ni = nameIndex;
                }
                seg = new Segment(genCol, sourceIndex, srcLine, srcCol, ni);
            }
            currentLine.add(seg);
        }
        if (commaJustSeen) {
            throw new SourceMapException("zero-field segment (trailing comma) at line " + d.line);
        }
        currentLine.sort((a, b) -> Integer.compare(a.getGenCol(), b.getGenCol()));
        result.add(currentLine);
        return result;
    }

    private int readVlq() {
        long result = 0L;
        int shift = 0;
        boolean continuation;
        do {
            if (pos >= src.length()) {
                throw new SourceMapException("truncated VLQ at line " + line);
            }
            char c = src.charAt(pos++);
            int digit = (c < BASE64.length) ? BASE64[c] : -1;
            if (digit < 0) {
                throw new SourceMapException(
                        "invalid Base64 character '" + c + "' at line " + line);
            }
            if (shift >= 64) {
                throw new SourceMapException(
                        "VLQ overflow (too many continuation bytes) at line " + line);
            }
            continuation = (digit & VLQ_CONTINUATION_BIT) != 0;
            long chunk = digit & VLQ_BASE_MASK;
            result |= chunk << shift;
            shift += VLQ_BASE_SHIFT;
        } while (continuation);
        boolean negative = (result & 1) != 0;
        result >>>= 1;
        if (result > Integer.MAX_VALUE) {
            throw new SourceMapException("VLQ magnitude exceeds 32 bits at line " + line);
        }
        int magnitude = (int) result;
        return negative ? -magnitude : magnitude;
    }
}
