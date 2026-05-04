# ECMA-426 Source Map Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real, spec-conformant `SourceMapper` implementation (`SourceMapV3`) that reads ECMA-426 plain source maps, validated by the official tc39/source-map-tests suite.

**Architecture:** A four-piece runtime split — a tiny strict JSON parser, a Base64-VLQ mappings decoder, the `SourceMapV3` aggregator (factories, validation, lookup, accessors), and a `SourceMapException` for parse errors. Plus a small refactor of the existing `Position`/`SourceMapper` interface so positions carry a source path, and a tc39-suite-driven parameterized test in the `tests` module backed by an excludelist mirroring test262.

**Tech Stack:** Java 11 (`rhino` module), JUnit 5, Gradle (gradle wrapper). No new dependencies.

**Spec:** [`docs/superpowers/specs/2026-05-04-ecma426-source-map-implementation-design.md`](../specs/2026-05-04-ecma426-source-map-implementation-design.md)

---

## File Structure

New files:
- `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapException.java` — public unchecked exception.
- `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapJsonParser.java` — package-private JSON parser.
- `rhino/src/main/java/org/mozilla/javascript/sourcemap/MappingsDecoder.java` — package-private VLQ decoder.
- `rhino/src/main/java/org/mozilla/javascript/sourcemap/Segment.java` — package-private record holding decoded segment fields.
- `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java` — public `SourceMapper` impl.
- `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapJsonParserTest.java`
- `rhino/src/test/java/org/mozilla/javascript/sourcemap/MappingsDecoderTest.java`
- `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java`
- `tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java`
- `tests/testsrc/source-map-tests-excludelist.txt`

Modified files:
- `rhino/src/main/java/org/mozilla/javascript/sourcemap/Position.java` — add `sourcePath` field.
- `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapper.java` — rename + reshape methods.
- `rhino/src/main/java/org/mozilla/javascript/Parser.java:306-320` — thread `sourcePath` through.
- `rhino/src/main/java/org/mozilla/javascript/Context.java:2683-2694` — rename `getOriginalSource` call.
- `rhino/src/main/java/org/mozilla/javascript/CodeGenerator.java:259-275` — adapt to new `Position` (line/column unchanged usage).
- `rhino/src/main/java/org/mozilla/javascript/optimizer/BodyCodegen.java:3155-3170` — same as above.
- `rhino/src/test/java/org/mozilla/javascript/SourceMapperTest.java` — adapt `TestMapper` and assertions.
- `tests/build.gradle` — wire in `updateSourceMapTestsExcludelist` system property.
- `.gitmodules` + new entry — add `tests/source-map-tests` submodule.

---

## Task 1: Refactor `Position` and `SourceMapper` interface and all consumers

**Files:**
- Modify: `rhino/src/main/java/org/mozilla/javascript/sourcemap/Position.java`
- Modify: `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapper.java`
- Modify: `rhino/src/main/java/org/mozilla/javascript/Parser.java:306-320`
- Modify: `rhino/src/main/java/org/mozilla/javascript/Context.java:2688-2694`
- Modify: `rhino/src/test/java/org/mozilla/javascript/SourceMapperTest.java`

This task is a single coupled refactor. The interface change requires touching all call sites in the same commit so that the codebase compiles between commits. The two backends (`BodyCodegen`, `CodeGenerator`) only consume `position.line()` so they don't need code changes — they recompile cleanly against the new record shape because the constructor was always invoked indirectly via the mapper.

- [ ] **Step 1: Update `Position` record**

Replace the entirety of `rhino/src/main/java/org/mozilla/javascript/sourcemap/Position.java` with:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

/**
 * A 1-indexed line and column position in a source file, along with the path of that source.
 *
 * @param sourcePath the path of the source file (may be {@code null} when the mapper has no source
 *     associated with the position)
 * @param line 1-indexed line number
 * @param column 1-indexed column number
 */
public record Position(String sourcePath, int line, int column) {}
```

- [ ] **Step 2: Update `SourceMapper` interface**

Replace the entirety of `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapper.java` with:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

/**
 * Maps positions in a transpiled (target) script back to positions in the original source. Attach
 * an instance via {@link
 * org.mozilla.javascript.ScriptCompileSpec.Builder#sourceMapper(SourceMapper)} (or the equivalent
 * on {@link org.mozilla.javascript.FunctionCompileSpec}) so that line numbers in stack traces, the
 * debugger source handoff, and parser error messages refer to the original source rather than the
 * transpiled output.
 */
public interface SourceMapper {

    /**
     * Maps a target {@code (line, column)} position to the corresponding original-source position.
     *
     * @param targetLine 1-indexed line in the transpiled source
     * @param targetColumn 1-indexed column in the transpiled source
     * @return the original-source position, or {@code null} if no mapping exists for this position
     */
    Position mapPosition(int targetLine, int targetColumn);

    /**
     * Returns the text of the given line within the named original source file. Used to populate
     * parser error messages with the offending line from the original source.
     *
     * @param sourcePath the resolved source path (as returned in {@link Position#sourcePath()})
     * @param lineNumber 1-indexed line number in the original source
     * @return the line text, or {@code null} if the source is unknown, the line is out of range,
     *     or no original-source content is available
     */
    String getSourceLineText(String sourcePath, int lineNumber);

    /**
     * Returns the full text of the primary original source so the debugger can display it during
     * compilation handoff. When the underlying source map references multiple sources, an
     * implementation must pick one (typically the first) — this method does not enumerate them.
     *
     * @return the primary original source content, or {@code null} if it is not available
     */
    String getPrimarySourceContent();
}
```

- [ ] **Step 3: Update `Parser.mapLocation`**

In `rhino/src/main/java/org/mozilla/javascript/Parser.java`, replace the body of `mapLocation` (lines 306-320) with:

```java
    private MappedLocation mapLocation(int line, String lineSource, int offset) {
        SourceMapper mapper = compilerEnv.getSourceMapper();
        if (mapper != null) {
            Position mapped = mapper.mapPosition(line, offset);
            if (mapped != null) {
                line = mapped.line();
                offset = mapped.column();
                String mappedLine = mapper.getSourceLineText(mapped.sourcePath(), line);
                if (mappedLine != null) {
                    lineSource = mappedLine;
                }
            }
        }
        return new MappedLocation(line, lineSource, offset);
    }
```

- [ ] **Step 4: Update `Context` debugger handoff**

In `rhino/src/main/java/org/mozilla/javascript/Context.java`, change line 2690:

```java
                    String original = mapper.getOriginalSource();
```

to:

```java
                    String original = mapper.getPrimarySourceContent();
```

- [ ] **Step 5: Update `SourceMapperTest.TestMapper`**

