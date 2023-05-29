/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;

/**
 * Test of strict mode APIs.
 *
 * @author Norris Boyd
 */
public class StrictModeApiTest {

    private ScriptableObject global;
    private ContextFactory contextFactory;

    static class MyContextFactory extends ContextFactory {
        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            switch (featureIndex) {
                case Context.FEATURE_STRICT_MODE:
                case Context.FEATURE_STRICT_VARS:
                case Context.FEATURE_STRICT_EVAL:
                case Context.FEATURE_WARNING_AS_ERROR:
                    return true;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }

    @Test
    public void strictModeError() {
        contextFactory = new MyContextFactory();
        try (Context cx = contextFactory.enterContext()) {
            global = cx.initStandardObjects();
            try {
                runScript("({}.nonexistent);");
                fail();
            } catch (EvaluatorException e) {
                assertTrue(e.getMessage().startsWith("Reference to undefined property"));
            }
        }
    }

    private Object runScript(final String scriptSourceText) {
        return this.contextFactory.call(
                context ->
                        context.evaluateString(global, scriptSourceText, "test source", 1, null));
    }
}
