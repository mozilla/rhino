# ECMA-426 Source Map Implementation — Design

Status: approved (2026-05-04)

## Goal

Provide a real, spec-conformant implementation of the existing
`org.mozilla.javascript.sourcemap.SourceMapper` interface that reads
ECMA-426 ("Source Map Format Specification") source maps. Validate the
implementation with the official tc39/source-map-tests suite.

## Non-goals

- Indexed (sectioned) source maps. The parser rejects them with a clear
  error.
- Auto-discovery of `//# sourceMappingURL=` directives in transpiled
  scripts.
- Surfacing source-map `names` to existing consumers (parser, debugger,
  stack traces). The decoder parses 5-field segments without error but
  the name index is dropped.
- Threading per-line source-file identity through bytecode emission,
  the interpreter icode, the `LineNumberTable`, the debugger UI, and
  stack traces. Stack traces continue to show the script's
  `sourceName`, not the per-line source path.
- The `range-mappings-proposal-tests.json` suite (tracks a stage-3
  proposal, not yet ECMA-426).

## Architecture

Five components, all in `org.mozilla.javascript.sourcemap`:

1. **`Position`** (existing record) — gains a leading `String sourcePath`
   field. Becomes `Position(String sourcePath, int line, int column)`.
2. **`SourceMapper`** (existing interface) — method signatures change:
   - `mapPosition(int targetLine, int targetColumn)` — unchanged
     signature, but now returns `Position` carrying `sourcePath`.
   - `getSourceLineText(String sourcePath, int lineNumber)` — gains
     `sourcePath` parameter.
   - `getPrimarySourceContent()` — renamed from `getOriginalSource()`
     for clarity (it returns the *content* of the *primary* source).
3. **`SourceMapV3`** (new, public) — concrete `SourceMapper`
   implementation backed by a parsed ECMA-426 plain source map. Public
   static factories. Test-harness accessors for `sources()`,
   `ignoreList()`, `file()`.
4. **`SourceMapJsonParser`** (new, package-private) — small dedicated
   strict JSON parser tailored to source-map shape. Returns generic
   Java values (`Map<String,Object>`, `List<Object>`, `String`, `Long`,
   `Double`, `Boolean`, `null`). No Rhino runtime dependency.
5. **`MappingsDecoder`** (new, package-private) — Base64-VLQ decoder
   producing `List<List<Segment>>` indexed by 0-based generated line.
6. **`SourceMapException`** (new, public) — `extends RuntimeException`.
   Thrown by parser/decoder for any spec violation, including indexed
   maps.

## Public API

```java
package org.mozilla.javascript.sourcemap;

public record Position(String sourcePath, int line, int column) {}

public interface SourceMapper {
    Position mapPosition(int targetLine, int targetColumn);
    String getSourceLineText(String sourcePath, int lineNumber);
    String getPrimarySourceContent();
}

public final class SourceMapV3 implements SourceMapper {
    public static SourceMapV3 parse(String json);
    public static SourceMapV3 parse(Reader reader) throws IOException;
    public static SourceMapV3 parseFile(Path path) throws IOException;

    @Override public Position mapPosition(int targetLine, int targetColumn);
    @Override public String getSourceLineText(String sourcePath, int lineNumber);
    @Override public String getPrimarySourceContent();

    public List<String> sources();
    public List<String> ignoreList();
    public String file();
}

public class SourceMapException extends RuntimeException { /* ... */ }
```

### API contract notes

- `parse(Reader)` reads to string then delegates to `parse(String)`.
  `parseFile(Path)` reads UTF-8 then delegates.
- The XSSI prefix `)]}'` (per ECMA-426 §3.2 note) is stripped before
  JSON parsing if present at the very start of the input.
- `sourceRoot` is concatenated onto each non-null entry of `sources` at
  parse time. A `/` separator is inserted only when `sourceRoot` is
  non-empty and does not already end in `/`. The resulting resolved
  paths are what `mapPosition` returns and what `getSourceLineText`
  expects as the lookup key.
- `ignoreList` (per spec, an array of `sources` indices) is resolved to
  an array of resolved source paths at parse time so the public API is
  uniformly string-based.
