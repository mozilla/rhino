/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.EvaluatorException;

/**
 * Test of strict mode APIs.
 *
 * @author Norris Boyd
 */
public class StrictModeApiTest extends TestCase {

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

  public void testStrictModeError() {
    contextFactory = new MyContextFactory();
    Context cx = contextFactory.enterContext();
    try {
        global = cx.initStandardObjects();
        try {
            runScript("({}.nonexistent);");
            fail();
        } catch (EvaluatorException e) {
            assertTrue(e.getMessage().startsWith("Reference to undefined property"));
        }
    } finally {
        Context.exit();
    }
  }

  private Object runScript(final String scriptSourceText) {
    return this.contextFactory.call(new ContextAction() {
      public Object run(Context context) {
          return context.evaluateString(global, scriptSourceText,
                  "test source", 1, null);
      }
    });
  }
}
