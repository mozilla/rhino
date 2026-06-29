/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2021;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.TestSource;
import org.mozilla.javascript.tools.shell.Global;

public class NativeFinalizationRegistryTest {

    @Test
    public void testFinalizationRegistryEnabled() throws IOException {
        try (var cx = Context.enter()) {
            cx.setFinalizationEnabled(true);
            try (var script =
                    new InputStreamReader(
                            new FileInputStream(
                                    TestSource.resolve(
                                            "testsrc/jstests/es2021/finalization-registry.js")),
                            StandardCharsets.UTF_8)) {
                var global = new Global(cx);
                global.setFileLoadPrefix(TestSource.getPrefix());
                var scope = TopLevel.createIsolate(global);
                cx.evaluateReader(scope, script, "finalization-registry.js", 1, null);
                // ScriptTestsBase does not necessarily do this so call it here
                cx.processMicrotasks();
            }
        }
    }

    @Test
    public void testFinalizationRegistryDisabled() throws IOException {
        try (var cx = Context.enter()) {
            cx.setFinalizationEnabled(false);
            try (var script =
                    new InputStreamReader(
                            new FileInputStream(
                                    TestSource.resolve(
                                            "testsrc/jstests/es2021/finalization-registry-disabled.js")),
                            StandardCharsets.UTF_8)) {
                var global = new Global(cx);
                global.setFileLoadPrefix(TestSource.getPrefix());
                var scope = TopLevel.createIsolate(global);
                cx.evaluateReader(scope, script, "finalization-registry.js", 1, null);
                // ScriptTestsBase does not necessarily do this so call it here
                cx.processMicrotasks();
            }
        }
    }

    @Test
    public void testCleanupDuringContextClose() throws IOException {
        try (var cx = Context.enter()) {
            var global = new Global(cx);
            var scope = TopLevel.createIsolate(global);

            // Set up a JS holder object via the Global scope.
            cx.evaluateString(
                    scope,
                    "globalThis._finalizerTest = {};"
                            + "var reg = new FinalizationRegistry(function(hv) { "
                            + "_finalizerTest.finalized = true; "
                            + "_finalizerTest.heldValue = hv;"
                            + "});\n"
                            + "var target = {};\n"
                            + "reg.register(target, 'test-hold-value');\n",
                    "test",
                    1,
                    null);

            // Force GC to enqueue the PhantomReference.
            System.gc();
        }
        // Context close via try-with-resources should complete without throwing or deadlocking.
    }
}
