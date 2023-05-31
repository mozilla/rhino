/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

public class OverloadTest {

    public static String x(Collection<String> x) {
        return "collection";
    }

    public static String x(Map<String, String> x) {
        return "map";
    }

    public static String x(Runnable r) {
        return "runnable";
    }

    @Test
    public void jsObjectToMap() {
        assertEvaluates("map", "String(org.mozilla.javascript.tests.OverloadTest.x({}));");
        assertEvaluates(
                "map",
                "String(org.mozilla.javascript.tests.OverloadTest.x({ run: function() {} }));");
    }

    @Test
    public void jsArrayToCollection() {
        assertEvaluates("collection", "String(org.mozilla.javascript.tests.OverloadTest.x([]));");
    }

    @Test
    public void jsFunctionToInterface() {
        assertThrows(
                EvaluatorException.class,
                "String(org.mozilla.javascript.tests.OverloadTest.x(function() {}));");
    }

    private static void assertEvaluates(final Object expected, final String source) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
                    assertEquals(expected, rep);
                    return null;
                });
    }

    private static void assertThrows(
            final Class<? extends Exception> exceptionClass, final String source) {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    try {
                        cx.evaluateString(scope, source, "test.js", 0, null);
                        fail("Did not throw exception");
                    } catch (Exception e) {
                        assertTrue(exceptionClass.isInstance(e));
                    }
                    return null;
                });
    }
}