- `getPrimarySourceContent()` returns `sourcesContent[0]` (the content
  of the first source). Returns `null` if `sourcesContent` is missing,
  empty, or its first entry is `null`. Per ECMA-426 the top-level
  `file` field names the *generated* file, not a source — so it is not
  used in primary-source selection.
- `getSourceLineText(sourcePath, lineNumber)` returns the
  `lineNumber`-th line (1-indexed) from the `sourcesContent` entry
  whose resolved path equals `sourcePath`. Returns `null` if the path
  is unknown, the entry is missing, or the line is out of range.

## Internal data flow

### Parsing (one-shot, at `parse(...)` time)

1. `SourceMapJsonParser.parse(json)` returns a generic `Map<String,Object>`
   tree. Strict: rejects trailing commas and other JSON5 extensions.
2. `SourceMapV3` validates the top-level object:
   - `version` must be the JSON number `3` (parser may return it as
     `Long` or `Double`; both `3L` and `3.0` are accepted, anything
     else rejected). Anything else → `SourceMapException`.
   - Presence of a `sections` key → `SourceMapException("indexed source maps are not supported")`.
   - `mappings` required (string).
   - `sources` required (array of string-or-null).
   - `sourcesContent`, `names`, `sourceRoot`, `file`, `ignoreList`
     optional; types per spec.
3. Resolve `sourceRoot` against each non-null `sources[i]`. Resolve
   `ignoreList` indices to paths.
4. `MappingsDecoder.decode(mappings, sourcesCount, namesCount)` returns
   `List<List<Segment>>` indexed by 0-based generated line. Each
   `Segment` is `(genCol, sourceIndex, srcLine, srcCol, nameIndex)`
   with `-1` indicating "absent" for the optional fields. Decoder
   enforces:
   - 1, 4, or 5 fields per segment
   - Per-line `genCol` non-decreasing
   - Source/name indices in range
   - Per-spec delta accumulation (deltas reset per generated line for
     `genCol`; do not reset for `sourceIndex`, `srcLine`, `srcCol`,
     `nameIndex`)
5. Lines without segments yield empty inner lists.

### Lookup (every `mapPosition`)

1. `targetLine` is 1-indexed externally. Subtract 1 for the array index;
   out-of-range → `null`.
2. Binary-search the line's segments for the largest `genCol <= (targetColumn - 1)`.
3. No such segment (line empty, or column precedes the first segment) → `null`.
4. Found segment is 1-field (no source info) → `null`.
5. Otherwise return `new Position(resolvedSourcePaths[sourceIndex], srcLine + 1, srcCol + 1)`.

### Storage

```
SourceMapV3 fields:
    String file;                            // optional, may be null
    List<String> sourcePaths;               // resolved; may contain nulls
    List<String> sourcesContent;            // parallel; nullable entries; may be null overall
    List<String> ignoreList;                // resolved paths
    List<List<Segment>> segmentsByLine;     // 0-indexed
    Map<String, List<String>> lineCache;    // per-path, computed lazily
```

`getSourceLineText` splits the matching `sourcesContent` entry by
`\n` lazily and caches the per-line array per source path.

## Refactor of existing consumers

Commit `c5e56f56e` introduced the SourceMapper consumers. They need
trivial threading updates:

- **`Parser`** — already calls `getSourceLineText(line)` for parser
  errors. After mapping, it has a `Position`; pass `pos.sourcePath()`
  through. If `Position.sourcePath()` is `null`, fall back to current
  behavior (no line text).
- **`BodyCodegen` / `CodeGenerator`** — call `mapPosition(...)` and use
  `position.line()` only. The `sourcePath` is dropped (see non-goals).
- **`Context.compileScript` / `Context.compileFunction`** — call
  `getOriginalSource()` for the debugger handoff. Renamed to
  `getPrimarySourceContent()`. Behavior unchanged.
- **`SourceMapperTest.TestMapper`** — adapted to the new method
  signatures. Existing assertions stay equivalent; tests can pass a
  stub `sourcePath` (e.g., `"original.js"`) that the assertions do not
  inspect.

## Error handling

### Parse-time

All parse-time validation throws `SourceMapException extends RuntimeException`.
Message families:

