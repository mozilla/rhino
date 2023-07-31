/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Scriptable;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=419940
 *
 * @author Norris Boyd
 */
public class Bug419940Test {
    static final int value = 12;

    public abstract static class BaseFoo {
        public abstract int doSomething();
    }

    public static class Foo extends BaseFoo {
        @Override
        public int doSomething() {
            return value;
        }
    }

    @Test
    public void adapter() {
        String source = "(new JavaAdapter(" + Foo.class.getName() + ", {})).doSomething();";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    Object result = cx.evaluateString(scope, source, "source", 1, null);
                    assertEquals(Integer.valueOf(value), result);

                    return null;
                });
    }
}
