/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import java.math.BigInteger;
import junit.framework.TestCase;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

public class NativeJavaObjectTest extends TestCase {

    @Test
    public void testCoerceType() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);

            Scriptable scope = cx.initStandardObjects();
            {
                String source = "java.util.Collections.singletonList('123').get(0)";
                Object result = cx.evaluateString(scope, source, "source", 1, null);
                assertTrue(result instanceof NativeJavaObject);
                Object rawObj = ((NativeJavaObject) result).unwrap();
                assertTrue(rawObj instanceof String);
                assertEquals("123", rawObj);
            }

            {
                String source = "java.util.Collections.singletonList(123).get(0)";
                Object result = cx.evaluateString(scope, source, "source", 1, null);
                assertTrue(result instanceof NativeJavaObject);
                Object rawObj = ((NativeJavaObject) result).unwrap();
                assertTrue(rawObj instanceof Double);
                assertEquals(Double.valueOf(123), rawObj);
            }

            {
                String source = "java.util.Collections.singletonList(123n).get(0)";
                Object result = cx.evaluateString(scope, source, "source", 1, null);
                assertTrue(result instanceof NativeJavaObject);
                Object rawObj = ((NativeJavaObject) result).unwrap();
                assertTrue(rawObj instanceof BigInteger);
                assertEquals(BigInteger.valueOf(123), rawObj);
            }

        } finally {
            Context.exit();
        }
    }
}