In `rhino/src/test/java/org/mozilla/javascript/SourceMapperTest.java`, replace the inner `TestMapper` class (lines 33-66) with:

```java
    /**
     * Maps target line N to source line {@code 100 + N} and column M to {@code 200 + M}, using a
     * fixed source path. Returns null for any target line in {@code skipLines} so we can exercise
     * the skip-on-null path.
     */
    private static final class TestMapper implements SourceMapper {
        private static final String SOURCE_PATH = "original.js";

        private final Set<Integer> skipLines;
        private final String originalSource;
        private final List<String> originalLines;

        TestMapper(String originalSource, Integer... skipLines) {
            this.originalSource = originalSource;
            this.originalLines =
                    originalSource == null
                            ? List.of()
                            : Arrays.asList(originalSource.split("\n", -1));
            this.skipLines = new HashSet<>(Arrays.asList(skipLines));
        }

        @Override
        public Position mapPosition(int targetLine, int targetColumn) {
            if (skipLines.contains(targetLine)) return null;
            return new Position(SOURCE_PATH, 100 + targetLine, 200 + targetColumn);
        }

        @Override
        public String getPrimarySourceContent() {
            return originalSource;
        }

        @Override
        public String getSourceLineText(String sourcePath, int lineNumber) {
            if (!SOURCE_PATH.equals(sourcePath)) return null;
            // mapPosition emits source lines starting at 101 (target line 1 → 101). Honor the
            // same offset here so the source space is internally consistent.
            int idx = lineNumber - 101;
            if (idx < 0 || idx >= originalLines.size()) return null;
            return originalLines.get(idx);
        }
    }
```

- [ ] **Step 6: Run all tests in the rhino module**

Run: `./gradlew :rhino:test`
Expected: PASS. The existing `SourceMapperTest` cases still cover the same observable behavior — only the `TestMapper` plumbing changed.

- [ ] **Step 7: Run spotless**

Run: `./gradlew spotlessApply`
Expected: no errors; possibly small formatting changes.

- [ ] **Step 8: Commit**

```bash
git add rhino/src/main/java/org/mozilla/javascript/sourcemap/Position.java \
        rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapper.java \
        rhino/src/main/java/org/mozilla/javascript/Parser.java \
        rhino/src/main/java/org/mozilla/javascript/Context.java \
        rhino/src/test/java/org/mozilla/javascript/SourceMapperTest.java
git commit -m "Refactor SourceMapper to carry source paths

Position now carries the source file path so that multi-source
maps can be represented faithfully. SourceMapper.getSourceLineText
gains a sourcePath parameter and getOriginalSource is renamed to
getPrimarySourceContent for clarity (it returns content, and is
necessarily a 'primary' choice when the map has many sources).

Consumers (Parser error reporter, Context debugger handoff) are
threaded through; both compiler backends use only line() so they
recompile unchanged."
```

---

## Task 2: Create `SourceMapException`

**Files:**
- Create: `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapException.java`

- [ ] **Step 1: Create the exception class**

Write the full file:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

/**
 * Thrown by {@link SourceMapV3#parse} (and friends) when the input is not a valid ECMA-426 source
 * map. Unchecked, matching the style of other Rhino exceptions.
 */
public class SourceMapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SourceMapException(String message) {
        super(message);
    }

    public SourceMapException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :rhino:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapException.java
git commit -m "Add SourceMapException for source-map parse errors"
```

---

## Task 3: Implement `SourceMapJsonParser`

**Files:**
- Create: `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapJsonParser.java`
- Create: `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapJsonParserTest.java`

The parser is package-private. It produces generic Java values:
- objects → `LinkedHashMap<String,Object>` (preserves field order so error messages can mention earlier-seen fields)
- arrays → `ArrayList<Object>`
- strings → `String` (with `\uXXXX`, `\\`, `\"`, `\n`, `\r`, `\t`, `\b`, `\f`, `\/` escapes)
- numbers → `Long` if no fractional/exponent part and within `long` range, else `Double`
- booleans → `Boolean`
- null → `null`

All errors throw `SourceMapException` with the offset.

- [ ] **Step 1: Write the test file**

Write `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapJsonParserTest.java`:

```java
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
        Map<String, Object> m = (Map<String, Object>) SourceMapJsonParser.parse(
                "{\"version\":3,\"sources\":[\"a.js\",\"b.js\"],\"mappings\":\"AAAA\"}");
        assertEquals(Long.valueOf(3L), m.get("version"));
        assertEquals(List.of("a.js", "b.js"), m.get("sources"));
        assertEquals("AAAA", m.get("mappings"));
    }

    @Test
    void parsesNestedArrayAndObject() {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) SourceMapJsonParser.parse(
                "{\"a\":[1,2,{\"b\":\"c\"}]}");
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
        SourceMapException ex = assertThrows(
                SourceMapException.class,
                () -> SourceMapJsonParser.parse("{\"a\":1,}"));
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :rhino:test --tests SourceMapJsonParserTest`
Expected: FAIL — `SourceMapJsonParser` does not exist yet.

- [ ] **Step 3: Implement the parser**

Write `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapJsonParser.java`:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strict JSON parser tailored to the source-map shape. Returns generic Java values: {@link Map},
 * {@link List}, {@link String}, {@link Long} or {@link Double}, {@link Boolean}, or {@code null}.
 *
 * <p>Strips the XSSI prefix {@code )]}'} per ECMA-426 §3.2 if present. Throws {@link
 * SourceMapException} on any spec violation.
 */
final class SourceMapJsonParser {

    private final String src;
    private int pos;

