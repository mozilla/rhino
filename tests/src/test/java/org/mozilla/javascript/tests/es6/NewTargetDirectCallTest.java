/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.reflect.ClassCompiler;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests for {@code new.target} on the two "direct call" code paths of {@link
 * org.mozilla.javascript.optimizer.Codegen}: the direct constructor emitted by {@code
 * emitDirectConstructor} and the inlined call site emitted by {@code
 * BodyCodegen.visitOptimizedCall}.
 *
 * <p>A function only becomes a direct call target when it is a named function statement of the
 * top-level script, is called from inside another function with a matching argument count and does
 * not require an activation object. Every script below is written so that those conditions hold;
 * the {@code generates*} tests assert that the optimized code was really emitted, so that the
 * semantic tests cannot silently start covering the generic path instead.
 */
public class NewTargetDirectCallTest {

    // ------------------------------------------------------------------
    // emitDirectConstructor: new.target inside a directly constructed function
    // ------------------------------------------------------------------

    @Test
    public void directConstructorSeesFunctionAsNewTarget() {
        Utils.assertWithAllModes_ES6(
                "function|target|true",
                Utils.lines(
                        "function target(a) {",
                        "  this.r =",
                        "    (typeof new.target) + '|' + new.target.name + '|' + (new.target === target);",
                        "}",
                        "function caller() { return new target(1).r; }",
                        "caller();"));
    }

    /**
     * Pins the new.target argument that {@code emitDirectConstructor} forwards to the body method.
     * Passing anything other than the incoming callee there - {@code null} in particular, which is
     * neither {@code === undefined} nor caught by a truthiness guard - has to be caught by at least
     * one of these checks.
     */
    @Test
    public void directConstructorForwardsCalleeAsNewTarget() {
        Utils.assertWithAllModes_ES6(
                "function|true|false|false|true",
                Utils.lines(
                        "function target(a) {",
                        "  this.r = [",
                        "    typeof new.target,",
                        "    new.target === target,",
                        "    new.target === null,",
                        "    new.target === undefined,",
                        "    !!new.target",
                        "  ].join('|');",
                        "}",
                        "function caller() { return new target(1).r; }",
                        "caller();"));
    }

    @Test
    public void directCallHasUndefinedNewTarget() {
        Utils.assertWithAllModes_ES6(
                "undefined",
                Utils.lines(
                        "function target(a) { return typeof new.target; }",
                        "function caller() { return target(1); }",
                        "caller();"));
    }

    @Test
    public void bothPathsFromTheSameCaller() {
        Utils.assertWithAllModes_ES6(
                "true|false",
                Utils.lines(
                        "function target(a) { this.r = new.target === target; return this.r; }",
                        "function caller() {",
                        "  var constructed = new target(1).r;",
                        "  var called = target(2);",
                        "  return constructed + '|' + called;",
                        "}",
                        "caller();"));
    }

    @Test
    public void newTargetIsNotInheritedByANestedDirectCall() {
        Utils.assertWithAllModes_ES6(
                "undefined|true",
                Utils.lines(
                        "function inner(b) { return typeof new.target; }",
                        "function outer(a) {",
                        "  this.r = inner(a) + '|' + (new.target === outer);",
                        "}",
                        "function caller() { return new outer(1).r; }",
                        "caller();"));
    }

    @Test
    public void newTargetGuardThrowsOnDirectCallOnly() {
        Utils.assertWithAllModes_ES6(
                "ok|TypeError",
                Utils.lines(
                        "function target(a) {",
                        "  if (new.target === undefined) throw new TypeError('needs new');",
                        "  this.a = a;",
                        "}",
                        "function caller() {",
                        "  var constructed = new target(1).a === 1 ? 'ok' : 'bad';",
                        "  var thrown;",
                        "  try { target(2); thrown = 'no throw'; } catch (e) { thrown = e.name; }",
                        "  return constructed + '|' + thrown;",
                        "}",
                        "caller();"));
    }

    @Test
    public void directConstructorUsesFunctionPrototype() {
        Utils.assertWithAllModes_ES6(
                "true|true|true",
                Utils.lines(
                        "function target(a) {",
                        "  this.r =",
                        "    (Object.getPrototypeOf(this) === target.prototype) + '|' +",
                        "    (new.target.prototype === target.prototype) + '|' +",
                        "    (this instanceof target);",
                        "}",
                        "function caller() { return new target(1).r; }",
                        "caller();"));
    }

