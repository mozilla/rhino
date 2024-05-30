/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class OverloadTestVarArgs {

    public String args(String arg1) {
        return "arg1";
    }

    public String args(String arg1, String arg2) {
        return "arg1 + arg2";
    }

    public String args(String arg1, String... args) {
        return "arg1 + args";
    }

    public String args2(String arg1, String... args) {
        return "arg1 + args";
    }

    public String args2(String arg1, String arg2) {
        return "arg1 + arg2";
    }

    public String args2(String arg1) {
        return "arg1";
    }

    @Test
    public void argsTestJavaReference() {
        // this is java reference
        assertEquals("arg1", this.args("foo"));
        assertEquals("arg1 + arg2", this.args("foo", "bar"));
        assertEquals("arg1 + args", this.args("foo", "bar", "baz"));
    }

    @Test
    public void argsTestJs() {
        assertEvaluates("arg1", "self.args('foo');");
        assertEvaluates("arg1 + arg2", "self.args('foo', 'bar');");
        assertEvaluates("arg1 + args", "self.args('foo', 'bar', 'baz');");
    }

    @Test
    public void args2TestJs() {
        assertEvaluates("arg1", "self.args2('foo');");
        assertEvaluates("arg1 + arg2", "self.args2('foo', 'bar');");
        assertEvaluates("arg1 + args", "self.args2('foo', 'bar', 'baz');");
    }

    private void assertEvaluates(final Object expected, final String source) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    scope.put("self", scope, this);
                    final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                    assertEquals(expected, Context.jsToJava(rep, String.class));
                    return null;
                });
    }
}
