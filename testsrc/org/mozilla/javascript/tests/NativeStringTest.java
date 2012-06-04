/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 *
 */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.Scriptable;

/**
 * @author Marc Guillemot
 */
public class NativeStringTest extends TestCase {

	/**
	 * Test for bug #492359
	 * https://bugzilla.mozilla.org/show_bug.cgi?id=492359
	 * Calling generic String or Array functions without arguments was causing ArrayIndexOutOfBoundsException
	 * in 1.7R2
	 */
    public void testtoLowerCaseApply() {
        assertEvaluates("hello", "var x = String.toLowerCase; x.apply('HELLO')");
        assertEvaluates("hello", "String.toLowerCase('HELLO')"); // first patch proposed to #492359 was breaking this
    }

    private void assertEvaluates(final Object expected, final String source) {
        final ContextAction action = new ContextAction() {
            public Object run(Context cx) {
                final Scriptable scope = cx.initStandardObjects();
                final Object rep = cx.evaluateString(scope, source, "test.js",
                        0, null);
                assertEquals(expected, rep);
                return null;
            }
        };
        Utils.runWithAllOptimizationLevels(action);
    }
 }
