# Source Map Support

Rhino can remap stack-trace positions, parser error positions, and debugger source
handoffs back to the original source when a transpiler or minifier has generated the
script being compiled. The feature is opt-in: attach a `SourceMapper` to a compile spec
and Rhino uses it automatically.

## Quick start

```java
SourceMapV3 mapper = SourceMapV3.parse(mapJsonString);

Script script = cx.compileScript(
    ScriptCompileSpec.fromSource(transpiledSource)
        .sourceName("bundle.js")
        .sourceMapper(mapper)
        .build());
```

From that point on a stack frame names the original file, line and column it came from,
even where one bundle maps back to many original files; parser error messages quote the
original source text; and the debugger receives the primary original source content at
compilation time.

## Architecture

The classes in `org.mozilla.javascript.sourcemap`:

| Class | Visibility | Role |
|---|---|---|
| `SourceMapper` | `public interface` | Contract consumed by Rhino's parser and runtime |
| `SourceMapV3` | `public final class` | ECMA-426 plain source map; implements `SourceMapper` |
| `SourceMapJsonParser` | package-private | Strict JSON parser; no external dependencies |
| `MappingsDecoder` | package-private | Base64-VLQ mappings decoder |
| `Segment` | package-private record | One decoded mappings segment |
| `SourceMapException` | `public class` | Unchecked parse/decode error |
| `Position` | `public record` | `(sourcePath, line, column)` in original source |

### SourceMapper interface

Three methods:

- `Position mapPosition(int targetLine, int targetColumn)` — maps a 1-indexed
  transpiled position to the original source. Returns `null` when no mapping exists
  (gap segment, out-of-range line, column before the first segment on a line).
- `String getSourceLineText(String sourcePath, int lineNumber)` — returns a line of
  original source text by path and 1-indexed line number. Used for parser error messages.
- `String getPrimarySourceContent()` — returns the full text of the first original
  source. Used for the debugger compilation handoff.

### SourceMapV3

Parses an ECMA-426 v3 source map and implements `SourceMapper`. Additional accessors:

- `List<String> sources()` — resolved source paths (after `sourceRoot` is prepended)
- `List<String> ignoreList()` — resolved paths of sources listed in `ignoreList`
- `String file()` — the `file` field, or `null`
- `String getMappedName(int targetLine, int targetColumn)` — the original symbol name
  from the 5th VLQ field (`names` array), or `null` if the segment carries no name

Three factory methods: `parse(String)`, `parse(Reader)`, `parseFile(Path)`.

The XSSI prefix `)]}'` is stripped automatically per ECMA-426 §3.2.

**Not supported:** indexed (sectioned) source maps (`sections` field) — parsing throws
`SourceMapException`. Transitive (chained) source maps are also out of scope.

### JSON parser

`SourceMapJsonParser` is a self-contained recursive-descent parser with no dependency on
any JSON library. It produces:

- objects → `LinkedHashMap<String, Object>` (insertion order preserved)
- arrays → `ArrayList<Object>`
- strings → `String` (full escape handling including `\uXXXX`)
- integers → `Long`; floats/exponents → `Double`
- booleans → `Boolean`; null → `null`

All errors throw `SourceMapException` with the byte offset.

### Mappings decoder

`MappingsDecoder` decodes the Base64-VLQ `mappings` string into
`List<List<Segment>>` grouped by 0-indexed generated line.

Key behaviours:
- Segments within each line are **sorted by generated column** after decoding.
  ECMA-426 allows out-of-order segments (negative genCol deltas); the sort makes the
  binary search in `mapPosition` correct regardless.
- **Zero-field segments** (consecutive commas, leading commas) are rejected.
- **Negative generated column** after applying a delta is rejected.
- **VLQ overflow**: accumulation uses `long`; decoded magnitude > `Integer.MAX_VALUE`
  throws. This enforces the ECMA-426 requirement to reject fields exceeding 32 bits.
  VLQs with many continuation-bit digits but a small final value (e.g. the
  `validMappingLargeVLQ` spec case) are accepted correctly.

Segment field counts: 1 (gap, no source info), 4 (source mapped), or 5 (source + name).
Other counts throw.

## Spec compliance

The implementation is validated against the
[tc39/source-map-tests](https://github.com/tc39/source-map-tests) suite
(submoduled at `tests/source-map-tests`). 93 of 99 cases pass. The 6 excluded cases
are:

| Case | Reason |
|---|---|
| `basicMappingWithIndexMap` | Indexed map (`sections`) — not supported |
| `indexMapEmptySections` | Indexed map |
| `indexMapWithMissingFile` | Indexed map |
| `indexMapWithTwoConcatenatedSources` | Indexed map |
| `transitiveMapping` | `checkMappingTransitive` action — chained maps not supported |
| `transitiveMappingWithThreeSteps` | `checkMappingTransitive` action |

The excludelist lives at `tests/testsrc/source-map-tests-excludelist.txt`. Cases on
the list that start passing will fail the build (keeps the list honest). To regenerate
after fixing bugs:

```
./gradlew :tests:test --tests SourceMapSpecSuiteTest -DupdateSourceMapTestsExcludelist=true
```

## Rhino integration points

| Where | What |
|---|---|
| `Parser.mapLocation` | Remaps error position and fetches original source line text |
| `Context` debugger handoff | Passes `getPrimarySourceContent()` to the debugger at compile time |
| `CodeGenerator` / `BodyCodegen` | Record the whole mapped position, so a stack frame reports the original file, line and column |

## Positions in a stack trace

Both backends record a position wherever one can be blamed: at each statement, and at each
property read, element read and call, so a failure is attributed to the operation rather
than to the start of the statement. For `foo.bar.baz()` that is the column of `baz`, which
is what V8 reports for the same code.

An AST node is positioned at the start of its whole expression, deliberately, for parity
with other parsers. Error reporting instead takes the position of the part being read, so
the parser's convention is left alone.

### The interpreter

Positions live in a table beside the code, keyed by the bytecode offset they take effect
at; `RhinoException` finds one by walking it up to the frame's program counter. Nothing is
spent maintaining them as the code runs, which matters because they are only ever read
while reporting an error. A `LINE` icode remains, without an operand, purely so the
debugger can step by line.

The table (`PositionTable`) is a stream of variable-length deltas, the form V8's source
position table and HotSpot's own line number table take: on a real bundle it costs about
three and a half bytes per position, where fixed words would cost eight and outweigh the
icode itself. It is read front to back, which is fine for something consulted once per
frame of an exception.

### The JVM bytecode backend

A compiled frame exposes one datum that varies from position to position: the
`line_number` of a `LineNumberTable` entry, sixteen bits per JVMS 4.7.12. The class name,
method name and source file are fixed for the whole frame, so those sixteen bits are the
entire channel a column can travel through.

Rhino chooses what goes in them. A position whose line is not yet spoken for emits its
real line, which is what a Java debugger expects. A second position on a line already
claimed by a different one emits a *position marker* from the top of the range instead,
which `CompiledPositions` maps back. Markers are handed out per method, since a lookup is
keyed by method as well; behind each method sits the same `PositionTable` the interpreter
uses, keyed by the reported number rather than by bytecode offset.

Code compiled ahead of time to class files, via `ClassCompiler`, is loaded without the
step that attaches that table, so it emits real line numbers only and reports no column
where a line holds several positions.
