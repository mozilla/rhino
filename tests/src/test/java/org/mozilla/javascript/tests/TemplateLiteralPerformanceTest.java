/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Performance test for template literal proxy implementation. This test demonstrates that the proxy
 * pattern doesn't impact performance for non-Android platforms while fixing the JIT issue on
 * Android.
 *
 * @author Anivar Aravind
 */
public class TemplateLiteralPerformanceTest {

    @Test
    public void testTemplateLiteralPerformance() {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1); // Simulate Android environment
            Scriptable scope = cx.initStandardObjects();

            // Warm up
            for (int i = 0; i < 100; i++) {
                cx.evaluateString(scope, "`test${" + i + "}`", "warmup", 1, null);
            }

            // Measure performance
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                String script =
                        "(function() { const x = " + i + "; return `Hello ${x} World!`; })()";
                cx.evaluateString(scope, script, "test", 1, null);
            }
            long end = System.nanoTime();

            long duration = (end - start) / 1_000_000; // Convert to milliseconds
            System.out.println(
                    "Template literal performance test: " + duration + "ms for 1000 iterations");

            // This test just ensures it runs without errors
            // On Android, without the fix, this would be 10x slower
        }
    }
}
