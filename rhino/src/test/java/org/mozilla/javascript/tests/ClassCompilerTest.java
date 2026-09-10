/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.DefiningClassLoader;
import org.mozilla.javascript.optimizer.ClassCompiler;

public class ClassCompilerTest {
    public static Collection<Object[]> testSources() {
        var res = new ArrayList<Object[]>();
        res.add(
                new Object[] {
                    """
                function f(str) { return (s) => (str + s); }

                function g() {
                  function h() {
                    function i() {
                    }
                  }
                }

                java.lang.System.out.println(f('hi, ')('mom!'));
                """
                });
        res.add(
                new Object[] {
                    """
                function f(str) { return (s) => (str.replaceAll(/i/g, 'ello'), + s); }

                function g() {
                  function h() {
                    function i() {
                    }
                  }
                }

                java.lang.System.out.println(f('hi, ')('mom!'));
                """
                });
        res.add(
                new Object[] {
                    """
                function f(str) { return (s) => (`${str}${s}`); }

                function g() {
                  function h() {
                    function i() {
                    }
                  }
                }

                java.lang.System.out.println(f('hi, ')('mom!'));
                """
                });
        res.add(
                new Object[] {
                    """
                function f(str) { return (s) => (greeting`${str}, ${s}`); }
                function greeting(strings, greet, person) {
                  return greet + strings[1] + person;
                }
                function g() {
                  function h() {
                    function i() {
                    }
                  }
                }

                java.lang.System.out.println(f('hi')('mom!'));
                """
                });
        return res;
    }

    @ParameterizedTest
    @MethodSource("testSources")
    public void testClassesCompile(String source) {
        var compilerEnv = new CompilerEnvirons();
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        var result = compiler.compileToClassFiles(source, "test", 0, "test");
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

    @ParameterizedTest
    @MethodSource("testSources")
    public void testClassesLoadAndLink(String source) {
        var compilerEnv = new CompilerEnvirons();
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        var result = compiler.compileToClassFiles(source, "test", 0, "test");
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

    @ParameterizedTest
    @MethodSource("testSources")
    public void testMainMethodExecutesWithoutError(String source)
            throws IllegalAccessException, InvocationTargetException {
        var compilerEnv = new CompilerEnvirons();
        ClassCompiler compiler = new ClassCompiler(compilerEnv);
        var result = compiler.compileToClassFiles(source, "test", 0, "test");
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
}
