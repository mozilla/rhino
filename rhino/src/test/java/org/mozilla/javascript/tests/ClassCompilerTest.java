/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.DefiningClassLoader;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.optimizer.ClassCompiler;

public class ClassCompilerTest {
    private static String SOURCE =
            "function f(str) { return (s) => (str + s); }\n"
                    + "\n"
                    + "function g() {\n"
                    + "    function h() {\n"
                    + "        function i() {\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
                    + "\n"
                    + "java.lang.System.out.println(f('hi, ')('mom!'));\n";

    @Test
    public void testClassesCompile() {
        var compilerEnv = new CompilerEnvirons();
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        var result = compiler.compileToClassFiles(SOURCE, "test", 0, "test");
        assertTrue(result.length > 0, "Expected > 0 entries in result array");
        assertTrue(result.length % 2 == 0, "Expected even number of results");
        boolean foundMain = false;
        for (int i = 0; i < result.length; i += 2) {
            if ("test".equals(result[i])) {
                foundMain = true;
            }
            assertTrue(
                    result[i] instanceof String,
                    String.format("Name at offset %d should be a string", i));
            assertTrue(
                    result[i + 1] instanceof byte[],
                    String.format("Name at offset %d should be a byte[]", i + 1));
        }
        assertTrue(foundMain, "Expected an entry for our main class");
    }

    @Test
    public void testClassesLoadAndLink() {
        var compilerEnv = new CompilerEnvirons();
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        var result = compiler.compileToClassFiles(SOURCE, "test", 0, "test");
        var loader = new DefiningClassLoader();

        var classes = new ArrayList<Class<?>>();

        for (int i = 0; i < result.length; i += 2) {
            Class<?> cl = loader.defineClass((String) result[i], (byte[]) result[i + 1]);
            classes.add(cl);
        }

        for (var cl : classes) {
            loader.linkClass(cl);
        }
    }

    @Test
    public void testMainMethodExecutesWithoutError()
            throws IllegalAccessException, InvocationTargetException {
        var compilerEnv = new CompilerEnvirons();
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        var result = compiler.compileToClassFiles(SOURCE, "test", 0, "test");
        var loader = new DefiningClassLoader();

        var classes = new ArrayList<Class<?>>();

        for (int i = 0; i < result.length; i += 2) {
            Class<?> cl = loader.defineClass((String) result[i], (byte[]) result[i + 1]);
            classes.add(cl);
        }

        for (var cl : classes) {
            loader.linkClass(cl);
        }

        Method main = null;
        for (var cl : classes) {
            try {
                main = cl.getMethod("main", String[].class);
            } catch (NoSuchMethodException e) {
            }
        }
        assertTrue(main != null, "Expected a main method");
        main.invoke(null, (Object) new String[0]);
    }

    /** Runs the compiled script and keeps its result, so tests can check it. */
    public static class ResultRunner {
        static Object result;

        public static void main(Script script, String[] args) {
            try (Context cx = Context.enter()) {
                var scope = cx.initStandardObjects();
                result = Context.toString(script.exec(cx, scope, scope));
            }
        }
    }

    private static Object runMain(String source) throws Exception {
        var compilerEnv = new CompilerEnvirons();
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        compiler.setMainMethodClass(ResultRunner.class.getName());
        var result = compiler.compileToClassFiles(source, "test", 0, "test");
        var loader = new DefiningClassLoader(ClassCompilerTest.class.getClassLoader());

        var classes = new ArrayList<Class<?>>();
        for (int i = 0; i < result.length; i += 2) {
            classes.add(loader.defineClass((String) result[i], (byte[]) result[i + 1]));
        }
        for (var cl : classes) {
            loader.linkClass(cl);
        }

        ResultRunner.result = null;
        Method main = loader.loadClass("testMain").getMethod("main", String[].class);
        main.invoke(null, (Object) new String[0]);
        return ResultRunner.result;
    }

    @Test
    public void regExpLiteralsWork() throws Exception {
        assertEquals(
                "a+b+c|true|x",
                runMain(
                        "function f(s) { return /y/.test(s); }\n"
                                + "'a-b-c'.replace(/-/g, '+') + '|' + f('xyz') + '|'"
                                + " + 'x1'.replace(/\\d/, '')"));
    }

    @Test
    public void taggedTemplateLiteralsWork() throws Exception {
        assertEquals(
                "a\\n|b",
                runMain(
                        "function tag(s) { return s.raw[0]; }\n"
                                + "function g() { return tag`b`; }\n"
                                + "tag`a\\n` + '|' + g()"));
    }
}
