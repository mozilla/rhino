/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;

/**
 * Misc utilities to make test code easier.
 *
 * @author Marc Guillemot
 * @author Ronald Brill
 */
public class Utils {
    /** The default set of levels to run tests at. */
    public static final int[] DEFAULT_OPT_LEVELS = new int[] {-1, 0, 9};

    /** Runs the action successively with all available optimization levels */
    public static void runWithAllOptimizationLevels(final ContextAction<?> action) {
        for (int level : getTestOptLevels()) {
            runWithOptimizationLevel(action, level);
        }
    }

    /** Runs the action successively with all available optimization levels */
    public static void runWithAllOptimizationLevels(
            final ContextFactory contextFactory, final ContextAction<?> action) {
        for (int level : getTestOptLevels()) {
            runWithOptimizationLevel(contextFactory, action, level);
        }
    }

    /** Runs the provided action at the given optimization level */
    public static void runWithOptimizationLevel(
            final ContextAction<?> action, final int optimizationLevel) {
        runWithOptimizationLevel(new ContextFactory(), action, optimizationLevel);
    }

    /** Runs the provided action at the given optimization level */
    public static void runWithOptimizationLevel(
            final ContextFactory contextFactory,
            final ContextAction<?> action,
            final int optimizationLevel) {

        try (final Context cx = contextFactory.enterContext()) {
            cx.setOptimizationLevel(optimizationLevel);
            action.run(cx);
        }
    }

    /**
     * Execute the provided script in a fresh context as "myScript.js".
     *
     * @param script the script code
     */
    static void executeScript(final String script, final int optimizationLevel) {
        Utils.runWithOptimizationLevel(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    return cx.evaluateString(scope, script, "myScript.js", 1, null);
                },
                optimizationLevel);
    }

    /**
     * If the TEST_OPTLEVEL system property is set, then return an array containing only that one
     * integer. Otherwise, return an array of the typical opt levels that we expect for testing.
     */
    public static int[] getTestOptLevels() {
        String overriddenLevel = System.getProperty("TEST_OPTLEVEL");
        if (overriddenLevel != null && !overriddenLevel.isEmpty()) {
            return new int[] {Integer.parseInt(overriddenLevel)};
        }
        return DEFAULT_OPT_LEVELS;
    }

    public static boolean isJavaVersionAtLeast(int desiredVersion) {
        String[] v = System.getProperty("java.version").split("\\.");
        int version = Integer.parseInt(v[0]);
        return version >= desiredVersion;
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

    public static void assertWithAllOptimizationLevels_1_8(
            final Object expected, final String script) {
        runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_1_8);
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

                    if (expected instanceof Integer && res instanceof Double) {
                        assertEquals(
                                ((Integer) expected).doubleValue(),
                                ((Double) res).doubleValue(),
                                0.00001);
                        return null;
                    }
                    if (expected instanceof Double && res instanceof Integer) {
                        assertEquals(
                                ((Double) expected).doubleValue(),
                                ((Integer) res).doubleValue(),
                                0.00001);
                        return null;
                    }

                    assertEquals(expected, res);
                    return null;
                });
    }

    public static void assertWithAllOptimizationLevelsTopLevelScopeES6(
            final Object expected, final String script) {
        runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects(new TopLevel());
                    final Object res = cx.evaluateString(scope, script, "test.js", 0, null);

                    assertEquals(expected, res);
                    return null;
                });
    }

    public static void assertEvaluatorExceptionES6(String expectedMessage, String js) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    try {
                        cx.evaluateString(scope, js, "test", 1, null);
                        fail("EvaluatorException expected");
                    } catch (EvaluatorException e) {
                        assertEquals(expectedMessage, e.getMessage());
                    }

                    return null;
                });
    }
}
