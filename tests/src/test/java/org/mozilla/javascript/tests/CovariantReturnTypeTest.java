/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.tools.shell.Global;

/**
 * If you inherit from an abstract class and narrow the return type, you will get both methods on
 * Class.getMethods() and in the bytecode.
 *
 * <p>See explanation of {@link Class#getMethods()} <i> There may be more than one method with a
 * particular name and parameter types in a class because while the Java language forbids a class to
 * declare multiple methods with the same signature but different return types, the Java virtual
 * machine does not. </i>
 *
 * <p>Note: It totally depends on the JVM, in which order the methods are returend, so we have a
 * class here, that tries to simulate that 5 times to increase the probability that rhino will not
 * detect at least one case as bean property
 *
 * @author Roland Praml
 */
public class CovariantReturnTypeTest {

    public abstract static class A {
        public abstract Object getValue1();

        public abstract Object getValue2();

        public abstract Object getValue3();

        public abstract Object getValue4();

        public abstract Object getValue5();
    }

    public static class B extends A {
        private Integer value1;
        private Integer value2;
        private Integer value3;
        private Integer value4;
        private Integer value5;

        // Note: Class B will inherit
        @Override
        public Integer getValue1() {
            return value1;
        }

        public void setValue1(Integer value1) {
            this.value1 = value1;
        }

        @Override
        public Integer getValue2() {
            return value2;
        }

        public void setValue2(Integer value2) {
            this.value2 = value2;
        }

        @Override
        public Integer getValue3() {
            return value3;
        }

        public void setValue3(Integer value3) {
            this.value3 = value3;
        }

        @Override
        public Integer getValue4() {
            return value4;
        }

        public void setValue4(Integer value4) {
            this.value4 = value4;
        }

        @Override
        public Integer getValue5() {
            return value5;
        }

        public void setValue5(Integer value5) {
            this.value5 = value5;
        }
    }

    static final String LS = System.getProperty("line.separator");

    @Test
    public void checkIt() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Global scope = new Global();
                    scope.init(cx);
                    scope.put("obj", scope, new B());
                    Object ret =
                            cx.evaluateString(
                                    scope,
                                    "obj.value1 = 1; obj.value2 = 2; obj.value3 = 3; obj.value4 = 4; obj.value5 = 5",
                                    "myScript.js",
                                    1,
                                    null);
                    return null;
                });
    }
}
