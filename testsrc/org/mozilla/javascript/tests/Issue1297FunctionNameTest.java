/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Test that we can redefine a function's name.
 */
public class Issue1297FunctionNameTest {
	private static final String source = "'use strict';" +
			"function X() {};\n" +
			"Object.defineProperty(X, 'name', {value: 'y', configurable: true, writable: true});" +
			"X.name";

	@Test
	public void canSetFunctionNameInCompiledClasses() {
		try (Context cx = Context.enter()) {
			Scriptable scope = cx.initStandardObjects(null);
			Object result = cx.evaluateString(scope,
					source,
					"test", 1, null);
			assertEquals("y", result);
		}
	}

	@Test
	public void canSetFunctionNameInInterpreter() {
		try (Context cx = Context.enter()) {
			cx.setOptimizationLevel(-1);
			Scriptable scope = cx.initStandardObjects(null);
			Object result = cx.evaluateString(scope,
					source,
					"test", 1, null);
			assertEquals("y", result);
		}
	}
}
