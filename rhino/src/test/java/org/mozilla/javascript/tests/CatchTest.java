/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

public class CatchTest {
    public static class Foo extends ScriptableObject {
        private static final long serialVersionUID = -8771045033217033529L;

        public String jsFunction_bar() throws IOException {
            throw new IOException("oops");
        }

        @Override
        public String getClassName() {
            return "Foo";
        }
    }

    @Test
    public void catchWrappedException() throws Exception {
        String res = doCatchWrappedException(null);
        assertEquals("JavaException: java.io.IOException: oops", res);

        String res2 =
                doCatchWrappedException(
                        new ClassShutter() {
                            @Override
                            public boolean visibleToScripts(String className) {
                                return false;
                            }
                        });
        assertEquals("InternalError: oops", res2);
    }

    public String doCatchWrappedException(final ClassShutter shutter) throws Exception {

        ContextFactory factory = new ContextFactory();

        return factory.call(
                context -> {
                    context.setInterpretedMode(true);
                    if (shutter != null) {
                        context.setClassShutter(shutter);
                    }
                    TopLevel scope = context.initStandardObjects();

                    try {
                        ScriptableObject.defineClass(scope, Foo.class);
                        final Scriptable foo = context.newObject(scope, "Foo", null);
                        scope.put("foo", scope, foo);
                        return Context.toString(
                                context.evaluateString(
                                        scope,
                                        "var res; try { res = foo.bar(); } catch(e) { res = e; }",
                                        "test script",
                                        1,
                                        null));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    /**
     * Regression: a try/catch/finally where the catch block exits normally and the finally block
     * unconditionally returns. Confirms the finally body is executed once on the catch-normal-exit
     * path, even when an exception was caught and the finally body itself contains a return.
     */
    @Test
    public void tryCatchFinallyReturnsFromFinally() {
        String script =
                "function f(value) {"
                        + "  var result = 0;"
                        + "  try {"
                        + "    switch (value) {"
                        + "    case 1: result += 4; throw result; break;"
                        + "    case 4: result += 64; throw 'ex'; }"
                        + "    return result;"
                        + "  } catch (e) {"
                        + "    if ((value===1)&&(e!==4)) throw 'fail2.1: '+e;"
                        + "    if ((value===4)&&(e!=='ex')) throw 'fail2.2: '+e;"
                        + "  } finally {"
                        + "    return result;"
                        + "  }"
                        + "}"
                        + "f(1) + ',' + f(4)";
        Utils.assertWithAllModes("4,64", script);
    }
}
