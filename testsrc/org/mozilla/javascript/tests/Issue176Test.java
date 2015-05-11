/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Issue176Test extends TestCase {

    public static Issue176Test singleton;

    public Issue176Test() {
        this.singleton = this;
    }

    Context cx;
    Scriptable scope;

    public void testThrowing() throws Exception {
        cx = Context.enter();
        try {
            Script script = cx.compileReader(new InputStreamReader(
                    Bug482203Test.class.getResourceAsStream("Issue176.js")),
                    "", 1, null);
            scope = cx.initStandardObjects();
            script.exec(cx, scope); // calls our static methods
        } finally {
            Context.exit();
        }
    }


    public static void throwError(String msg) {
        ScriptRuntime.throwError(singleton.cx, singleton.scope, msg);
    }


    public static void throwCustomg(String constr, String msg) {
        ScriptRuntime.throwCustomError(singleton.cx, singleton.scope, constr, msg);
    }

}
