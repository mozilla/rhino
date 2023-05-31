/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.InputStreamReader;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class Issue176Test {

    Context cx;
    Scriptable scope;

    @Test
    public void throwing() throws Exception {
        cx = Context.enter();
        try {
            InputStreamReader in =
                    new InputStreamReader(Bug482203Test.class.getResourceAsStream("Issue176.js"));
            Script script = cx.compileReader(in, "Issue176.js", 1, null);
            scope = cx.initStandardObjects();
            scope.put("host", scope, this);
            script.exec(cx, scope); // calls our methods
        } finally {
            Context.exit();
        }
    }

    public void throwError(String msg) {
        throw ScriptRuntime.throwError(cx, scope, msg);
    }

    public void throwCustomError(String constr, String msg) {
        throw ScriptRuntime.throwCustomError(cx, scope, constr, msg);
    }
}
