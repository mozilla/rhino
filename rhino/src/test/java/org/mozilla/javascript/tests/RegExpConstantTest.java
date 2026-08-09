/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.DefiningClassLoader;
import org.mozilla.javascript.optimizer.ClassCompiler;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regexp literals are written to generated class files as dynamic constants, rather than compiled
 * into static fields when the class is defined.
 */
public class RegExpConstantTest {

    @Test
    public void literalsMatch() {
        Utils.assertWithAllModes_ES6("bar", "'foobarbaz'.match(/b.r/)[0]");
        Utils.assertWithAllModes_ES6(true, "/a[bc];/.test('xab;y')");
        Utils.assertWithAllModes_ES6("BAR", "'fooBARbaz'.match(/b[a-z]r/i)[0]");
        Utils.assertWithAllModes_ES6("b", "/(?<letter>b)/.exec('abc').groups.letter");
    }

    @Test
    public void repeatedLiteralsKeepSeparateState() {
        // The two literals share a compiled form, but each evaluation must produce a regexp with a
        // "lastIndex" of its own.
        Utils.assertWithAllModes_ES6(
                "0|2",
                "var a = /b/g, b = /b/g;\n"
                        + "a.exec('abcbd');\n"
                        + "'' + b.lastIndex + '|' + a.lastIndex");
    }

    @Test
    public void literalIsANewObjectEachTimeItIsEvaluated() {
        Utils.assertWithAllModes_ES6(
                false,
                "var previous = null, same = true;\n"
                        + "for (var i = 0; i < 2; i++) {\n"
                        + "  var re = /x/;\n"
                        + "  if (previous !== null) same = (previous === re);\n"
                        + "  previous = re;\n"
                        + "}\n"
                        + "same");
    }

    @Test
    public void literalsInFunctionsMatch() {
        Utils.assertWithAllModes_ES6(
                "a|b",
                "function f() { return 'xax'.match(/a/)[0]; }\n"
                        + "function g() { return 'xbx'.match(/b/)[0]; }\n"
                        + "f() + '|' + g()");
    }

    @Test
    public void invalidLiteralIsRejected() {
        // Compiled, this is reported while the constant is prepared, which is the whole reason
        // resolving it later can do without a context to report against.
        Utils.assertEcmaErrorES6(
                "SyntaxError: Invalid regular expression: The quantifier maximum '1' is less than"
                        + " the minimum '2'.",
                "var re = /a{2,1}/;");
    }

    @Test
    public void generatedClassHasNoRegExpInitializer() {
        Class<?> script = compileAndDefine("var re = /a[bc];/g; re.source").get(0);

        for (Method method : script.getDeclaredMethods()) {
            assertFalse(
                    method.getName().startsWith("_reInit"),
                    "the literal should be a constant rather than something initialized when the"
                            + " class is defined, but found "
                            + method.getName());
        }
        for (Field field : script.getDeclaredFields()) {
            assertFalse(
                    field.getName().startsWith("_re"),
                    "the literal should be a constant rather than a field, but found "
                            + field.getName());
        }
    }

    @Test
    public void aheadOfTimeCompiledLiteralRuns() throws Exception {
        // The script throws if the regexp does not work, and its constants resolve here, on a
        // thread with no context of its own and long after the class was compiled.
        var classes =
                compileAndDefine(
                        "if ('foobarbaz'.match(/b(.)r/i)[1] !== 'a') throw new Error('no match');");

        Method main = null;
        for (var cl : classes) {
            try {
                main = cl.getMethod("main", String[].class);
            } catch (NoSuchMethodException e) {
                // Only the generated main class has one.
            }
        }
        assertNotNull(main, "expected a main method");
        main.invoke(null, (Object) new String[0]);
    }

    /**
     * Compile a script the way the class compiler tool does and define the result, with the script
     * class itself first.
     */
    private static ArrayList<Class<?>> compileAndDefine(String source) {
        Object[] compiled;
        try (Context cx = Context.enter()) {
            CompilerEnvirons env = new CompilerEnvirons();
            env.initFromContext(cx);
            compiled = new ClassCompiler(env).compileToClassFiles(source, "test.js", 1, "test");
        }

        var loader = new DefiningClassLoader();
        var classes = new ArrayList<Class<?>>();
        for (int i = 0; i != compiled.length; i += 2) {
            classes.add(loader.defineClass((String) compiled[i], (byte[]) compiled[i + 1]));
        }
        for (var cl : classes) {
            loader.linkClass(cl);
        }
        return classes;
    }
}