    @Test
    public void directConstructorHonoursReplacedPrototype() {
        Utils.assertWithAllModes_ES6(
                true,
                Utils.lines(
                        "var proto = {};",
                        "function target(a) { this.a = a; }",
                        "target.prototype = proto;",
                        "function caller() { return Object.getPrototypeOf(new target(1)) === proto; }",
                        "caller();"));
    }

    @Test
    public void directConstructorReturningObjectWins() {
        Utils.assertWithAllModes_ES6(
                "replaced|true",
                Utils.lines(
                        "function target(a) {",
                        "  this.a = a;",
                        "  return { tag: 'replaced', nt: new.target === target };",
                        "}",
                        "function caller() {",
                        "  var o = new target(1);",
                        "  return o.tag + '|' + o.nt;",
                        "}",
                        "caller();"));
    }

    @Test
    public void directConstructorReturningPrimitiveYieldsNewInstance() {
        Utils.assertWithAllModes_ES6(
                "1|true",
                Utils.lines(
                        "function target(a) { this.a = a; return 42; }",
                        "function caller() {",
                        "  var o = new target(1);",
                        "  return o.a + '|' + (o instanceof target);",
                        "}",
                        "caller();"));
    }

    @Test
    public void zeroArgumentDirectConstructor() {
        Utils.assertWithAllModes_ES6(
                "true|undefined",
                Utils.lines(
                        "function target() { this.r = new.target === target; }",
                        "function target2() { return typeof new.target; }",
                        "function caller() { return new target().r + '|' + target2(); }",
                        "caller();"));
    }

    /**
     * Numeric arguments travel through the {@code Object}/{@code double} register pairs of the
     * direct call signature rather than through the object slot, which is a separate branch of
     * {@code visitOptimizedCall}.
     */
    @Test
    public void numericArgumentsOnBothDirectPaths() {
        Utils.assertWithAllModes_ES6(
                "6|3",
                Utils.lines(
                        "function target(x) { return (new.target === target ? 2 : 1) * x; }",
                        "function targetWrap(x) { this.r = (new.target === targetWrap ? 2 : 1) * x; }",
                        "function caller() {",
                        "  return (new targetWrap(3).r) + '|' + target(3);",
                        "}",
                        "caller();"));
    }

    /**
     * The direct constructor and the plain direct call of the same function are nested inside each
     * other, so a leaked {@code new.target} would show up on the inner call.
     */
    @Test
    public void newTargetOfRecursiveDirectConstructor() {
        Utils.assertWithAllModes_ES6(
                "true|undefined|true",
                Utils.lines(
                        "function target(depth) {",
                        "  if (depth === 0) return typeof new.target;",
                        "  this.r =",
                        "    (new.target === target) + '|' + target(0) + '|' + (new.target === target);",
                        "  return undefined;",
                        "}",
                        "function caller() { return new target(1).r; }",
                        "caller();"));
    }

    // ------------------------------------------------------------------
    // The guarded fall back: the call site no longer refers to the compiled function
    // ------------------------------------------------------------------

    @Test
    public void newTargetWhenCallSiteTargetIsReplaced() {
        Utils.assertWithAllModes_ES6(
                "-1|function|undefined",
                Utils.lines(
                        "function target(a) { this.a = a; }",
                        "function caller() {",
                        "  var seen = [];",
                        "  target = function (a) { seen.push(typeof new.target); this.a = -a; };",
                        "  var o = new target(1);",
                        "  target(2);",
                        "  return o.a + '|' + seen.join('|');",
                        "}",
                        "caller();"));
    }

    // ------------------------------------------------------------------
    // The non direct entry point of a function that is a direct call target
    // ------------------------------------------------------------------

    /**
     * A function that is a direct call target still gets a normal, non direct entry point (see
     * {@code Codegen.emitNonDirectCall}) which has to forward {@code new.target} as well.
     */
    @Test
    public void nonDirectEntryPointOfADirectCallTarget() {
        Utils.assertWithAllModes_ES6(
                "true|true|false|3",
                Utils.lines(
                        "var seen = [];",
                        "function target(a, b) { seen.push(new.target === target); this.a = a; }",
                        "function makeDirect() { return new target(1, 2); }",
                        "makeDirect();", // direct constructor path
                        "var o = new target(3, 4);", // non direct entry point, constructing
                        "target(5, 6);", // non direct entry point, plain call
                        "seen.join('|') + '|' + o.a;"));
    }

