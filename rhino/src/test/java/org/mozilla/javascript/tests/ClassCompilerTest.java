/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.DefiningClassLoader;
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
        assertTrue("Expected > 0 entries in result array", result.length > 0);
        assertTrue("Expected even number of results", result.length % 2 == 0);
        boolean foundMain = false;
        for (int i = 0; i < result.length; i += 2) {
            if ("test".equals(result[i])) {
                foundMain = true;
            }
            assertTrue(
                    String.format("Name at offset %d should be a string", i),
                    result[i] instanceof String);
            assertTrue(
                    String.format("Name at offset %d should be a byte[]", i + 1),
                    result[i + 1] instanceof byte[]);
        }
        assertTrue("Expected an entry for our main class", foundMain);
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
        assertTrue("Expected a main method", main != null);
        main.invoke(null, (Object) new String[0]);
    }
}
