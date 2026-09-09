/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.testutils.Utils;

/**
 * A class has one method that builds every template literal in it and one that compiles every
 * regexp literal in it, and neither may grow past the 64K of bytecode a method can hold, however
 * many literals the file has.
 *
 * <p>The literals are spread over several functions so that nothing but those two methods can be
 * what grows too big.
 */
public class InitMethodSizeTest {

    /** Together, more literals than one init method can hold. */
    private static final int FUNCTIONS = 50;

    private static final int PER_FUNCTION = 100;

    @Test
    public void manyTemplateLiteralsCompile() {
        assertDoesNotThrow(() -> compile(templateLiterals("")));
    }

    @Test
    public void manyTemplateLiteralsEvaluate() {
        Utils.assertWithAllModes_ES6(
                "value 0-99\n|value 0-99\\n;value 49-99\n|value 49-99\\n",
                templateLiterals("[f0(), f49()].join(';');"));
    }

    @Test
    public void manyRegExpLiteralsCompile() {
        assertDoesNotThrow(() -> compile(regExpLiterals("")));
    }

    @Test
    public void manyRegExpLiteralsEvaluate() {
        Utils.assertWithAllModes_ES6(
                "value 0-99;value 49-99", regExpLiterals("[f0(), f49()].join(';');"));
    }

    private static String templateLiterals(String tail) {
        StringBuilder source =
                new StringBuilder("function tag(s) { return s[0] + '|' + s.raw[0]; }\n");
        for (int f = 0; f < FUNCTIONS; f++) {
            source.append("function f").append(f).append("() { var last;");
            for (int i = 0; i < PER_FUNCTION; i++) {
                source.append("last = tag`value ").append(f).append('-').append(i).append("\\n`;");
            }
            source.append(" return last; }\n");
        }
        return source.append(tail).toString();
    }

    private static String regExpLiterals(String tail) {
        StringBuilder source = new StringBuilder();
        for (int f = 0; f < FUNCTIONS; f++) {
            source.append("function f").append(f).append("() { var last;");
            for (int i = 0; i < PER_FUNCTION; i++) {
                source.append("last = /value ").append(f).append('-').append(i).append("/g;");
            }
            source.append(" return last.source; }\n");
        }
        return source.append(tail).toString();
    }

    private static void compile(String source) {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        ScriptNode tree =
                new IRFactory(env, source)
                        .transformTree(new Parser(env).parse(source, "test.js", 1));
        new Codegen().compileScript(env, tree, source);
    }
}
