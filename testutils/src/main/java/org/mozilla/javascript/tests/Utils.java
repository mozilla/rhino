/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
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
    public static final int[] DEFAULT_OPT_LEVELS = new int[] {-1, 9};

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

    /** Runs the provided action at the given optimization level */
    public static void runWithOptimizationLevel(
            final ContextAction<?> action, final int optimizationLevel) {
        runWithOptimizationLevel(new ContextFactory(), action, optimizationLevel);
    }

    /** Runs the provided action at the given interpretation mode */
    public static void runWithMode(final ContextAction<?> action, final boolean interpretedMode) {
        runWithMode(new ContextFactory(), action, interpretedMode);
    }

    /** Runs the provided action at the given optimization level */
    @SuppressWarnings("deprecation")
    public static void runWithOptimizationLevel(
            final ContextFactory contextFactory,
            final ContextAction<?> action,
            final int optimizationLevel) {

        try (final Context cx = contextFactory.enterContext()) {
            cx.setOptimizationLevel(optimizationLevel);
            action.run(cx);
        }
    }

    /** Runs the provided action at the given interpretation mode */
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

    /**
     * Execute the provided script and assert the result. The language version is not changed.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllOptimizationLevels(final Object expected, final String script) {
        assertWithAllOptimizationLevels(-1, expected, script);
    }

    /**
     * Execute the provided script and assert the result. Before the execution the language version
     * is set to {@link Context#VERSION_1_8}.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllOptimizationLevels_1_8(
            final Object expected, final String script) {
        assertWithAllOptimizationLevels(Context.VERSION_1_8, expected, script);
    }

    /**
     * Execute the provided script and assert the result. Before the execution the language version
     * is set to {@link Context#VERSION_ES6}.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllOptimizationLevelsES6(
            final Object expected, final String script) {
        assertWithAllOptimizationLevels(Context.VERSION_ES6, expected, script);
    }

    /**
     * Execute the provided script and assert the result.
     *
     * @param languageVersion the language version constant from @{@link Context} or -1 to not
     *     change the language version at all
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllOptimizationLevels(
            final int languageVersion, final Object expected, final String script) {
        runWithAllOptimizationLevels(
                cx -> {
                    if (languageVersion > -1) {
                        cx.setLanguageVersion(languageVersion);
                    }
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

    /**
     * Execute the provided script and assert an {@link EvaluatorException}. The error message of
     * the {@link EvaluatorException} has to start with the provided expectedMessage. Before the
     * execution the language version is set to {@link Context#VERSION_1_8}.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertEvaluatorException_1_8(
            final String expectedMessage, final String script) {
        assertException(Context.VERSION_1_8, EvaluatorException.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link EvaluatorException}. The error message of
     * the {@link EvaluatorException} has to start with the provided expectedMessage. Before the
     * execution the language version is set to {@link Context#VERSION_ES6}.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertEvaluatorExceptionES6(
            final String expectedMessage, final String script) {
        assertException(Context.VERSION_ES6, EvaluatorException.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link EcmaError}. The error message of the {@link
     * EcmaError} has to start with the provided expectedMessage.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertEcmaError(final String expectedMessage, final String script) {
        assertException(-1, EcmaError.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link EcmaError}. The error message of the {@link
     * EcmaError} has to start with the provided expectedMessage. Before the execution the language
     * version is set to {@link Context#VERSION_1_8}.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertEcmaError_1_8(final String expectedMessage, final String script) {
        assertException(Context.VERSION_1_8, EcmaError.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link EcmaError}. The error message of the {@link
     * EcmaError} has to start with the provided expectedMessage. Before the execution the language
     * version is set to {@link Context#VERSION_1_8}.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertEcmaErrorES6(final String expectedMessage, final String script) {
        assertException(Context.VERSION_ES6, EcmaError.class, expectedMessage, script);
    }

    private static <T extends Exception> void assertException(
            final int languageVersion,
            final Class<T> expectedThrowable,
            final String expectedMessage,
            String js) {

        // to avoid false positives because we use startsWith()
        assertTrue("expectedMessage can't be empty",expectedMessage != null && !expectedMessage.isEmpty());

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    if (languageVersion > -1) {
                        cx.setLanguageVersion(languageVersion);
                    }
                    ScriptableObject scope = cx.initStandardObjects();

                    T e =
                            assertThrows(
                                    expectedThrowable,
                                    () -> cx.evaluateString(scope, js, "test", 1, null));

                    assertTrue(
                            "'"
                                    + e.getMessage()
                                    + "' does not start with '"
                                    + expectedMessage
                                    + "'",
                            e.getMessage().startsWith(expectedMessage));
                    return null;
                });
    }
}
