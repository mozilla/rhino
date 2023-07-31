/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class ObjectIsPrototypeOfTest {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void isPrototypeOfUndefined() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Object.prototype.isPrototypeOf.call(undefined, []);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert undefined to an object.", result);
    }

    @Test
    public void isPrototypeOfNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Object.prototype.isPrototypeOf.call(null, []);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }
}
