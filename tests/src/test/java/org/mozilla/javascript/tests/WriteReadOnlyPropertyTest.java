/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptableObject;

/**
 * Test that read-only properties can be... set when needed. This was the standard behavior in Rhino
 * until 1.7R2 but has changed then. It is needed by HtmlUnit to simulate IE as well as FF2 (but not
 * FF3).
 *
 * @see <a href="https://bugzilla.mozilla.org/show_bug.cgi?id=519933">Rhino bug 519933</a>
 * @author Marc Guillemot
 */
public class WriteReadOnlyPropertyTest {

    /** @throws Exception if the test fails */
    @Test
    public void writeReadOnly_accepted() throws Exception {
        testWriteReadOnly(true);
    }

    /** @throws Exception if the test fails */
    @Test
    public void writeReadOnly_throws() throws Exception {
        try {
            testWriteReadOnly(false);
            Assert.fail();
        } catch (EcmaError e) {
            Assert.assertTrue(
                    e.getMessage(),
                    e.getMessage()
                            .contains(
                                    "Cannot set property [Foo].myProp that has only a getter to value '123'"));
        }
    }

    void testWriteReadOnly(final boolean acceptWriteReadOnly) throws Exception {
        final Method readMethod = Foo.class.getMethod("getMyProp", (Class[]) null);
        final Foo foo = new Foo("hello");
        foo.defineProperty("myProp", null, readMethod, null, ScriptableObject.EMPTY);

        final String script = "foo.myProp = 123; foo.myProp";

        final ContextFactory contextFactory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(final Context cx, final int featureIndex) {
                        if (Context.FEATURE_STRICT_MODE == featureIndex) {
                            return !acceptWriteReadOnly;
                        }
                        return super.hasFeature(cx, featureIndex);
                    }
                };
        contextFactory.call(
                cx -> {
                    final ScriptableObject top = cx.initStandardObjects();
                    ScriptableObject.putProperty(top, "foo", foo);

                    cx.evaluateString(top, script, "script", 0, null);
                    return null;
                });
    }

    /** Simple utility allowing to better see the concerned scope while debugging */
    static class Foo extends ScriptableObject {
        final String prop_;

        Foo(final String label) {
            prop_ = label;
        }

        @Override
        public String getClassName() {
            return "Foo";
        }

        public String getMyProp() {
            return prop_;
        }
    }
}
