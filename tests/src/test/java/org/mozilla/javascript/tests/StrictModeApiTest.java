/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import org.junit.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test of strict mode APIs.
 *
 * @author Norris Boyd
 */
public class StrictModeApiTest {

    @Test
    public void strictModeError() {
        final ContextFactory contextFactory =
                Utils.contextFactoryWithFeatures(
                        Context.FEATURE_STRICT_MODE,
                        Context.FEATURE_STRICT_VARS,
                        Context.FEATURE_STRICT_EVAL,
                        Context.FEATURE_WARNING_AS_ERROR);

        Utils.assertException(
                contextFactory,
                Context.VERSION_ES6,
                EvaluatorException.class,
                "Reference to undefined property",
                "({}.nonexistent);");
    }

    /** Unit test for bug 604674 https://bugzilla.mozilla.org/show_bug.cgi?id=604674 */
    @Test
    public void onlyGetterError() {
        final String script = "o.readonlyProp = 123";

        Utils.runWithAllModes(
                Utils.contextFactoryWithFeatures(Context.FEATURE_STRICT_MODE),
                cx -> {
                    try {
                        Scriptable scope = cx.initSafeStandardObjects();
                        final MyHostObject prototype = new MyHostObject();
                        ScriptableObject.defineClass(scope, MyHostObject.class);
                        final Method readMethod = MyHostObject.class.getMethod("jsxGet_x");
                        prototype.defineProperty(
                                "readonlyProp", null, readMethod, null, ScriptableObject.EMPTY);

                        ScriptableObject.defineProperty(
                                scope, "o", prototype, ScriptableObject.DONTENUM);

                        cx.evaluateString(scope, script, "test_script", 1, null);
                        throw new RuntimeException("Should have failed!");
                    } catch (final EcmaError e) {
                        assertEquals(
                                "TypeError: Cannot set property [MyHostObject].readonlyProp that has only a getter to value '123'. (test_script#1)",
                                e.getMessage());
                        return null;
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static class MyHostObject extends ScriptableObject {
        private int x;

        @Override
        public String getClassName() {
            return getClass().getSimpleName();
        }

        public int jsxGet_x() {
            return x;
        }
    }
}