    @Test
    public void reflectConstructOverridesNewTargetOfADirectCallTarget() {
        Utils.assertWithAllModes_ES6(
                "other|true|false",
                Utils.lines(
                        "function target(a) { this.nt = new.target.name; }",
                        "function other(a) {}",
                        "function makeDirect() { return new target(1); }",
                        "makeDirect();",
                        "var r = Reflect.construct(target, [2], other);",
                        "r.nt + '|' + (r instanceof other) + '|' + (r instanceof target);"));
    }

    // ------------------------------------------------------------------
    // Structural checks: the optimized code really is emitted
    // ------------------------------------------------------------------

    @Test
    public void generatesDirectConstructorAndInlinedNewCallSite() {
        String source =
                Utils.lines(
                        "function target(a) { this.r = new.target === target; }",
                        "function caller() { return new target(1).r; }",
                        "caller();");

        List<String> directCtors = declaredDirectConstructors(source);
        assertTrue(
                !directCtors.isEmpty(),
                "expected emitDirectConstructor to declare a direct constructor, methods: "
                        + declaredMethodNames(source));

        Set<String> invoked = invokedMethodNames(source);
        assertTrue(
                invoked.contains("getDescriptor"),
                "expected the guarded direct call site of visitOptimizedCall");
        assertTrue(
                invoked.containsAll(directCtors),
                "expected the call site to invoke " + directCtors + " but only found " + invoked);
    }

    @Test
    public void generatesInlinedPlainCallSite() {
        String source =
                Utils.lines(
                        "function target(a) { return typeof new.target; }",
                        "function caller() { return target(1); }",
                        "caller();");

        Set<String> invoked = invokedMethodNames(source);
        assertTrue(
                invoked.contains("getDescriptor"),
                "expected the guarded direct call site of visitOptimizedCall");
        assertTrue(
                invoked.contains("getThisObj"),
                "expected the non-constructor branch of visitOptimizedCall");

        // The direct constructor is still declared for any direct call target, but nothing
        // constructs "target" here, so it must not be invoked.
        List<String> directCtors = declaredDirectConstructors(source);
        assertTrue(!directCtors.isEmpty(), "expected a declared direct constructor");
        for (String directCtor : directCtors) {
            assertTrue(
                    !invoked.contains(directCtor), "did not expect an invocation of " + directCtor);
        }
    }

    @Test
    public void noDirectCallWhenActivationIsRequired() {
        // "arguments" forces an activation object, which disqualifies the function.
        String source =
                Utils.lines(
                        "function target(a) { return arguments.length + (new.target ? 1 : 0); }",
                        "function caller() { return new target(1); }",
                        "caller();");

        assertTrue(
                declaredDirectConstructors(source).isEmpty(),
                "expected no direct constructor, methods: " + declaredMethodNames(source));
        assertTrue(
                !invokedMethodNames(source).contains("getDescriptor"),
                "expected no guarded direct call site");
    }

    // ------------------------------------------------------------------
    // Minimal class file reader used by the structural checks
    // ------------------------------------------------------------------

    /** Names of the {@code _n<index>} methods emitted by {@code Codegen.emitDirectConstructor}. */
    private static List<String> declaredDirectConstructors(String source) {
        List<String> result = new ArrayList<>();
        for (String name : declaredMethodNames(source)) {
            if (name.startsWith("_n")) {
                result.add(name);
            }
        }
        return result;
    }

    private static List<String> declaredMethodNames(String source) {
        List<String> result = new ArrayList<>();
        for (byte[] classFile : compile(source)) {
            result.addAll(new ClassFile(classFile).declaredMethodNames());
        }
        return result;
    }

    /**
     * Names of all methods for which a {@code CONSTANT_Methodref} exists. Unlike a declaration, a
     * method reference is only added to the constant pool when some instruction actually invokes
     * the method, so this shows which calls were emitted.
     */
    private static Set<String> invokedMethodNames(String source) {
        Set<String> result = new HashSet<>();
        for (byte[] classFile : compile(source)) {
            result.addAll(new ClassFile(classFile).invokedMethodNames());
        }
        return result;
    }

    private static List<byte[]> compile(String source) {
        ClassCompiler compiler = new ClassCompiler(new CompilerEnvirons());
        Object[] compiled = compiler.compileToClassFiles(source, "test.js", 1, "test");
        List<byte[]> result = new ArrayList<>();
        for (int i = 1; i < compiled.length; i += 2) {
            result.add((byte[]) compiled[i]);
        }
        return result;
    }

