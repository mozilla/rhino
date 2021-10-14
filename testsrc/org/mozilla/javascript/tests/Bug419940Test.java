/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=419940
 *
 * @author Norris Boyd
 */
public class Bug419940Test extends TestCase {
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

    public void testAdapter() {
        String source = "(new JavaAdapter(" + Foo.class.getName() + ", {})).doSomething();";

        Context cx = ContextFactory.getGlobal().enterContext();
        try {
            Scriptable scope = cx.initStandardObjects();
            Object result = cx.evaluateString(scope, source, "source", 1, null);
            assertEquals(Integer.valueOf(value), result);
        } finally {
            Context.exit();
        }
    }
}
