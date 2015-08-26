/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import junit.framework.TestCase;

/**
 * Test for overloaded varargs/non-varargs methods.
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=467396
 * @author Hannes Wallnoefer
 */
public class Bug467396Test extends TestCase {

    public void testOverloadedVarargs() {
        Context cx = ContextFactory.getGlobal().enterContext();
        try {
            Scriptable scope = cx.initStandardObjects();
            Object result = unwrap(cx.evaluateString(scope,
                    "java.lang.reflect.Array.newInstance(java.lang.Object, 1)",
                    "source", 1, null));
            assertTrue(result instanceof Object[]);
            assertEquals(1, ((Object[]) result).length);
            result = unwrap(cx.evaluateString(scope,
                    "java.lang.reflect.Array.newInstance(java.lang.Object, [1])",
                    "source", 1, null));
            assertTrue(result instanceof Object[]);
            assertEquals(1, ((Object[]) result).length);
            result = unwrap(cx.evaluateString(scope,
                    "java.lang.reflect.Array.newInstance(java.lang.Object, [1, 1])",
                    "source", 1, null));
            assertTrue(result instanceof Object[][]);
            assertEquals(1, ((Object[][]) result).length);
            assertEquals(1, ((Object[][]) result)[0].length);
            result = unwrap(cx.evaluateString(scope,
                    "java.lang.reflect.Array.newInstance(java.lang.Object, 1, 1)",
                    "source", 1, null));
            assertTrue(result instanceof Object[][]);
            assertEquals(1, ((Object[][]) result).length);
            assertEquals(1, ((Object[][]) result)[0].length);
        } finally {
            Context.exit();
        }
    }


    private Object unwrap(Object obj) {
        return obj instanceof Wrapper ? ((Wrapper) obj).unwrap() : obj;
    }
}
