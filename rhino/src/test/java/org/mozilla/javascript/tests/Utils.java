/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Misc utilities to make test code easier.
 *
 * @author Marc Guillemot
 * @author Ronald Brill
 */
public class Utils {
    /** Runs the action successively with all available optimization levels */
    public static void runWithAllOptimizationLevels(final ContextAction<?> action) {
        runWithMode(action, false);
        runWithMode(action, true);
    }

    /** Runs the action successively with all available optimization levels */
    public static void runWithAllOptimizationLevels(
            final ContextFactory contextFactory, final ContextAction<?> action) {
        runWithMode(contextFactory, action, false);
        runWithMode(contextFactory, action, true);
    }

    /** Runs the provided action at the given interpretation mode */
    public static void runWithMode(final ContextAction<?> action, final boolean interpretedMode) {
        runWithMode(new ContextFactory(), action, interpretedMode);
    }

    /** Runs the provided action at the given optimization level */
    public static void runWithMode(
            final ContextFactory contextFactory,
            final ContextAction<?> action,
            final boolean interpretedMode) {
        try (final Context cx = contextFactory.enterContext()) {
            cx.setInterpretedMode(interpretedMode);
            action.run(cx);
        }
    }

    /**
     * Execute the provided script in a fresh context as "myScript.js".
     *
     * @param script the script code
     */
    static void executeScript(String script, boolean interpreted) {
        Utils.runWithMode(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    return cx.evaluateString(scope, script, "myScript.js", 1, null);
                },
                interpreted);
    }

    public static void assertWithAllOptimizationLevels(final Object expected, final String script) {
        runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    final Object res = cx.evaluateString(scope, script, "test.js", 0, null);

                    assertEquals(expected, res);
                    return null;
                });
    }

    public static void assertWithAllOptimizationLevelsES6(
            final Object expected, final String script) {
        runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();
                    final Object res = cx.evaluateString(scope, script, "test.js", 0, null);

                    assertEquals(expected, res);
                    return null;
                });
    }
}
