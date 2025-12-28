/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Rhino's ECMAScript regular expression engine.
 *
 * <p>This package implements the ECMAScript RegExp specification (ECMA-262 §22.2) with full support
 * for ES5 through ES2025 features.
 *
 * <h2>Architecture</h2>
 *
 * The RegExp engine follows a classic three-phase design:
 *
 * <ol>
 *   <li><b>Parsing</b> - {@link org.mozilla.javascript.regexp.NativeRegExp} parses pattern syntax
 *       into an abstract syntax tree ({@link org.mozilla.javascript.regexp.RENode})
 *   <li><b>Compilation</b> - Transforms AST into bytecode program ({@link
 *       org.mozilla.javascript.regexp.RECompiled})
 *   <li><b>Execution</b> - Bytecode interpreter matches patterns against input strings using
 *       backtracking
 * </ol>
 *
 * <h2>ECMAScript Compatibility</h2>
 *
 * <table border="1">
 *   <caption>Supported ECMAScript Features</caption>
 *   <tr><th>Standard</th><th>Features</th><th>Status</th></tr>
 *   <tr><td>ES5</td><td>Global, case-insensitive, multiline flags</td><td>✓ Complete</td></tr>
 *   <tr><td>ES2015/ES6</td><td>Unicode flag (u), sticky flag (y), Unicode properties</td><td>✓
 *       Complete</td></tr>
 *   <tr><td>ES2018</td><td>dotAll flag (s), lookbehind, named groups</td><td>✓ Complete</td></tr>
 *   <tr><td>ES2022</td><td>hasIndices flag (d), indices array</td><td>✓ Complete</td></tr>
 *   <tr><td>ES2024</td><td>unicodeSets flag (v), Property of Strings, set operations</td><td>✓
 *       Complete</td></tr>
 *   <tr><td>ES2025</td><td>RegExp.escape() static method</td><td>✓ Complete</td></tr>
 * </table>
 *
 * <h2>Unicode Support</h2>
 *
 * <p>The engine supports 109+ Unicode properties including:
 *
 * <ul>
 *   <li>59 binary properties (Alphabetic, Emoji, etc.) via {@link
 *       org.mozilla.javascript.regexp.UnicodeProperties}
 *   <li>30 general categories (Letter, Number, etc.)
 *   <li>34+ scripts with optional ICU4J integration via {@link
 *       org.mozilla.javascript.regexp.ICU4JAdapter}
 *   <li>7 Property of Strings for emoji sequences via {@link
 *       org.mozilla.javascript.regexp.EmojiSequenceData}
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <dl>
 *   <dt>{@link org.mozilla.javascript.regexp.NativeRegExp}
 *   <dd>Main RegExp implementation, parser, and bytecode compiler (4400+ lines)
 *   <dt>{@link org.mozilla.javascript.regexp.CharacterClassCompiler}
 *   <dd>ES2024 v-flag character class compiler with set operations (600+ lines)
 *   <dt>{@link org.mozilla.javascript.regexp.StringMatcher}
 *   <dd>Universal string matcher for single chars, multi-char sequences, and emoji
 *   <dt>{@link org.mozilla.javascript.regexp.UnicodeProperties}
 *   <dd>Unicode property lookup engine (109+ properties, 900+ lines)
 *   <dt>{@link org.mozilla.javascript.regexp.EmojiSequenceData}
 *   <dd>ES2024 Property of Strings data for emoji sequences
 *   <dt>{@link org.mozilla.javascript.regexp.ICU4JAdapter}
 *   <dd>Optional ICU4J integration with graceful fallback to Java's built-in Unicode APIs
 *   <dt>{@link org.mozilla.javascript.regexp.RegExpDebugger}
 *   <dd>Bytecode disassembler for debugging compiled patterns
 * </dl>
 *
 * <h2>Bytecode Format</h2>
 *
 * <p>Compiled patterns use a custom bytecode format with 46 opcodes. See {@link
 * org.mozilla.javascript.regexp.NativeRegExp} lines 100-126 for bytecode format documentation and
 * {@link org.mozilla.javascript.regexp.RegExpDebugger#disassemble} for opcode details.
 *
 * <h2>Requirements</h2>
 *
 * <ul>
 *   <li><b>Java Version:</b> Java 11 or higher (uses standard Java Unicode APIs)
 *   <li><b>Optional:</b> ICU4J 70+ for enhanced Unicode support (falls back to Java APIs if absent)
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // ES2024 unicodeSets with Property of Strings
 * var re = /[\p{RGI_Emoji_Flag_Sequence}]/v;
 * re.test("🏳️‍🌈");  // true - matches rainbow flag emoji sequence
 *
 * // ES2025 RegExp.escape()
 * var userInput = "How much? $5.00";
 * var escaped = RegExp.escape(userInput);  // "How much\\? \\$5\\.00"
 * var pattern = new RegExp(escaped);       // Safe pattern matching
 * }</pre>
 *
 * @since Rhino 1.0
 * @see <a href="https://tc39.es/ecma262/#sec-regexp-regular-expression-objects">ECMA-262 §22.2
 *     RegExp Objects</a>
 */
package org.mozilla.javascript.regexp;
