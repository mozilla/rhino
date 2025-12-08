/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.testutils;

import static org.junit.Assert.*;

import java.util.stream.IntStream;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
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
     * helper for joining multiple lines into one string, so that you don't need to do {@code
     * "line1\n" + "line2\n" + "line3"} by yourself. This should be used when creating strings to
     * compare against output that has explicit "\n" separators.
     *
     * @param lines the lines to join
     * @return the joined lines
     */
    public static String lines(String... lines) {
        return String.join("\n", lines);
    }

    /**
     * helper for joining multiple lines into one string, so that you don't need to do {@code
     * "line1\n" + "line2\n" + "line3"} by yourself, that uses the system line break so that it will
     * work across platforms. This must be used when comparing output written using "println" or
     * other methods that use the system line separator rather than a constant "\n".
     *
     * @param lines the lines to join
     * @return the joined lines
     */
    public static String portableLines(String... lines) {
        return String.join(System.lineSeparator(), lines);
    }

    /** Make the ctor private; this is a utility classe. */
    private Utils() {}

    /**
     * Execute the provided script in a fresh context as "myScript.js".
     *
     * @param script the script code
     * @param interpreted true if interpreted mode should be used
     */
    public static void executeScript(String script, boolean interpreted) {
        Utils.runWithMode(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    return cx.evaluateString(scope, script, "myScript.js", 1, null);
                },
                interpreted);
    }

    /**
     * Runs the action successively with interpreted and optimized mode
     *
     * @param action the action to execute
     */
    public static void runWithAllModes(final ContextAction<?> action) {
        runWithMode(action, true);
        runWithMode(action, false);
    }

    /**
     * Runs the action successively with interpreted and optimized mode
     *
     * @param contextFactory the context factory to use
     * @param action the action to execute
     */
    public static void runWithAllModes(
            final ContextFactory contextFactory, final ContextAction<?> action) {
        runWithMode(contextFactory, action, true);
        runWithMode(contextFactory, action, false);
    }

    /**
     * Runs the provided action at the given interpretation mode
     *
     * @param action the action to execute
     * @param interpretedMode true if interpreted mode should be used
     */
    public static void runWithMode(final ContextAction<?> action, final boolean interpretedMode) {
        runWithMode(new ContextFactory(), action, interpretedMode);
    }

    /**
     * Runs the provided action at the given interpretation mode
     *
     * @param contextFactory the context factory to use
     * @param action the action to execute
     * @param interpretedMode true if interpreted mode should be used
     */
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
     *
     * @return the opt level
     */
    public static int[] getTestOptLevels() {
        String overriddenLevel = System.getProperty("TEST_OPTLEVEL");
        if (overriddenLevel != null && !overriddenLevel.isEmpty()) {
            return new int[] {Integer.parseInt(overriddenLevel)};
        }
        return DEFAULT_OPT_LEVELS;
    }

    /**
     * Check the java version used.
     *
     * @param desiredVersion the minimal java version needed
     * @return true if the current java version is at least the desiredVersion
     */
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
    public static void assertWithAllModes(final Object expected, final String script) {
        assertWithAllModes(-1, null, expected, script);
    }

    /**
     * Execute the provided script and assert the result. Before the execution the language version
     * is set to {@link Context#VERSION_1_4}.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModes_1_4(final Object expected, final String script) {
        assertWithAllModes(Context.VERSION_1_4, null, expected, script);
    }

    /**
     * Execute the provided script and assert the result. Before the execution the language version
     * is set to {@link Context#VERSION_1_5}.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModes_1_5(final Object expected, final String script) {
        assertWithAllModes(Context.VERSION_1_5, null, expected, script);
    }

    /**
     * Execute the provided script and assert the result. Before the execution the language version
     * is set to {@link Context#VERSION_1_8}.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModes_1_8(final Object expected, final String script) {
        assertWithAllModes(Context.VERSION_1_8, null, expected, script);
    }

    /**
     * Execute the provided script and assert the result. Before the execution the language version
     * is set to {@link Context#VERSION_ES6}.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModes_ES6(final Object expected, final String script) {
        assertWithAllModes(Context.VERSION_ES6, null, expected, script);
    }

    /**
     * Execute the provided script and assert the result. Before the execution the language version
     * is set to {@link Context#VERSION_ES6}.
     *
     * @param message the message to be used if this fails
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModes_ES6(
            final String message, final Object expected, final String script) {
        assertWithAllModes(Context.VERSION_ES6, message, expected, script);
    }

    /**
     * Execute the provided script and assert the result.
     *
     * @param languageVersion the language version constant from @{@link Context} or -1 to not
     *     change the language version at all
     * @param message the message to be used if this fails
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModes(
            final int languageVersion,
            final String message,
            final Object expected,
            final String script) {
        assertWithAllModes(new ContextFactory(), languageVersion, message, expected, script);
    }

    /**
     * Execute the provided script and assert the result.
     *
     * @param contextFactory a user defined {@link ContextFactory}
     * @param languageVersion the language version constant from @{@link Context} or -1 to not
     *     change the language version at all
     * @param message the message to be used if this fails
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModes(
            final ContextFactory contextFactory,
            final int languageVersion,
            final String message,
            final Object expected,
            final String script) {
        runWithAllModes(
                contextFactory,
                cx -> {
                    if (languageVersion > -1) {
                        cx.setLanguageVersion(languageVersion);
                    }
                    final Scriptable scope = cx.initStandardObjects();
                    final Object res = cx.evaluateString(scope, script, "test.js", 1, null);

                    if (expected instanceof Integer && res instanceof Double) {
                        assertEquals(
                                message,
                                ((Integer) expected).doubleValue(),
                                ((Double) res).doubleValue(),
                                0.00001);
                        return null;
                    }
                    if (expected instanceof Double && res instanceof Integer) {
                        assertEquals(
                                message,
                                ((Double) expected).doubleValue(),
                                ((Integer) res).doubleValue(),
                                0.00001);
                        return null;
                    }

                    assertEquals(message, expected, res);
                    return null;
                });
    }

    /**
     * Execute the provided script using a {@link TopLevel} instance as scope and assert the result.
     * Before the execution the language version is set to {@link Context#VERSION_ES6}.
     *
     * @param expected the expected result
     * @param script the javascript script to execute
     */
    public static void assertWithAllModesTopLevelScope_ES6(
            final Object expected, final String script) {
        runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects(new TopLevel());
                    final Object res = cx.evaluateString(scope, script, "test.js", 1, null);

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
     * Execute the provided script and assert an {@link org.mozilla.javascript.JavaScriptException}.
     * The error message of the {@link org.mozilla.javascript.JavaScriptException} has to start with
     * the provided expectedMessage.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertJavaScriptException(
            final String expectedMessage, final String script) {
        assertException(-1, JavaScriptException.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link JavaScriptException}. The error message of
     * the {@link JavaScriptException} has to start with the provided expectedMessage. Before the
     * execution the language version is set to {@link Context#VERSION_1_8}.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertJavaScriptException_1_8(
            final String expectedMessage, final String script) {
        assertException(Context.VERSION_1_8, JavaScriptException.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link org.mozilla.javascript.JavaScriptException}.
     * The error message of the {@link org.mozilla.javascript.JavaScriptException} has to start with
     * the provided expectedMessage. Before the execution the language version is set to {@link
     * Context#VERSION_1_8}.
     *
     * @param expectedMessage the expected result
     * @param script the javascript script to execute
     */
    public static void assertJavaScriptException_ES6(
            final String expectedMessage, final String script) {
        assertException(Context.VERSION_ES6, JavaScriptException.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link EcmaError}. The error message of the {@link
     * EcmaError} has to start with the provided expectedMessage. Before the execution the language
     * version is set to {@link Context#VERSION_1_8}.
     *
     * @param expectedMessage the expected exception message
     * @param script the javascript script to execute
     */
    public static void assertEcmaErrorES6(final String expectedMessage, final String script) {
        assertException(Context.VERSION_ES6, EcmaError.class, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link EcmaError}. The error message of the {@link
     * EcmaError} has to start with the provided expectedMessage.
     *
     * @param languageVersion the language version to be used
     * @param <T> the type of the expected throwable
     * @param expectedThrowable the class of the expected exception
     * @param expectedMessage the expected exception message
     * @param script the javascript script to execute
     */
    public static <T extends Exception> void assertException(
            final int languageVersion,
            final Class<T> expectedThrowable,
            final String expectedMessage,
            String script) {
        assertException(
                new ContextFactory(), languageVersion, expectedThrowable, expectedMessage, script);
    }

    /**
     * Execute the provided script and assert an {@link EcmaError}. The error message of the {@link
     * EcmaError} has to start with the provided expectedMessage.
     *
     * @param contextFactory the context factory to be used
     * @param languageVersion the language version to be used
     * @param <T> the type of the expected throwable
     * @param expectedThrowable the class of the expected exception
     * @param expectedMessage the expected exception message
     * @param script the javascript script to execute
     */
    public static <T extends Exception> void assertException(
            final ContextFactory contextFactory,
            final int languageVersion,
            final Class<T> expectedThrowable,
            final String expectedMessage,
            String script) {

        // to avoid false positives because we use startsWith()
        assertTrue(
                "expectedMessage can't be empty",
                expectedMessage != null && !expectedMessage.isEmpty());

        Utils.runWithAllModes(
                contextFactory,
                cx -> {
                    if (languageVersion > -1) {
                        cx.setLanguageVersion(languageVersion);
                    }
                    ScriptableObject scope = cx.initStandardObjects();

                    T e =
                            assertThrows(
                                    expectedThrowable,
                                    () -> cx.evaluateString(scope, script, "test", 1, null));

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

    /**
     * Construct a new {@link ContextFactory}
     *
     * @param features the features to enable in addition to the already enabled featured from the
     *     {@link ContextFactory}
     * @return a new {@link ContextFactory} with all provided features enabled
     */
    public static ContextFactory contextFactoryWithFeatures(int... features) {
        return new ContextFactoryWithFeatures(features, new int[0]);
    }

    /**
     * Construct a new {@link ContextFactory}
     *
     * @param features the features to forcibly disable, overriding the default from {@link
     *     ContextFactory}
     * @return a new {@link ContextFactory} with all provided features disabled
     */
    public static ContextFactory contextFactoryWithFeatureDisabled(int... features) {
        return new ContextFactoryWithFeatures(new int[0], features);
    }

    private static class ContextFactoryWithFeatures extends ContextFactory {
        private final int[] enabledFeatures;
        private final int[] disabledFeatures;

        private ContextFactoryWithFeatures(int[] enabledFeatures, int[] disabledFeatures) {
            this.enabledFeatures = enabledFeatures;
            this.disabledFeatures = disabledFeatures;
        }

        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            if (contains(enabledFeatures, featureIndex)) {
                return true;
            }
            if (contains(disabledFeatures, featureIndex)) {
                return false;
            }

            return super.hasFeature(cx, featureIndex);
        }

        private static boolean contains(int[] array, int target) {
            // shortcut for no and single modified feature
            if (array.length == 0) {
                return false;
            } else if (array.length == 1) {
                return target == array[0];
            }

            return IntStream.of(array).anyMatch(x -> x == target);
        }
    }
}
