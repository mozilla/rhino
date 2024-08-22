/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Unit tests to check error handling. Especially, we expect to get a correct cause, when an error
 * happened in Java.
 *
 * @author Roland Praml
 */
public class AmbiguousMethodTest {

    static final String LS = System.getProperty("line.separator");

    public static String check1(Object o) {
        return "Object";
    }

    public static String check1(String o) {
        return "String";
    }

    public static String check1(Integer o) {
        return "Integer";
    }

    public static String check2(Object o) {
        return "Object";
    }

    public static String check2(Integer o) {
        return "Integer";
    }

    public static String check2(UUID o) {
        return "UUID";
    }

    public static String check3(Object o) {
        return "Object";
    }

    public static String check3(UUID o) {
        return "UUID";
    }

    public static String check4(UUID o) {
        return "UUID";
    }

    public static String check4(Integer o) {
        return "Integer";
    }

    @Test
    public void checkIt() {
        testIt("AmbiguousMethodTest.check1('foo')", "String");
        testIt("AmbiguousMethodTest.check1(1)", "Integer");
        testIt("AmbiguousMethodTest.check1(new Date())", "Object");

        testIt("AmbiguousMethodTest.check2('foo')", "Object");
        testIt("AmbiguousMethodTest.check2(1)", "Integer");
        testIt("AmbiguousMethodTest.check2(new Date())", "Object");

        testIt("AmbiguousMethodTest.check3('foo')", "Object");
        testIt("AmbiguousMethodTest.check3(1)", "Object");
        testIt("AmbiguousMethodTest.check3(new Date())", "Object");

        testIt("AmbiguousMethodTest.check1(undefined)", "String");
        testIt("AmbiguousMethodTest.check2(undefined)", "Object"); // Do not use Integer
        testIt("AmbiguousMethodTest.check3(undefined)", "Object"); // UUID is not a candidate

        testIt("AmbiguousMethodTest.check1(null)", "String");
        testIt("AmbiguousMethodTest.check2(null)", "Object"); // Do not use Integer
        testIt("AmbiguousMethodTest.check3(null)", "Object"); // UUID is not a candidate

        testIt("AmbiguousMethodTest.check4(1)", "Integer");
        testIt("AmbiguousMethodTest.check4(java.util.UUID.randomUUID())", "UUID");
        try {
            testIt("AmbiguousMethodTest.check4(null)", "Object");
            Assert.fail();
        } catch (EvaluatorException e) {
            Assert.assertTrue(
                    e.getMessage()
                            .contains("matching JavaScript argument types (null) is ambiguous"));
        }
        // There is currently no way in Rhino to invoke a certain method. Ideas:
        // 1. testIt("AmbiguousMethodTest.check4(java.lang.Integer.cast(null))", "Object");
        //    would require to add a virtual 'cast' method to NativeJavaClass that returns
        //    an object carrying type and value. (Currently most promising way)
        // 2a. testIt("AmbiguousMethodTest.check4((java.lang.Integer) null)", "Object");
        // 2b. testIt("AmbiguousMethodTest.check4<java.lang.Integer>(null)", "Object");
        //    would require to modify the parser. This means, we are no longer JS-syntax compatible
        // 3. testIt("AmbiguousMethodTest.check4__java_lang_Integer(null)", "Object");
        //    less invasive method, when searching for method. But makes code ugly and unreadable
    }

    private void testIt(final String script, final String expected) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Global scope = new Global();
                    scope.init(cx);
                    Object ret =
                            cx.evaluateString(
                                    scope,
                                    "importClass(Packages.org.mozilla.javascript.tests.AmbiguousMethodTest); "
                                            + script,
                                    "myScript.js",
                                    1,
                                    null);
                    if (ret instanceof Wrapper) {
                        ret = ((Wrapper) ret).unwrap();
                    }
                    Assert.assertEquals(expected, ret);
                    return null;
                });
    }
}