| Cause | Message shape |
|---|---|
| Malformed JSON | `"invalid JSON: <reason> at offset N"` |
| Wrong/missing `version` | `"unsupported source map version: <value>"` (or `"missing"`) |
| `sections` key present | `"indexed source maps are not supported"` |
| Missing required field | `"missing required field 'mappings'"` etc. |
| Wrong field type | `"field 'sources' must be an array of strings, got <type>"` |
| VLQ decode error | `"invalid VLQ at line L, segment S"` |
| Segment with 2/3 fields, or >5 | `"invalid segment length at line L"` |
| Source/name index out of range | `"source index N out of range at line L"` |
| `sourcesContent` length mismatch | `"sourcesContent length does not match sources length"` |

### Lookup-time

`mapPosition`, `getSourceLineText`, and `getPrimarySourceContent`
never throw. They return `null` for "no answer" per the existing
interface contract.

### I/O

`parseFile(Path)` and `parse(Reader)` propagate `IOException` directly.
They do not wrap it in `SourceMapException` — these are filesystem
issues, not source-map issues.

## Testing strategy

### Unit tests (JUnit 5, in `rhino` module)

- **`SourceMapJsonParserTest`** — happy paths, malformed JSON, type
  strictness, XSSI prefix stripping.
- **`MappingsDecoderTest`** — VLQ corner cases (continuation bits, sign
  bit, zero, large), 1/4/5-field segments, out-of-range indices, empty
  lines (`;;`), missing trailing newline.
- **`SourceMapV3Test`** — top-level field validation, indexed-map
  rejection, `sourceRoot` resolution, `ignoreList` resolution,
  `getPrimarySourceContent` selection logic, `getSourceLineText` line
  splitting and caching, lookup semantics (largest-genCol-≤, 1-field
  segment returns `null`, out-of-range line returns `null`).

### Refactor of existing tests

`SourceMapperTest` adapts to the new method signatures. `TestMapper`
implements the new `getSourceLineText(String sourcePath, int line)` and
`getPrimarySourceContent()`. Assertions stay equivalent.

### Spec-suite integration (in `tests` module)

- Submodule at `tests/source-map-tests`, pinned to a specific
  upstream commit.
- `SourceMapSpecSuiteTest` (JUnit 5, `@ParameterizedTest` with
  `@MethodSource`):
  - Loads `tests/source-map-tests/source-map-spec-tests.json`.
  - For each case: locate `sourceMapFile` under
    `tests/source-map-tests/resources/`, call `SourceMapV3.parse(...)`,
    assert against `sourceMapIsValid`.
  - For each `testActions` entry of type `checkMapping`: call
    `mapPosition(...)` and assert returned tuple. Assertions on
    `name` are skipped (we do not surface names) — those cases go on
    the excludelist.
  - For each `testActions` entry of type `checkIgnoreList`: assert
    against `SourceMapV3.ignoreList()`.
  - Unrecognized `testActions` types → test fails with a clear
    "unsupported action" message, prompting an excludelist entry.
- Excludelist: `tests/testsrc/source-map-tests-excludelist.txt`. One
  case `name` per line. Failing cases not on the list fail the build.
  Cases on the list that currently pass fail the build (keeps the
  list honest, mirroring test262's roll-up check).
- Suite runs as part of `./gradlew test` (default), not gated behind
  a separate task.
- `range-mappings-proposal-tests.json` is **not** wired in.

### Excludelist update workflow

Mirrors test262's `updateTest262properties` flag.

- `./gradlew :tests:test --tests SourceMapSpecSuiteTest -DupdateSourceMapTestsExcludelist=true`
  flips the suite into write-back mode:
  - Failures are recorded but do not fail the build.
  - An `@AfterAll` hook writes a fresh
    `tests/testsrc/source-map-tests-excludelist.txt` containing exactly
    the currently-failing case names, sorted, with a header comment
    noting "auto-generated; review diff before committing".
  - The developer reviews `git diff` and commits.
- Comments in the excludelist file get wiped on regeneration. Reasons
  live in PR descriptions and `git blame` on the file. (This matches
  test262's `.properties` workflow.)

