package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test cases for the {@link ScriptRuntime} support for ScriptableObject#equivalentValues(Object)
 * method.
 *
 * @author Ronald Brill
 */
public class ScriptRuntimeEquivalentValuesTest {

    @Test
    public void equivalentValuesUndefined() throws Exception {
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    try {
                        ScriptableObject.defineClass(scope, EquivalentTesterObject.class);
                    } catch (Exception e) {
                    }

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var o = new EquivalentTesterObject();"
                                            + "'' + (o == undefined) + ' ' + (undefined == o)",
                                    "test",
                                    1,
                                    null);
                    assertEquals("" + cx.isInterpretedMode(), "true true", result);

                    return null;
                });
    }

    @Test
    public void equivalentValuesNull() throws Exception {
        Utils.runWithAllModes(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    try {
                        ScriptableObject.defineClass(scope, EquivalentTesterObject.class);
                    } catch (Exception e) {
                    }

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var o = new EquivalentTesterObject();"
                                            + "'' + (o == null) + ' ' + (null == o)",
                                    "test",
                                    1,
                                    null);
                    assertEquals("" + cx.isInterpretedMode(), "true true", result);

                    return null;
                });
    }

    public static class EquivalentTesterObject extends ScriptableObject {

        public EquivalentTesterObject() {}

        @Override
        public String getClassName() {
            return "EquivalentTesterObject";
        }

        @JSConstructor
        public void jsConstructorMethod() {}

        @Override
        protected Object equivalentValues(final Object value) {
            if (value == null || Undefined.isUndefined(value)) {
                return Boolean.TRUE;
            }

            return super.equivalentValues(value);
        }
    }
}
