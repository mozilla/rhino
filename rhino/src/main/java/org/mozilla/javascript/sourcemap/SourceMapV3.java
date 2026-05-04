/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link SourceMapper} backed by a parsed ECMA-426 plain source map. Indexed (sectioned) source
 * maps are not supported; calling {@link #parse} on those will throw {@link SourceMapException}.
 */
public final class SourceMapV3 implements SourceMapper {

    private final String file; // optional, may be null
    private final List<String> sourcePaths; // resolved, may contain nulls
    private final List<String> sourcesContent; // parallel; nullable entries; may be null overall
    private final List<String> ignoreList; // resolved paths
    private final List<String> names;
    private final List<List<Segment>> segmentsByLine;
    private final Map<String, List<String>> lineCache = new HashMap<>();

    private SourceMapV3(
            String file,
            List<String> sourcePaths,
            List<String> sourcesContent,
            List<String> ignoreList,
            List<String> names,
            List<List<Segment>> segmentsByLine) {
        this.file = file;
        this.sourcePaths = sourcePaths;
        this.sourcesContent = sourcesContent;
        this.ignoreList = ignoreList;
        this.names = names;
        this.segmentsByLine = segmentsByLine;
    }

    public static SourceMapV3 parse(String json) {
        Object root = SourceMapJsonParser.parse(json);
        if (!(root instanceof Map)) {
            throw new SourceMapException("source map must be a JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;

        validateVersion(obj.get("version"));

        if (obj.containsKey("sections")) {
            throw new SourceMapException("indexed source maps are not supported");
        }

        String file = optionalString(obj, "file");
        String sourceRoot = optionalString(obj, "sourceRoot");
        List<String> rawSources = requiredStringOrNullArray(obj, "sources");
        List<String> sourcesContent = optionalStringOrNullArray(obj, "sourcesContent");
        if (sourcesContent != null && sourcesContent.size() != rawSources.size()) {
            throw new SourceMapException("sourcesContent length does not match sources length");
        }
        List<String> names = optionalStringOrNullArray(obj, "names");
        if (names == null) names = Collections.emptyList();

        String mappings = requiredString(obj, "mappings");

        List<String> sourcePaths = resolveSourceRoot(sourceRoot, rawSources);

        List<Long> ignoreIndices = optionalLongArray(obj, "ignoreList");
        List<String> ignoreList;
        if (ignoreIndices == null) {
            ignoreList = Collections.emptyList();
        } else {
            ignoreList = new ArrayList<>(ignoreIndices.size());
            for (Long idx : ignoreIndices) {
                int i = idx.intValue();
                if (i < 0 || i >= sourcePaths.size()) {
                    throw new SourceMapException("ignoreList index " + i + " out of range");
                }
                ignoreList.add(sourcePaths.get(i));
            }
        }

        List<List<Segment>> segments =
                MappingsDecoder.decode(mappings, sourcePaths.size(), names.size());

        return new SourceMapV3(file, sourcePaths, sourcesContent, ignoreList, names, segments);
    }

    public static SourceMapV3 parse(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) >= 0) sb.append(buf, 0, n);
        return parse(sb.toString());
    }

    public static SourceMapV3 parseFile(Path path) throws IOException {
        return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    @Override
    public Position mapPosition(int targetLine, int targetColumn) {
        if (targetLine < 1 || targetColumn < 1) return null;
        int lineIdx = targetLine - 1;
        if (lineIdx >= segmentsByLine.size()) return null;
        List<Segment> segs = segmentsByLine.get(lineIdx);
        if (segs.isEmpty()) return null;
        int targetGenCol = targetColumn - 1;
        Segment found = findLargestGenColLE(segs, targetGenCol);
        if (found == null) return null;
        if (!found.hasSource()) return null;
        String path = sourcePaths.get(found.sourceIndex());
        return new Position(path, found.srcLine() + 1, found.srcCol() + 1);
    }

    /**
     * Returns the original name for the symbol at the given target position, or {@code null} if the
     * segment has no name or the position is unmapped.
     *
     * @param targetLine 1-indexed line in the transpiled source
     * @param targetColumn 1-indexed column in the transpiled source
     */
    public String getMappedName(int targetLine, int targetColumn) {
        if (targetLine < 1 || targetColumn < 1) return null;
        int lineIdx = targetLine - 1;
        if (lineIdx >= segmentsByLine.size()) return null;
        List<Segment> segs = segmentsByLine.get(lineIdx);
        if (segs.isEmpty()) return null;
        Segment found = findLargestGenColLE(segs, targetColumn - 1);
        if (found == null || found.nameIndex() == Segment.ABSENT) return null;
        return names.get(found.nameIndex());
    }

    private static Segment findLargestGenColLE(List<Segment> segs, int target) {
        int lo = 0;
        int hi = segs.size() - 1;
        int best = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int gc = segs.get(mid).genCol();
            if (gc <= target) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best < 0 ? null : segs.get(best);
    }

    @Override
    public String getSourceLineText(String sourcePath, int lineNumber) {
        if (sourcePath == null || lineNumber < 1) return null;
        if (sourcesContent == null) return null;
        List<String> lines = lineCache.computeIfAbsent(sourcePath, this::splitSourceLines);
        if (lines == null) return null;
        if (lineNumber > lines.size()) return null;
        return lines.get(lineNumber - 1);
    }

    @Override
    public String getPrimarySourceContent() {
        if (sourcesContent == null || sourcesContent.isEmpty()) return null;
        return sourcesContent.get(0);
    }

    private List<String> splitSourceLines(String sourcePath) {
        int idx = sourcePaths.indexOf(sourcePath);
        if (idx < 0) return null;
        if (sourcesContent == null) return null;
        String content = sourcesContent.get(idx);
        if (content == null) return null;
        // Split on CRLF, LF, or lone CR.
        List<String> out = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '\r' || c == '\n') {
                out.add(content.substring(start, i));
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                i++;
                start = i;
            } else {
                i++;
            }
        }
        if (start < content.length()) out.add(content.substring(start));
        return out;
    }

    public List<String> sources() {
        return Collections.unmodifiableList(sourcePaths);
    }

    public List<String> ignoreList() {
        return Collections.unmodifiableList(ignoreList);
    }

    public String file() {
        return file;
    }

    // -- helpers --------------------------------------------------------

    private static void validateVersion(Object v) {
        if (v == null) throw new SourceMapException("missing required field 'version'");
        long ver;
        if (v instanceof Long) {
            ver = (Long) v;
        } else if (v instanceof Double) {
            double d = (Double) v;
            if (d != Math.floor(d) || Double.isInfinite(d)) {
                throw new SourceMapException("unsupported source map version: " + v);
            }
            ver = (long) d;
        } else {
            throw new SourceMapException("unsupported source map version: " + v);
        }
        if (ver != 3) {
            throw new SourceMapException("unsupported source map version: " + ver);
        }
    }

    private static String optionalString(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v == null) return null;
        if (!(v instanceof String)) {
            throw new SourceMapException(
                    "field '" + key + "' must be a string, got " + typeName(v));
        }
        return (String) v;
    }

    private static String requiredString(Map<String, Object> obj, String key) {
        if (!obj.containsKey(key)) {
            throw new SourceMapException("missing required field '" + key + "'");
        }
        Object v = obj.get(key);
        if (!(v instanceof String)) {
            throw new SourceMapException(
                    "field '" + key + "' must be a string, got " + typeName(v));
        }
        return (String) v;
    }

    private static List<String> requiredStringOrNullArray(Map<String, Object> obj, String key) {
        if (!obj.containsKey(key)) {
            throw new SourceMapException("missing required field '" + key + "'");
        }
        Object v = obj.get(key);
        if (!(v instanceof List)) {
            throw new SourceMapException(
                    "field '" + key + "' must be an array, got " + typeName(v));
        }
        List<?> raw = (List<?>) v;
        List<String> out = new ArrayList<>(raw.size());
        for (Object e : raw) {
            if (e != null && !(e instanceof String)) {
                throw new SourceMapException("field '" + key + "' must be an array of strings");
            }
            out.add((String) e);
        }
        return out;
    }

    private static List<String> optionalStringOrNullArray(Map<String, Object> obj, String key) {
        if (!obj.containsKey(key) || obj.get(key) == null) return null;
        return requiredStringOrNullArray(obj, key);
    }

    private static List<Long> optionalLongArray(Map<String, Object> obj, String key) {
        if (!obj.containsKey(key) || obj.get(key) == null) return null;
        Object v = obj.get(key);
        if (!(v instanceof List)) {
            throw new SourceMapException(
                    "field '" + key + "' must be an array, got " + typeName(v));
        }
        List<?> raw = (List<?>) v;
        List<Long> out = new ArrayList<>(raw.size());
        for (Object e : raw) {
            if (!(e instanceof Long)) {
                throw new SourceMapException("field '" + key + "' must be an array of integers");
            }
            out.add((Long) e);
        }
        return out;
    }

    private static List<String> resolveSourceRoot(String sourceRoot, List<String> rawSources) {
        if (sourceRoot == null || sourceRoot.isEmpty()) return rawSources;
        String prefix = sourceRoot.endsWith("/") ? sourceRoot : sourceRoot + "/";
        List<String> out = new ArrayList<>(rawSources.size());
        for (String s : rawSources) {
            out.add(s == null ? null : prefix + s);
        }
        return out;
    }

    private static String typeName(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return "string";
        if (v instanceof Long || v instanceof Double) return "number";
        if (v instanceof Boolean) return "boolean";
        if (v instanceof List) return "array";
        if (v instanceof Map) return "object";
        return v.getClass().getSimpleName();
    }
}
