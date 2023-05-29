/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

/** There is some special handling for document.all */
public class AvoidObjectDetectionTest {

    public static class Foo extends ScriptableObject {
        private static final long serialVersionUID = -6284330659327161113L;

        @Override
        public String getClassName() {
            return "Foo";
        }
    }

    public static class Avoid extends ScriptableObject {
        private static final long serialVersionUID = -1975590541658828651L;

        @Override
        public boolean avoidObjectDetection() {
            return true;
        }

        @Override
        public String getClassName() {
            return "Avoid";
        }
    }

    /**
     * make sure ScriptRuntime.toBoolean(document.all) and new Boolean(document.all) are returning
     * the same see Note on page
     * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Boolean
     *
     * @throws Exception in case of errors
     */
    @Test
    public void ctor() throws Exception {
        final ContextFactory factory = new ContextFactory();

        try (Context cx = factory.enterContext()) {
            final ScriptableObject topScope = cx.initStandardObjects();
            ScriptableObject.defineClass(topScope, Foo.class);
            ScriptableObject.defineClass(topScope, Avoid.class);

            Boolean toBoolean =
                    (Boolean)
                            cx.evaluateString(
                                    topScope,
                                    "if (new Foo()) true; else false;",
                                    "myScript",
                                    1,
                                    null);
            assertTrue(toBoolean);

            toBoolean =
                    (Boolean)
                            cx.evaluateString(
                                    topScope,
                                    "if (new Avoid()) true; else false;",
                                    "myScript",
                                    1,
                                    null);
            assertFalse(toBoolean);

            Boolean ctor =
                    (Boolean)
                            cx.evaluateString(topScope, "Boolean(new Foo())", "myScript", 1, null);
            assertTrue(ctor);

            ctor =
                    (Boolean)
                            cx.evaluateString(
                                    topScope, "Boolean(new Avoid())", "myScript", 1, null);
            assertFalse(ctor);
        }
    }
}
