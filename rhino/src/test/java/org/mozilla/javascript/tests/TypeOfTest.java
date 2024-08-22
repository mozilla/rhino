/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Takes care that it's possible to customize the result of the typeof operator. See
 * https://bugzilla.mozilla.org/show_bug.cgi?id=463996 Includes fix and test for
 * https://bugzilla.mozilla.org/show_bug.cgi?id=453360
 *
 * @author Marc Guillemot
 */
public class TypeOfTest {

    public static class Foo extends ScriptableObject {
        private static final long serialVersionUID = -8771045033217033529L;
        private final String typeOfValue_;

        public Foo(final String _typeOfValue) {
            typeOfValue_ = _typeOfValue;
        }

        @Override
        public String getTypeOf() {
            return typeOfValue_;
        }

        @Override
        public String getClassName() {
            return "Foo";
        }
    }

    /** ECMA 11.4.3 says that typeof on host object is Implementation-dependent */
    @Test
    public void customizeTypeOf() throws Exception {
        doTest("object", "typeof myObj", new Foo("object"));
        doTest("blabla", "typeof myObj", new Foo("blabla"));
    }

    /** ECMA 11.4.3 says that typeof on host object is Implementation-dependent */
    @Test
    public void test0() throws Exception {
        final Function f =
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context _cx, Scriptable _scope, Scriptable _thisObj, Object[] _args) {
                        return _args[0].getClass().getName();
                    }
                };
        doTest("function", "typeof myObj", f);
    }

    /** See https://bugzilla.mozilla.org/show_bug.cgi?id=453360 */
    @Test
    public void bug453360() throws Exception {
        doTest("object", "typeof new RegExp();", null);
        doTest("object", "typeof /foo/;", null);
    }

    private static void doTest(String expected, final String script, final Scriptable obj) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    scope.put("myObj", scope, obj);

                    String res =
                            Context.toString(
                                    cx.evaluateString(scope, script, "test script", 1, null));
                    assertEquals(expected, res);

                    return null;
                });
    }
}
