/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.optimizer.ClassCompiler;

/**
 * Code compiled ahead of time to class files is loaded without the step that attaches a position
 * table, so its line numbers have to stand on their own.
 */
public class AheadOfTimePositionTest {

    /** Several statements on one line, which is what would otherwise call for position markers. */
    private static final String SOURCE =
            "function f() { var a = 1; var b = 2; throw new Error(a + b); }\nf();";

    @Test
    public void classFilesCarryOnlyRealLineNumbers() throws IOException {
        var compilerEnv = new CompilerEnvirons();
        compilerEnv.setLanguageVersion(Context.VERSION_ES6);
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        Object[] result = compiler.compileToClassFiles(SOURCE, "aot.js", 1, "AotScript");

        List<Integer> lines = new ArrayList<>();
        for (int i = 1; i < result.length; i += 2) {
            lines.addAll(lineNumberTableEntries((byte[]) result[i]));
        }

        assertTrue(!lines.isEmpty(), "expected some line numbers");
        for (int line : lines) {
            assertTrue(
                    line > 0 && line <= 2,
                    "the source has two lines, so "
                            + line
                            + " can only be a position marker, which nothing will decode for a "
                            + "class loaded from disk");
        }
    }

    /** Reads every {@code line_number} out of every LineNumberTable in a class file. */
    private static List<Integer> lineNumberTableEntries(byte[] classFile) throws IOException {
        List<Integer> out = new ArrayList<>();
        // Locating the attribute properly means walking the constant pool, so instead reuse the
        // parser the toolchain already has: javap-style parsing is overkill for a smoke test.
        // The LineNumberTable attribute name appears in the pool; find its index, then scan.
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(classFile));
        in.readInt(); // magic
        in.readUnsignedShort(); // minor
        in.readUnsignedShort(); // major
        int poolCount = in.readUnsignedShort();
        int lineNumberTableIndex = -1;
        for (int i = 1; i < poolCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: // Utf8
                    String s = in.readUTF();
                    if ("LineNumberTable".equals(s)) lineNumberTableIndex = i;
                    break;
                case 7: // Class
                case 8: // String
                case 16: // MethodType
                case 19: // Module
                case 20: // Package
                    in.skipBytes(2);
                    break;
                case 15: // MethodHandle
                    in.skipBytes(3);
                    break;
                case 3: // Integer
                case 4: // Float
                case 9: // Fieldref
                case 10: // Methodref
                case 11: // InterfaceMethodref
                case 12: // NameAndType
                case 17: // Dynamic
                case 18: // InvokeDynamic
                    in.skipBytes(4);
                    break;
                case 5: // Long
                case 6: // Double
                    in.skipBytes(8);
                    i++; // takes two pool entries
                    break;
                default:
                    throw new IOException("unexpected constant pool tag " + tag);
            }
        }
        if (lineNumberTableIndex < 0) return out;

        in.readUnsignedShort(); // access flags
        in.readUnsignedShort(); // this class
        in.readUnsignedShort(); // super class
        int interfaces = in.readUnsignedShort();
        in.skipBytes(interfaces * 2);
        for (int pass = 0; pass < 2; pass++) { // fields, then methods
            int count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                in.skipBytes(6); // access, name, descriptor
                readAttributes(in, lineNumberTableIndex, out);
            }
        }
        return out;
    }

    private static void readAttributes(
            DataInputStream in, int lineNumberTableIndex, List<Integer> out) throws IOException {
        int attributes = in.readUnsignedShort();
        for (int i = 0; i < attributes; i++) {
            int nameIndex = in.readUnsignedShort();
            int length = in.readInt();
            if (nameIndex == lineNumberTableIndex) {
                int entries = in.readUnsignedShort();
                for (int e = 0; e < entries; e++) {
                    in.readUnsignedShort(); // start_pc
                    out.add(in.readUnsignedShort());
                }
            } else {
                // Code attributes nest a LineNumberTable inside them.
                byte[] body = new byte[length];
                in.readFully(body);
                DataInputStream nested = new DataInputStream(new ByteArrayInputStream(body));
                if (length > 8) {
                    try {
                        nested.skipBytes(4); // max_stack, max_locals
                        int codeLength = nested.readInt();
                        if (codeLength >= 0 && codeLength <= body.length) {
                            nested.skipBytes(codeLength);
                            int exceptions = nested.readUnsignedShort();
                            nested.skipBytes(exceptions * 8);
                            readAttributes(nested, lineNumberTableIndex, out);
                        }
                    } catch (IOException ignored) {
                        // Not a Code attribute; nothing to read.
                    }
                }
            }
        }
    }
}
