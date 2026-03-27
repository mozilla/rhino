/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

/**
 * @author André Bargull
 */
public class Bug689308Test {

    private Context cx;

    @BeforeEach
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
    }

    @AfterEach
    public void tearDown() {
        Context.exit();
    }

    private AstRoot parse(CharSequence cs) {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.initFromContext(cx);
        ErrorReporter compilationErrorReporter = compilerEnv.getErrorReporter();
        Parser p = new Parser(compilerEnv, compilationErrorReporter);
        return p.parse(cs.toString(), "<eval>", 1);
    }

    private String toSource(CharSequence cs) {
        return parse(cs).toSource();
    }

    @Test
    public void toSourceArray() {
        assertEquals("[];\n", toSource("[]"));
        assertEquals("[,];\n", toSource("[,]"));
        assertEquals("[, ,];\n", toSource("[,,]"));
        assertEquals("[, , ,];\n", toSource("[,,,]"));

        assertEquals("[1];\n", toSource("[1]"));
        assertEquals("[1];\n", toSource("[1,]"));
        assertEquals("[, 1];\n", toSource("[,1]"));
        assertEquals("[1, 1];\n", toSource("[1,1]"));
    }
}
