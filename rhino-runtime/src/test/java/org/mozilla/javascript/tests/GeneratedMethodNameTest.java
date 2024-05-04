/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * Takes care that the name of the method generated for a function "looks like" the original
 * function name. See https://bugzilla.mozilla.org/show_bug.cgi?id=460726
 *
 * @author Marc Guillemot
 */
public class GeneratedMethodNameTest {

    @Test
    public void standardFunction() throws Exception {
        final String scriptCode =
                "function myFunc() {\n"
                        + " var m = javaNameGetter.readCurrentFunctionJavaName();\n"
                        + "  if (m != 'myFunc') throw 'got '  + m;"
                        + "}\n"
                        + "myFunc();";
        doTest(scriptCode);
    }

    @Test
    public void functionDollar() throws Exception {
        final String scriptCode =
                "function $() {\n"
                        + " var m = javaNameGetter.readCurrentFunctionJavaName();\n"
                        + "  if (m != '$') throw 'got '  + m;"
                        + "}\n"
                        + "$();";
        doTest(scriptCode);
    }

    @Test
    public void scriptName() throws Exception {
        final String scriptCode =
                "var m = javaNameGetter.readCurrentFunctionJavaName();\n"
                        + "if (m != 'script') throw 'got '  + m;";
        doTest(scriptCode);
    }

    @Test
    public void constructor() throws Exception {
        final String scriptCode =
                "function myFunc() {\n"
                        + " var m = javaNameGetter.readCurrentFunctionJavaName();\n"
                        + "  if (m != 'myFunc') throw 'got '  + m;"
                        + "}\n"
                        + "new myFunc();";
        doTest(scriptCode);
    }

    @Test
    public void anonymousFunction() throws Exception {
        final String scriptCode =
                "var myFunc = function() {\n"
                        + " var m = javaNameGetter.readCurrentFunctionJavaName();\n"
                        + "  if (m != 'anonymous') throw 'got '  + m;"
                        + "}\n"
                        + "myFunc();";
        doTest(scriptCode);
    }

    public class JavaNameGetter {
        public String readCurrentFunctionJavaName() {
            final Throwable t = new RuntimeException();
            // remove prefix and suffix of method name
            return t.getStackTrace()[8].getMethodName().replaceFirst("_[^_]*_(.*)_[^_]*", "$1");
        }
    }

    public void doTest(final String scriptCode) throws Exception {
        try (Context cx = ContextFactory.getGlobal().enterContext()) {
            Scriptable topScope = cx.initStandardObjects();
            topScope.put("javaNameGetter", topScope, new JavaNameGetter());
            Script script = cx.compileString(scriptCode, "myScript", 1, null);
            script.exec(cx, topScope);
        }
    }
}
