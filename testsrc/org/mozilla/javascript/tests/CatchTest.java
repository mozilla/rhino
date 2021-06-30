/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.IOException;
import junit.framework.TestCase;
import org.mozilla.javascript.*;

public class CatchTest extends TestCase {
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

    public void testCatchWrappedException() throws Exception {
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

        return (String)
                factory.call(
                        context -> {
                            context.setOptimizationLevel(-1);
                            if (shutter != null) {
                                context.setClassShutter(shutter);
                            }
                            final Scriptable scope = context.initStandardObjects();

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
}