    /** Just enough of a class file parser to read the constant pool and the method table. */
    private static final class ClassFile {
        private static final int CONSTANT_UTF8 = 1;
        private static final int CONSTANT_INTEGER = 3;
        private static final int CONSTANT_FLOAT = 4;
        private static final int CONSTANT_LONG = 5;
        private static final int CONSTANT_DOUBLE = 6;
        private static final int CONSTANT_CLASS = 7;
        private static final int CONSTANT_STRING = 8;
        private static final int CONSTANT_FIELDREF = 9;
        private static final int CONSTANT_METHODREF = 10;
        private static final int CONSTANT_INTERFACE_METHODREF = 11;
        private static final int CONSTANT_NAME_AND_TYPE = 12;
        private static final int CONSTANT_METHOD_HANDLE = 15;
        private static final int CONSTANT_METHOD_TYPE = 16;
        private static final int CONSTANT_DYNAMIC = 17;
        private static final int CONSTANT_INVOKE_DYNAMIC = 18;
        private static final int CONSTANT_MODULE = 19;
        private static final int CONSTANT_PACKAGE = 20;

        private final int[] tags;
        private final String[] strings;

        /** For the *ref and NameAndType entries: the two index operands. */
        private final int[] first;

        private final int[] second;

        private final List<String> declaredMethodNames = new ArrayList<>();

        ClassFile(byte[] bytes) {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                in.readInt(); // magic
                in.readUnsignedShort(); // minor version
                in.readUnsignedShort(); // major version

                int poolCount = in.readUnsignedShort();
                tags = new int[poolCount];
                strings = new String[poolCount];
                first = new int[poolCount];
                second = new int[poolCount];

                for (int i = 1; i < poolCount; i++) {
                    int tag = in.readUnsignedByte();
                    tags[i] = tag;
                    switch (tag) {
                        case CONSTANT_UTF8:
                            strings[i] = in.readUTF();
                            break;
                        case CONSTANT_INTEGER:
                        case CONSTANT_FLOAT:
                            in.readInt();
                            break;
                        case CONSTANT_LONG:
                        case CONSTANT_DOUBLE:
                            in.readLong();
                            i++; // takes two constant pool slots
                            break;
                        case CONSTANT_CLASS:
                        case CONSTANT_STRING:
                        case CONSTANT_METHOD_TYPE:
                        case CONSTANT_MODULE:
                        case CONSTANT_PACKAGE:
                            first[i] = in.readUnsignedShort();
                            break;
                        case CONSTANT_FIELDREF:
                        case CONSTANT_METHODREF:
                        case CONSTANT_INTERFACE_METHODREF:
                        case CONSTANT_NAME_AND_TYPE:
                        case CONSTANT_DYNAMIC:
                        case CONSTANT_INVOKE_DYNAMIC:
                            first[i] = in.readUnsignedShort();
                            second[i] = in.readUnsignedShort();
                            break;
                        case CONSTANT_METHOD_HANDLE:
                            in.readUnsignedByte();
                            in.readUnsignedShort();
                            break;
                        default:
                            throw new IllegalStateException("unknown constant pool tag " + tag);
                    }
                }

                in.readUnsignedShort(); // access flags
                in.readUnsignedShort(); // this class
                in.readUnsignedShort(); // super class
                skip(in, 2 * in.readUnsignedShort()); // interfaces
                skipMembers(in); // fields
                readMethods(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        List<String> declaredMethodNames() {
            return declaredMethodNames;
        }

        Set<String> invokedMethodNames() {
            Set<String> result = new HashSet<>();
            for (int i = 1; i < tags.length; i++) {
                if (tags[i] == CONSTANT_METHODREF) {
                    // Methodref -> NameAndType -> name
                    result.add(strings[first[second[i]]]);
                }
            }
            return result;
        }

        private void readMethods(DataInputStream in) throws IOException {
            int count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                in.readUnsignedShort(); // access flags
                declaredMethodNames.add(strings[in.readUnsignedShort()]);
                in.readUnsignedShort(); // descriptor
                skipAttributes(in);
            }
        }

        private static void skipMembers(DataInputStream in) throws IOException {
            int count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                skip(in, 6);
                skipAttributes(in);
            }
        }

        private static void skipAttributes(DataInputStream in) throws IOException {
            int count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                in.readUnsignedShort(); // name
                skip(in, in.readInt());
            }
        }

        private static void skip(DataInputStream in, long n) throws IOException {
            long remaining = n;
            while (remaining > 0) {
                long skipped = in.skip(remaining);
                if (skipped <= 0) {
                    in.readByte();
                    remaining--;
                } else {
                    remaining -= skipped;
                }
            }
        }
    }
}