    private SourceMapJsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    static Object parse(String json) {
        if (json == null) throw new SourceMapException("invalid JSON: input is null");
        String stripped = stripXssiPrefix(json);
        SourceMapJsonParser p = new SourceMapJsonParser(stripped);
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
                return readObject();
            case '[':
                return readArray();
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
                        int code;
                        try {
                            code = Integer.parseInt(src.substring(pos, pos + 4), 16);
                        } catch (NumberFormatException e) {
                            throw error("invalid \\u escape");
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
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        if (pos < src.length() && src.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        String text = src.substring(start, pos);
        if (text.isEmpty() || text.equals("-")) throw error("invalid number");
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :rhino:test --tests SourceMapJsonParserTest`
Expected: PASS (all 14 tests).

- [ ] **Step 5: Run spotless**

Run: `./gradlew spotlessApply`
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapJsonParser.java \
        rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapJsonParserTest.java
git commit -m "Add strict JSON parser for source maps

Tiny dedicated parser, package-private, no Rhino runtime dependency.
Returns generic Java values (Map/List/String/Long/Double/Boolean/null).
Strips the ECMA-426 XSSI prefix. Throws SourceMapException on any
spec violation."
```

---

## Task 4: Implement `Segment` record and `MappingsDecoder`

**Files:**
- Create: `rhino/src/main/java/org/mozilla/javascript/sourcemap/Segment.java`
- Create: `rhino/src/main/java/org/mozilla/javascript/sourcemap/MappingsDecoder.java`
- Create: `rhino/src/test/java/org/mozilla/javascript/sourcemap/MappingsDecoderTest.java`

VLQ recap (per ECMA-426 §4.1): Base64 alphabet `A-Za-z0-9+/`. Each digit is 6 bits. Continuation bit is the high (bit 5). Value bits are the low 5 bits per digit (with the lowest-order bit of the *first* digit being the sign bit). To decode: accumulate value bits in groups of 5 across digits with the continuation bit set; the digit without the continuation bit ends the number; the LSB of the final value is the sign.

- [ ] **Step 1: Create `Segment` record**

Write `rhino/src/main/java/org/mozilla/javascript/sourcemap/Segment.java`:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

/**
 * A decoded source-map segment. The optional fields use {@code -1} as the "absent" sentinel.
 *
 * @param genCol 0-indexed generated column
 * @param sourceIndex index into the {@code sources} array, or {@code -1} for a 1-field segment
 * @param srcLine 0-indexed source line, or {@code -1} for a 1-field segment
 * @param srcCol 0-indexed source column, or {@code -1} for a 1-field segment
 * @param nameIndex index into the {@code names} array, or {@code -1} for a 1- or 4-field segment
 */
record Segment(int genCol, int sourceIndex, int srcLine, int srcCol, int nameIndex) {

    static final int ABSENT = -1;

    boolean hasSource() {
        return sourceIndex != ABSENT;
    }
}
```

- [ ] **Step 2: Write the decoder test file**

Write `rhino/src/test/java/org/mozilla/javascript/sourcemap/MappingsDecoderTest.java`:

```java
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
        assertThrows(
                SourceMapException.class,
                () -> MappingsDecoder.decode("AAAA,CCAA", 1, 0));
    }

    @Test
    void rejectsNameIndexOutOfRange() {
        // sourcesCount=1, namesCount=0 → any nameIndex is invalid.
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("AAAAA", 1, 0));
    }

    @Test
    void rejectsDecreasingGenCol() {
        // Two segments on same line where the second has negative genCol delta resulting in lower
        // generated column than the first → rejected.
        assertThrows(
                SourceMapException.class,
                () -> MappingsDecoder.decode("CAAA,DAAA", 1, 0));
    }

    @Test
    void rejectsInvalidBase64Char() {
        assertThrows(SourceMapException.class, () -> MappingsDecoder.decode("@", 0, 0));
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :rhino:test --tests MappingsDecoderTest`
Expected: FAIL — `MappingsDecoder` does not exist.

- [ ] **Step 4: Implement the decoder**

Write `rhino/src/main/java/org/mozilla/javascript/sourcemap/MappingsDecoder.java`:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.sourcemap;

import java.util.ArrayList;
import java.util.Collections;
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
        if (mappings.isEmpty()) return Collections.emptyList();

        MappingsDecoder d = new MappingsDecoder(mappings);
        List<List<Segment>> result = new ArrayList<>();
        List<Segment> currentLine = new ArrayList<>();

        int sourceIndex = 0;
        int srcLine = 0;
        int srcCol = 0;
        int nameIndex = 0;

        int genCol = 0;
        int prevGenCol = -1;

        while (d.pos < d.src.length()) {
            char c = d.src.charAt(d.pos);
            if (c == ';') {
                result.add(currentLine);
                currentLine = new ArrayList<>();
                d.pos++;
                d.line++;
                genCol = 0;
                prevGenCol = -1;
                continue;
            }
            if (c == ',') {
                d.pos++;
                continue;
            }

            int[] fields = new int[5];
            int count = 0;
            while (count < 5 && d.pos < d.src.length()) {
                char nc = d.src.charAt(d.pos);
                if (nc == ',' || nc == ';') break;
                fields[count++] = d.readVlq();
            }
            if (count != 1 && count != 4 && count != 5) {
                throw new SourceMapException(
                        "invalid segment length " + count + " at line " + d.line);
            }

            genCol += fields[0];
            if (genCol < prevGenCol) {
                throw new SourceMapException(
                        "decreasing generated column at line " + d.line);
            }
            prevGenCol = genCol;

            Segment seg;
            if (count == 1) {
                seg = new Segment(genCol, Segment.ABSENT, Segment.ABSENT, Segment.ABSENT, Segment.ABSENT);
            } else {
                sourceIndex += fields[1];
                srcLine += fields[2];
                srcCol += fields[3];
                if (sourceIndex < 0 || sourceIndex >= sourcesCount) {
                    throw new SourceMapException(
                            "source index " + sourceIndex + " out of range at line " + d.line);
                }
                if (srcLine < 0 || srcCol < 0) {
                    throw new SourceMapException(
                            "negative source line/column at line " + d.line);
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
        result.add(currentLine);
        return result;
    }

    private int readVlq() {
        int result = 0;
        int shift = 0;
        boolean continuation;
        do {
            if (pos >= src.length()) {
                throw new SourceMapException("truncated VLQ at line " + line);
            }
            char c = src.charAt(pos++);
            int digit = (c >= 0 && c < BASE64.length) ? BASE64[c] : -1;
            if (digit < 0) {
                throw new SourceMapException(
                        "invalid Base64 character '" + c + "' at line " + line);
            }
            continuation = (digit & VLQ_CONTINUATION_BIT) != 0;
            int chunk = digit & VLQ_BASE_MASK;
            result |= chunk << shift;
            shift += VLQ_BASE_SHIFT;
        } while (continuation);
        boolean negative = (result & 1) != 0;
        result >>>= 1;
        return negative ? -result : result;
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :rhino:test --tests MappingsDecoderTest`
Expected: PASS.

- [ ] **Step 6: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 7: Commit**

```bash
git add rhino/src/main/java/org/mozilla/javascript/sourcemap/Segment.java \
        rhino/src/main/java/org/mozilla/javascript/sourcemap/MappingsDecoder.java \
        rhino/src/test/java/org/mozilla/javascript/sourcemap/MappingsDecoderTest.java
git commit -m "Add Base64-VLQ mappings decoder

Decodes the source-map mappings string into segments grouped by
generated line. Enforces 1/4/5 field segments, monotonic generated
columns within a line, and source/name indices within range."
```

---

## Task 5: Implement `SourceMapV3` parse + validate (no lookup yet)

**Files:**
- Create: `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java`
- Create: `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java`

This task focuses on parsing and validation. `mapPosition`, `getSourceLineText`, and `getPrimarySourceContent` are stubbed to throw `UnsupportedOperationException` until later tasks. The accessors `sources()`, `ignoreList()`, `file()` work because tests need them.

- [ ] **Step 1: Write the test file (validation only)**

Write `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java`:

```java
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
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("a.js"), m.sources());
        assertTrue(m.ignoreList().isEmpty());
        assertNull(m.file());
    }

    @Test
    void rejectsMissingVersion() {
        SourceMapException ex = assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse("{\"sources\":[\"a\"],\"mappings\":\"\"}"));
        assertTrue(ex.getMessage().toLowerCase().contains("version"));
    }

    @Test
    void rejectsWrongVersion() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse(
                        "{\"version\":2,\"sources\":[\"a\"],\"mappings\":\"\"}"));
    }

    @Test
    void rejectsNonNumericVersion() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse(
                        "{\"version\":\"3\",\"sources\":[\"a\"],\"mappings\":\"\"}"));
    }

    @Test
    void acceptsVersionAsFloat() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3.0,\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("a.js"), m.sources());
    }

    @Test
    void rejectsIndexedMap() {
        SourceMapException ex = assertThrows(
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
                () -> SourceMapV3.parse(
                        "{\"version\":3,\"sources\":\"a\",\"mappings\":\"\"}"));
    }

    @Test
    void rejectsSourcesContentLengthMismatch() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse(
                        "{\"version\":3,\"sources\":[\"a\",\"b\"],"
                                + "\"sourcesContent\":[\"x\"],\"mappings\":\"\"}"));
    }

    @Test
    void resolvesSourceRoot() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sourceRoot\":\"/root\","
                        + "\"sources\":[\"a.js\",\"sub/b.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("/root/a.js", "/root/sub/b.js"), m.sources());
    }

    @Test
    void sourceRootWithTrailingSlash() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sourceRoot\":\"/root/\","
                        + "\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("/root/a.js"), m.sources());
    }

    @Test
    void emptySourceRootIsNoOp() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sourceRoot\":\"\","
                        + "\"sources\":[\"a.js\"],\"mappings\":\"\"}");
        assertEquals(List.of("a.js"), m.sources());
    }

    @Test
    void resolvesIgnoreListIndices() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sourceRoot\":\"/r\","
                        + "\"sources\":[\"a.js\",\"b.js\",\"c.js\"],"
                        + "\"ignoreList\":[0,2],\"mappings\":\"\"}");
        assertEquals(List.of("/r/a.js", "/r/c.js"), m.ignoreList());
    }

    @Test
    void rejectsIgnoreListOutOfRange() {
        assertThrows(
                SourceMapException.class,
                () -> SourceMapV3.parse(
                        "{\"version\":3,\"sources\":[\"a\"],"
                                + "\"ignoreList\":[5],\"mappings\":\"\"}"));
    }

    @Test
    void exposesFile() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"file\":\"out.js\","
                        + "\"sources\":[\"a\"],\"mappings\":\"\"}");
        assertEquals("out.js", m.file());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :rhino:test --tests SourceMapV3Test`
Expected: FAIL — `SourceMapV3` does not exist.

- [ ] **Step 3: Implement `SourceMapV3` (parse/validate, no lookup)**

Write `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java`:

```java
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
 * maps are not supported; use {@link #parse} on those will throw {@link SourceMapException}.
 */
public final class SourceMapV3 implements SourceMapper {

    private final String file; // optional, may be null
    private final List<String> sourcePaths; // resolved, may contain nulls
    private final List<String> sourcesContent; // parallel; nullable; may be null overall
    private final List<String> ignoreList; // resolved paths
    private final List<List<Segment>> segmentsByLine;
    private final Map<String, List<String>> lineCache = new HashMap<>();

    private SourceMapV3(
            String file,
            List<String> sourcePaths,
            List<String> sourcesContent,
            List<String> ignoreList,
            List<List<Segment>> segmentsByLine) {
        this.file = file;
        this.sourcePaths = sourcePaths;
        this.sourcesContent = sourcesContent;
        this.ignoreList = ignoreList;
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
            throw new SourceMapException(
                    "sourcesContent length does not match sources length");
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
                    throw new SourceMapException(
                            "ignoreList index " + i + " out of range");
                }
                ignoreList.add(sourcePaths.get(i));
            }
        }

        List<List<Segment>> segments =
                MappingsDecoder.decode(mappings, sourcePaths.size(), names.size());

        return new SourceMapV3(file, sourcePaths, sourcesContent, ignoreList, segments);
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
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public String getSourceLineText(String sourcePath, int lineNumber) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public String getPrimarySourceContent() {
        throw new UnsupportedOperationException("not implemented yet");
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
        if (v instanceof Long) ver = (Long) v;
        else if (v instanceof Double) {
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
                throw new SourceMapException(
                        "field '" + key + "' must be an array of strings");
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
                throw new SourceMapException(
                        "field '" + key + "' must be an array of integers");
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :rhino:test --tests SourceMapV3Test`
Expected: PASS (16 tests).

- [ ] **Step 5: Run spotless and full rhino tests**

Run: `./gradlew spotlessApply :rhino:test`
Expected: full rhino test suite still passes.

- [ ] **Step 6: Commit**

```bash
git add rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java \
        rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java
git commit -m "Add SourceMapV3 parsing and validation

Validates top-level fields per ECMA-426, rejects indexed maps,
resolves sourceRoot and ignoreList. Lookup methods stubbed to
UnsupportedOperationException; implemented in following commits."
```

---

## Task 6: Implement `SourceMapV3.mapPosition`

**Files:**
- Modify: `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java`
- Modify: `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java`

- [ ] **Step 1: Add lookup tests**

Append to `SourceMapV3Test.java` (inside the class):

```java
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
        SourceMapV3 m = SourceMapV3.parse(
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
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"o.js\"],\"mappings\":\"EAAA\"}");
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
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[],\"mappings\":\"A\"}");
        assertNull(m.mapPosition(1, 1));
    }

    @Test
    void mapPositionReturnsNullForEmptyLine() {
        // Two lines: first has segments, second is empty (";;").
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"o.js\"],\"mappings\":\"AAAA;\"}");
        assertNull(m.mapPosition(2, 1));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :rhino:test --tests SourceMapV3Test`
Expected: FAIL — lookup methods throw `UnsupportedOperationException`.

- [ ] **Step 3: Implement `mapPosition`**

In `SourceMapV3.java`, replace the `mapPosition` stub with:

```java
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

    private static Segment findLargestGenColLE(List<Segment> segs, int target) {
        // Binary search for the largest segment whose genCol <= target.
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :rhino:test --tests SourceMapV3Test`
Expected: PASS.

- [ ] **Step 5: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 6: Commit**

```bash
git add rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java \
        rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java
git commit -m "Implement SourceMapV3.mapPosition

Per-line binary search for the largest genCol <= target. Returns
null for out-of-range lines, empty lines, columns before the first
segment, and 1-field (gap) segments."
```

---

## Task 7: Implement `getSourceLineText` and `getPrimarySourceContent`

**Files:**
- Modify: `rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java`
- Modify: `rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java`

- [ ] **Step 1: Add tests**

Append to `SourceMapV3Test.java`:

```java
    @Test
    void getSourceLineTextReturnsLineFromSourcesContent() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"orig.js\"],"
                        + "\"sourcesContent\":[\"line one\\nline two\\nline three\"],"
                        + "\"mappings\":\"\"}");
        assertEquals("line one", m.getSourceLineText("orig.js", 1));
        assertEquals("line two", m.getSourceLineText("orig.js", 2));
        assertEquals("line three", m.getSourceLineText("orig.js", 3));
    }

    @Test
    void getSourceLineTextReturnsNullForUnknownPath() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"orig.js\"],"
                        + "\"sourcesContent\":[\"x\"],\"mappings\":\"\"}");
        assertNull(m.getSourceLineText("other.js", 1));
    }

    @Test
    void getSourceLineTextReturnsNullForNullPath() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"orig.js\"],"
                        + "\"sourcesContent\":[\"x\"],\"mappings\":\"\"}");
        assertNull(m.getSourceLineText(null, 1));
    }

    @Test
    void getSourceLineTextReturnsNullForOutOfRangeLine() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"orig.js\"],"
                        + "\"sourcesContent\":[\"only\"],\"mappings\":\"\"}");
        assertNull(m.getSourceLineText("orig.js", 0));
        assertNull(m.getSourceLineText("orig.js", 99));
    }

    @Test
    void getSourceLineTextReturnsNullWhenSourcesContentMissing() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"orig.js\"],\"mappings\":\"\"}");
        assertNull(m.getSourceLineText("orig.js", 1));
    }

    @Test
    void getSourceLineTextReturnsNullWhenEntryIsNull() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"orig.js\"],"
                        + "\"sourcesContent\":[null],\"mappings\":\"\"}");
        assertNull(m.getSourceLineText("orig.js", 1));
    }

    @Test
    void getPrimarySourceContentReturnsFirstSourcesContent() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"a\",\"b\"],"
                        + "\"sourcesContent\":[\"alpha\",\"beta\"],\"mappings\":\"\"}");
        assertEquals("alpha", m.getPrimarySourceContent());
    }

    @Test
    void getPrimarySourceContentReturnsNullWhenAbsent() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"a\"],\"mappings\":\"\"}");
        assertNull(m.getPrimarySourceContent());
    }

    @Test
    void getPrimarySourceContentReturnsNullWhenFirstEntryIsNull() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"a\"],"
                        + "\"sourcesContent\":[null],\"mappings\":\"\"}");
        assertNull(m.getPrimarySourceContent());
    }

    @Test
    void getSourceLineTextHandlesCrLfAndLoneCr() {
        SourceMapV3 m = SourceMapV3.parse(
                "{\"version\":3,\"sources\":[\"o.js\"],"
                        + "\"sourcesContent\":[\"a\\r\\nb\\rc\\nd\"],\"mappings\":\"\"}");
        assertEquals("a", m.getSourceLineText("o.js", 1));
        assertEquals("b", m.getSourceLineText("o.js", 2));
        assertEquals("c", m.getSourceLineText("o.js", 3));
        assertEquals("d", m.getSourceLineText("o.js", 4));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :rhino:test --tests SourceMapV3Test`
Expected: FAIL.

- [ ] **Step 3: Implement the two methods**

In `SourceMapV3.java`, replace the `getSourceLineText` and `getPrimarySourceContent` stubs with:

```java
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
        // Split on CRLF, LF, or lone CR. Preserve trailing empty line.
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\r' || c == '\n') {
                out.add(content.substring(start, i));
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        if (start <= content.length()) out.add(content.substring(start));
        // Drop the empty trailing entry if the content ended with a newline.
        if (!out.isEmpty() && out.get(out.size() - 1).isEmpty() && content.length() > 0) {
            char last = content.charAt(content.length() - 1);
            if (last == '\n' || last == '\r') out.remove(out.size() - 1);
        }
        return out;
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :rhino:test --tests SourceMapV3Test`
Expected: PASS.

- [ ] **Step 5: Run all rhino tests**

Run: `./gradlew :rhino:test`
Expected: full PASS — `SourceMapperTest` and others unaffected.

- [ ] **Step 6: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 7: Commit**

```bash
git add rhino/src/main/java/org/mozilla/javascript/sourcemap/SourceMapV3.java \
        rhino/src/test/java/org/mozilla/javascript/sourcemap/SourceMapV3Test.java
git commit -m "Implement SourceMapV3 text accessors

getSourceLineText splits sourcesContent lazily and caches per source
path. Handles CRLF, LF, and lone CR line terminators. Returns null
for unknown paths, missing/null entries, or out-of-range lines.
getPrimarySourceContent returns sourcesContent[0] (or null)."
```

---

## Task 8: End-to-end integration test

**Files:**
- Modify: `rhino/src/test/java/org/mozilla/javascript/SourceMapperTest.java`

Demonstrate that a real `SourceMapV3` plugged into Rhino produces remapped stack-trace lines and parser error positions, exercising the same paths as the existing `TestMapper` cases. Adds confidence that our impl wires up correctly end-to-end.

- [ ] **Step 1: Add an integration test method**

Append to `SourceMapperTest.java` (inside the class, after the existing tests):

```java
    @Test
    void realSourceMapV3RemapsStackTraceLine() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    // Generated source: 1 line, "throw 'err';\n" (12 chars before newline).
                    // Source map: maps generated line 1 col 0 → orig.js line 5 col 0.
                    // VLQ: AAKA = (genCol=0, srcIdx=0, srcLine=5, srcCol=0)
                    String mapJson =
                            "{\"version\":3,\"sources\":[\"orig.js\"],"
                                    + "\"sourcesContent\":[\"line1\\nline2\\nline3\\nline4\\nthrow 'err';\"],"
                                    + "\"mappings\":\"AAKA\"}";
                    SourceMapV3 mapper = SourceMapV3.parse(mapJson);

                    Script script =
                            cx.compileScript(
                                    ScriptCompileSpec.fromSource("throw 'err';\n")
                                            .sourceName("transpiled.js")
                                            .lineno(1)
                                            .sourceMapper(mapper)
                                            .build());

                    RhinoException ex =
                            assertThrows(RhinoException.class, () -> script.exec(cx, scope, scope));
                    assertEquals(6, ex.lineNumber(),
                            "expected source line 6 (5 zero-indexed + 1)");
                    return null;
                });
    }

    @Test
    void realSourceMapV3SurfacesPrimarySourceToDebugger() {
        Utils.runWithMode(
                cx -> {
                    String original = "var primary = true;\n";
                    String mapJson =
                            "{\"version\":3,\"sources\":[\"orig.js\"],"
                                    + "\"sourcesContent\":[\"" + original.replace("\n", "\\n") + "\"],"
                                    + "\"mappings\":\"\"}";
                    SourceMapV3 mapper = SourceMapV3.parse(mapJson);

                    RecordingDebugger debugger = new RecordingDebugger();
                    cx.setDebugger(debugger, null);
                    try {
                        cx.compileScript(
                                ScriptCompileSpec.fromSource("var x = 1;\n")
                                        .sourceName("transpiled.js")
                                        .lineno(1)
                                        .sourceMapper(mapper)
                                        .build());
                    } finally {
                        cx.setDebugger(null, null);
                    }

                    assertEquals(original, debugger.sources.iterator().next());
                    return null;
                },
                true);
    }
```

The required import `import org.mozilla.javascript.sourcemap.SourceMapV3;` should be added at the top of the file.

- [ ] **Step 2: Run tests**

Run: `./gradlew :rhino:test --tests SourceMapperTest`
Expected: PASS — the new tests join the existing ones.

> NOTE: VLQ encoding `AAKA` corresponds to `genCol=0, srcIdx=0, srcLine=5, srcCol=0`. Verify by tracing: `K` is base64 index 10; sign bit 0; magnitude `10 >> 1 = 5` → +5. Other characters are `A` = 0.

- [ ] **Step 3: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 4: Commit**

```bash
git add rhino/src/test/java/org/mozilla/javascript/SourceMapperTest.java
git commit -m "Add end-to-end SourceMapV3 integration tests

Real SourceMapV3 attached to a compile, asserting the stack-trace
line is remapped to the source line and the debugger handoff
receives the primary source content."
```

---

## Task 9: Add tc39/source-map-tests submodule

**Files:**
- Modify: `.gitmodules`
- Create: `tests/source-map-tests` (submodule)

- [ ] **Step 1: Add the submodule**

Run: `git submodule add https://github.com/tc39/source-map-tests.git tests/source-map-tests`
Expected: clones the repo and adds an entry to `.gitmodules`.

- [ ] **Step 2: Verify expected files exist**

Run: `ls tests/source-map-tests/source-map-spec-tests.json tests/source-map-tests/resources/ | head -5`
Expected: file exists, resources directory has `.js`/`.js.map` files.

If the structure differs from the design assumption, **stop and update the plan** before proceeding — the test runner (Task 10) depends on these paths.

- [ ] **Step 3: Pin the submodule to the current commit**

Run: `git -C tests/source-map-tests rev-parse HEAD`
Record this SHA in the commit message.

- [ ] **Step 4: Commit**

```bash
git add .gitmodules tests/source-map-tests
git commit -m "Add tc39/source-map-tests submodule

Pinned at upstream commit <SHA-from-step-3>."
```

---

## Task 10: Implement `SourceMapSpecSuiteTest` (validity-only with excludelist)

**Files:**
- Create: `tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java`
- Create: `tests/testsrc/source-map-tests-excludelist.txt`
- Modify: `tests/build.gradle`

This task wires up the parameterized test harness: it loads the test cases, runs each through `SourceMapV3.parse(...)`, asserts `sourceMapIsValid`, and consults the excludelist. `testActions` are deferred to Tasks 11 and 12.

- [ ] **Step 1: Create the empty excludelist**

Write `tests/testsrc/source-map-tests-excludelist.txt`:

```
# Auto-generated; review diff before committing.
# One source-map-spec-tests.json case "name" per line.
```

- [ ] **Step 2: Wire up `tests/build.gradle`**

In `tests/build.gradle`, inside the `test {` block, add (near the existing `test262properties` line):

```groovy
    if (System.getProperty('updateSourceMapTestsExcludelist') != null) {
        systemProperty 'updateSourceMapTestsExcludelist', System.getProperty('updateSourceMapTestsExcludelist')
    }
```

- [ ] **Step 3: Write the test class**

Write `tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java`:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mozilla.javascript.sourcemap.SourceMapException;
import org.mozilla.javascript.sourcemap.SourceMapJsonParser;
import org.mozilla.javascript.sourcemap.SourceMapV3;

class SourceMapSpecSuiteTest {

    private static final Path SUITE_ROOT = Paths.get("source-map-tests");
    private static final Path TESTS_JSON = SUITE_ROOT.resolve("source-map-spec-tests.json");
    private static final Path RESOURCES = SUITE_ROOT.resolve("resources");
    private static final Path EXCLUDELIST = Paths.get("testsrc", "source-map-tests-excludelist.txt");

    private static final boolean UPDATE_MODE =
            System.getProperty("updateSourceMapTestsExcludelist") != null;

    private static Set<String> excludelist;
    private static final Set<String> currentlyFailing = ConcurrentHashMap.newKeySet();
    private static final Set<String> caseNames = new LinkedHashSet<>();

    @BeforeAll
    static void loadExcludelist() throws IOException {
        excludelist = new LinkedHashSet<>();
        if (Files.exists(EXCLUDELIST)) {
            for (String line : Files.readAllLines(EXCLUDELIST, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                excludelist.add(trimmed);
            }
        }
    }

    @TestFactory
    Collection<DynamicTest> specCases() throws IOException {
        if (!Files.exists(TESTS_JSON)) {
            return List.of(
                    DynamicTest.dynamicTest(
                            "missing-suite",
                            () ->
                                    fail(
                                            "tc39/source-map-tests submodule not initialized; run "
                                                    + "`git submodule update --init`")));
        }
        String json = Files.readString(TESTS_JSON, StandardCharsets.UTF_8);
        Object root = SourceMapJsonParser.parse(json);
        if (!(root instanceof List)) fail("source-map-spec-tests.json must be a JSON array");
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) root;

        List<DynamicTest> tests = new ArrayList<>();
        for (Object entry : raw) {
            if (!(entry instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) entry;
            String name = (String) obj.get("name");
            if (name == null) continue;
            caseNames.add(name);
            tests.add(DynamicTest.dynamicTest(name, () -> runCase(obj)));
        }
        return tests;
    }

    private void runCase(Map<String, Object> obj) {
        String name = (String) obj.get("name");
        String sourceMapFile = (String) obj.get("sourceMapFile");
        Boolean expectedValid = (Boolean) obj.get("sourceMapIsValid");
        boolean isOnExcludelist = excludelist.contains(name);

        try {
            assertCase(name, sourceMapFile, expectedValid, obj);
            if (isOnExcludelist && !UPDATE_MODE) {
                fail(
                        "case '"
                                + name
                                + "' is on the excludelist but currently passes — remove it.");
            }
        } catch (Throwable t) {
            currentlyFailing.add(name);
            if (UPDATE_MODE || isOnExcludelist) {
                // suppress
                return;
            }
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            if (t instanceof AssertionError) throw (AssertionError) t;
            throw new RuntimeException(t);
        }
    }

    /**
     * Override hook: subclasses (or future tasks) extend this with checkMapping/checkIgnoreList
     * logic. For Task 10, only validity is asserted.
     */
    private void assertCase(
            String name, String sourceMapFile, Boolean expectedValid, Map<String, Object> obj)
            throws IOException {
        if (sourceMapFile == null || expectedValid == null) {
            fail("case '" + name + "' is missing sourceMapFile or sourceMapIsValid");
        }
        Path mapPath = RESOURCES.resolve(sourceMapFile);
        String json = Files.readString(mapPath, StandardCharsets.UTF_8);

        SourceMapV3 parsed = null;
        SourceMapException parseError = null;
        try {
            parsed = SourceMapV3.parse(json);
        } catch (SourceMapException e) {
            parseError = e;
        }

        if (Boolean.TRUE.equals(expectedValid)) {
            if (parsed == null) {
                fail("expected valid map but parse failed: " + parseError.getMessage());
            }
        } else {
            if (parsed != null) {
                fail("expected invalid map but parse succeeded");
            }
        }
    }

    @AfterAll
    static void writeExcludelistIfRequested() throws IOException {
        if (!UPDATE_MODE) return;
        Set<String> sorted = new TreeSet<>(currentlyFailing);
        StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated; review diff before committing.\n");
        sb.append("# One source-map-spec-tests.json case \"name\" per line.\n");
        for (String n : sorted) sb.append(n).append('\n');
        Files.writeString(EXCLUDELIST, sb.toString(), StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 4: Run the suite (will likely fail many cases)**

Run: `./gradlew :tests:test --tests SourceMapSpecSuiteTest`
Expected: many failures, since the excludelist is empty.

This is fine — Task 14 will populate the excludelist via update mode.

- [ ] **Step 5: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 6: Commit**

```bash
git add tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java \
        tests/testsrc/source-map-tests-excludelist.txt \
        tests/build.gradle
git commit -m "Add tc39 source-map spec suite test (validity only)

Parameterized test driven from source-map-spec-tests.json. Asserts
sourceMapIsValid against SourceMapV3.parse outcomes. Excludelist at
tests/testsrc/source-map-tests-excludelist.txt; cases on the list
that pass cause the build to fail (keeps the list honest).
testActions support added in following commits."
```

---

## Task 11: Add `checkMapping` action support

**Files:**
- Modify: `tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java`

`checkMapping` action shape (per the suite README and observed cases):
```json
{
  "actionType": "checkMapping",
  "generatedLine": 0,
  "generatedColumn": 12,
  "originalSource": "src/index.js",
  "originalLine": 0,
  "originalColumn": 5,
  "mappingName": "foo"
}
```
All line/column values are 0-indexed in the JSON. Field `mappingName` is optional.

- [ ] **Step 1: Extend `assertCase`**

In `SourceMapSpecSuiteTest.java`, replace the `assertCase` method body (after the validity check, before its end) — i.e., after the `if (parsed != null) ...` block, add:

```java
        if (parsed != null) {
            @SuppressWarnings("unchecked")
            List<Object> actions = (List<Object>) obj.get("testActions");
            if (actions != null) {
                for (Object a : actions) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> action = (Map<String, Object>) a;
                    String type = (String) action.get("actionType");
                    if ("checkMapping".equals(type)) {
                        runCheckMapping(parsed, action);
                    } else if ("checkIgnoreList".equals(type)) {
                        // Implemented in Task 12.
                        runCheckIgnoreList(parsed, action);
                    } else {
                        fail("unsupported testActions type: " + type);
                    }
                }
            }
        }
```

Add the helper methods:

```java
    private static void runCheckMapping(SourceMapV3 parsed, Map<String, Object> action) {
        int genLine = ((Long) action.get("generatedLine")).intValue() + 1;
        int genCol = ((Long) action.get("generatedColumn")).intValue() + 1;
        org.mozilla.javascript.sourcemap.Position p = parsed.mapPosition(genLine, genCol);
        String origSource = (String) action.get("originalSource");
        Object origLine = action.get("originalLine");
        Object origCol = action.get("originalColumn");

        if (origSource == null && origLine == null && origCol == null) {
            // Action asserts that no mapping exists.
            if (p != null) {
                fail("expected no mapping at (" + genLine + "," + genCol + ") but got " + p);
            }
            return;
        }

        if (p == null) {
            fail("expected mapping at (" + genLine + "," + genCol + ") but got null");
        }
        if (origSource != null) {
            assertEquals(origSource, p.sourcePath(), "sourcePath mismatch");
        }
        if (origLine != null) {
            int expected = ((Long) origLine).intValue() + 1;
            assertEquals(expected, p.line(), "source line mismatch");
        }
        if (origCol != null) {
            int expected = ((Long) origCol).intValue() + 1;
            assertEquals(expected, p.column(), "source column mismatch");
        }
        if (action.containsKey("mappingName")) {
            // Names are not surfaced through SourceMapper in v1. Cases that assert
            // mappingName must be excludelisted — fail loudly so we notice.
            fail(
                    "checkMapping with mappingName is unsupported in v1 — excludelist this case ("
                            + action.get("mappingName")
                            + ")");
        }
    }

    private static void runCheckIgnoreList(SourceMapV3 parsed, Map<String, Object> action) {
        // Implemented in Task 12.
        fail("checkIgnoreList not yet implemented");
    }
```

- [ ] **Step 2: Run the suite**

Run: `./gradlew :tests:test --tests SourceMapSpecSuiteTest`
Expected: more cases now exercised. Failures land on the (still empty) excludelist's currently-failing pile.

- [ ] **Step 3: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 4: Commit**

```bash
git add tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java
git commit -m "Add checkMapping action to source-map spec suite

Asserts the mapper returns the expected source path, line, and
column. mappingName assertions remain unsupported (names are not
surfaced through SourceMapper in v1); affected cases must be
excludelisted."
```

---

## Task 12: Add `checkIgnoreList` action support

**Files:**
- Modify: `tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java`

`checkIgnoreList` action shape (per spec):
```json
{
  "actionType": "checkIgnoreList",
  "ignoreList": ["src/lib.js"]
}
```
The expected `ignoreList` is an array of *resolved* source paths (post-`sourceRoot`).

- [ ] **Step 1: Replace the stub**

In `SourceMapSpecSuiteTest.java`, replace the `runCheckIgnoreList` method body with:

```java
    private static void runCheckIgnoreList(SourceMapV3 parsed, Map<String, Object> action) {
        @SuppressWarnings("unchecked")
        List<Object> expected = (List<Object>) action.get("ignoreList");
        if (expected == null) {
            fail("checkIgnoreList missing 'ignoreList' field");
        }
        List<String> expectedStrings = new ArrayList<>(expected.size());
        for (Object e : expected) expectedStrings.add((String) e);
        assertEquals(expectedStrings, parsed.ignoreList(), "ignoreList mismatch");
    }
```

- [ ] **Step 2: Run the suite**

Run: `./gradlew :tests:test --tests SourceMapSpecSuiteTest`
Expected: `checkIgnoreList` cases now actually run.

- [ ] **Step 3: Run spotless**

Run: `./gradlew spotlessApply`

- [ ] **Step 4: Commit**

```bash
git add tests/src/test/java/org/mozilla/javascript/tests/sourcemap/SourceMapSpecSuiteTest.java
git commit -m "Add checkIgnoreList action to source-map spec suite"
```

---

## Task 13: Verify update-mode write-back works

**Files:** none (verification only)

- [ ] **Step 1: Run the suite in update mode**

Run: `./gradlew :tests:test --tests SourceMapSpecSuiteTest -DupdateSourceMapTestsExcludelist=true`
Expected: BUILD SUCCESSFUL (failures are suppressed in update mode); `tests/testsrc/source-map-tests-excludelist.txt` is regenerated.

- [ ] **Step 2: Verify the excludelist now contains failing case names**

Run: `head -20 tests/testsrc/source-map-tests-excludelist.txt`
Expected: header comments, then a sorted list of case names (one per line).

- [ ] **Step 3: Run the suite in normal mode**

Run: `./gradlew :tests:test --tests SourceMapSpecSuiteTest`
Expected: BUILD SUCCESSFUL — the excludelist now matches actual failures, so neither a "passing case on excludelist" nor a "failing case off excludelist" violation occurs.

- [ ] **Step 4: Do not commit yet**

The excludelist commit lands in Task 14 with a clean inspection of its contents.

---

## Task 14: Inspect, sanitize, and commit the initial excludelist

**Files:**
- Modify: `tests/testsrc/source-map-tests-excludelist.txt` (already regenerated)

This task is a manual verification pass: skim the excludelist, sanity-check that nothing on it is suspiciously something we should have handled, and commit.

- [ ] **Step 1: Read the excludelist**

Run: `wc -l tests/testsrc/source-map-tests-excludelist.txt && cat tests/testsrc/source-map-tests-excludelist.txt`
Note any patterns:
- Cases with `mappingName` assertions → expected (we don't surface names)
- Cases with `sections` → expected (we reject indexed maps)
- Cases that look like they should pass → investigate; do not commit until resolved

If you find a "should pass" case, debug:
- Run it directly: `./gradlew :tests:test --tests "SourceMapSpecSuiteTest.<case-name>" --info`
- Read the `.js.map` fixture and the test action to understand the discrepancy
- Either fix the underlying parser/decoder bug (in a separate commit, then regenerate the excludelist) or add a comment-line above the entry explaining why it stays excluded

- [ ] **Step 2: Run the suite once more to confirm clean status**

Run: `./gradlew :tests:test --tests SourceMapSpecSuiteTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run the full repo test suite**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add tests/testsrc/source-map-tests-excludelist.txt
git commit -m "Initial source-map-tests excludelist

Generated via -DupdateSourceMapTestsExcludelist=true. Each entry
is a source-map-spec-tests.json case whose assertions our
implementation cannot satisfy in v1 — primarily indexed-map and
mappingName assertions per the design's non-goals."
```

---

## Self-Review Notes

**Spec coverage check:**

| Spec section | Covered by |
|---|---|
| `Position` carries `sourcePath` | Task 1 (Step 1) |
| `SourceMapper` reshape | Task 1 (Step 2) |
| Consumer refactor (Parser, Context, BodyCodegen, CodeGenerator) | Task 1 (Steps 3-4); BodyCodegen/CodeGenerator unchanged because they only use `position.line()` |
| `SourceMapV3` factories `parse(String)/parse(Reader)/parseFile(Path)` | Task 5 (Step 3) — all three included from the start |
| Strict JSON parser | Task 3 |
| XSSI prefix stripping | Task 3 (Step 1 test, Step 3 implementation) |
| VLQ decoder + Segment record | Task 4 |
| Indexed-map rejection | Task 5 (Step 1 test, Step 3 implementation) |
| `sourceRoot` resolution + ignoreList resolution | Task 5 |
| `mapPosition` lookup semantics | Task 6 |
| `getSourceLineText` per-source line splitting + cache | Task 7 |
| `getPrimarySourceContent` returns `sourcesContent[0]` | Task 7 |
| End-to-end integration | Task 8 |
| tc39 submodule | Task 9 |
| Spec suite (validity, checkMapping, checkIgnoreList) | Tasks 10-12 |
| Excludelist normal-mode strictness | Task 10 |
| Update-mode write-back | Task 10 (implementation), Task 13 (verification) |
| Initial excludelist commit | Task 14 |

**Type/name consistency check (cross-task):**

- `Position(String sourcePath, int line, int column)` — same shape used in all references.
- `Segment(int genCol, int sourceIndex, int srcLine, int srcCol, int nameIndex)` — same in `Segment.java`, `MappingsDecoder`, `SourceMapV3.mapPosition`.
- `SourceMapV3.parse(String)`, `parse(Reader)`, `parseFile(Path)` — consistent signatures across plan.
- `getSourceLineText(String sourcePath, int lineNumber)` — same parameter order in interface, impl, tests, and consumers.
- `getPrimarySourceContent()` — used everywhere (interface, impl, tests, `Context`), no stale `getOriginalSource` references after Task 1.
- `SourceMapException` — referenced in `SourceMapJsonParser`, `MappingsDecoder`, `SourceMapV3`, all tests; consistent.

**Placeholder scan:** none found. All code blocks are complete; all gradle commands are concrete.
