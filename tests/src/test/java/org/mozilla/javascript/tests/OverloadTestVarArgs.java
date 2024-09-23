/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 *
 */
package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.assertEquals;

public class OverloadTestVarArgs {

	public String args(String arg1) {
		return "args(String arg1)";
	}

	public String args(String arg1, int integer) {
		return "args(String arg1, int integer)";
	}

	public String args(String arg1, String arg2) {
		return "args(String arg1, String arg2)";
	}

	public String args(String arg1, String... args) {
		return "args(String arg1, String... args)";
	}

	public String args2(String arg1, String... args) {
		return "args2(String arg1, String... args)";
	}

	public String args2(String arg1, String arg2) {
		return "args2(String arg1, String arg2)";
	}

	public String args2(String arg1) {
		return "args2(String arg1)";
	}

	public String args3(String arg1, int... integers) {
		return "args3(String arg1, int... integers)";
	}

	public String args3(String arg1, String... strings) {
		return "args3(String arg1, String... strings)";
	}

	public String args3(String arg1, String arg2) {
		return "args3(String arg1, String arg2)";
	}

	public String args4(String arg1, Object arg2) {
		return "args4(String arg1, Object arg2)";
	}

	public String args4(String arg1, Object... objs) {
		return "args4(String arg1, Object... objs)";
	}


	@Test
	public void argsTest1() {
		assertEvaluates("args(String arg1)", this.args("foo"), "self.args('foo');");
	}
	@Test
	public void argsTest2() {
		assertEvaluates(
				"args(String arg1, String arg2)",
				this.args("foo", "bar"),
				"self.args('foo', 'bar');");
	}
	@Test
	public void argsTest3() {
		assertEvaluates(
				"args(String arg1, String... args)",
				this.args("foo", "bar", "baz"),
				"self.args('foo', 'bar', 'baz');");
	}
	@Test
	public void argsTest4() {
		assertEvaluates(
				"args(String arg1, int integer)", this.args("foo", 1), "self.args('foo', 1);");
	}
	@Test
	public void argsTest5() {
		assertEvaluates("args2(String arg1)", this.args2("foo"), "self.args2('foo');");
	}
	@Test
	public void argsTest6() {
		assertEvaluates(
				"args2(String arg1, String arg2)",
				this.args2("foo", "bar"),
				"self.args2('foo', 'bar');");
	}
	@Test
	public void argsTest7() {
		assertEvaluates(
				"args2(String arg1, String... args)",
				this.args2("foo", "bar", "baz"),
				"self.args2('foo', 'bar', 'baz');");
	}
	@Test
	public void argsTest8() {
		assertEvaluates(
				"args3(String arg1, String arg2)",
				this.args3("foo", "bar"),
				"self.args3('foo', 'bar');");
	}
	@Test
	public void argsTest9() {
		assertEvaluates(
				"args3(String arg1, int... integers)",
				this.args3("foo", 1),
				"self.args3('foo', 1);");
	}
	@Test
	public void argsTest10() {
		int[] intArr = new int[]{1, 2, 3};
		assertEvaluates(
				"args3(String arg1, int... integers)",
				this.args3("foo", intArr),
				"self.args3('foo', arrayArg);",
				intArr);
	}
	@Test
	public void argsTest11() {
		String[] stringArr = new String[]{"foo", "bar", "baz"};
		assertEvaluates(
				"args3(String arg1, String... strings)",
				this.args3("foo", stringArr),
				"self.args3('foo', arrayArg);",
				stringArr);
	}
	@Test
	public void argsTest12() {
		String[] stringArr = new String[]{"foo", "bar", "baz"};
		assertEvaluates(
				"args4(String arg1, Object... objs)",
				this.args4("foo", stringArr),
				"self.args4('foo', arrayArg);",
				stringArr);
	}
	@Test
	public void argsTest13() {
		Object[] objectArr = new Object[]{"foo", "bar", "baz"};
		assertEvaluates(
				"args4(String arg1, Object... objs)",
				this.args4("foo", objectArr),
				"self.args4('foo', arrayArg);",
				objectArr);
	}

	@Test
	public void argsTest14() {
		String[] stringArr = new String[]{"foo", "bar", "baz"};
		assertEvaluates(
				"args4(String arg1, Object... objs)",
				this.args4("foo", stringArr, "bar"),
				"self.args4('foo', arrayArg, 'bar');",
				stringArr);
	}
	@Test
	public void argsTest15() {
		Object[] objectArr = new Object[]{"foo", "bar", "baz"};
		assertEvaluates(
				"args4(String arg1, Object... objs)",
				this.args4("foo", objectArr, "bar"),
				"self.args4('foo', arrayArg, 'bar');",
				objectArr);
	}

	private void assertEvaluates(final Object expected, String javaResult, final String source) {
		assertEvaluates(expected, javaResult, source, null);
	}

	private void assertEvaluates(
			final Object expected, String javaResult, final String source, Object arg) {
		assertEquals(expected, javaResult);
		Utils.runWithAllOptimizationLevels(
				cx -> {
					final Scriptable scope = cx.initStandardObjects();
					scope.put("self", scope, this);
					scope.put("arrayArg", scope, arg);
					final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
					assertEquals(expected, Context.jsToJava(rep, String.class));
					return null;
				});
	}
}
