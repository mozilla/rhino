package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

public class MicrotaskTest {
    private Context cx;
    private Scriptable scope;

    @BeforeEach
    public void init() {
        cx = Context.enter();
        scope = cx.initStandardObjects();
    }

    @AfterEach
    public void cleanup() {
        Context.exit();
    }

    @Test
    public void testNormalPromiseResolution() {
        // Promises just get resolved normally, even if we call
        // "then" after resolution.
        var o =
                (Scriptable)
                        cx.evaluateString(
                                scope,
                                Utils.lines(
                                        "var o = {};",
                                        "var p = new Promise((resolve) => { o.resolve = resolve; });",
                                        "p.then((v) => { o.resolved = true; });",
                                        "o.resolve();",
                                        "p.then((v) => { o.resolvedAgain = true; });",
                                        "o"),
                                "test.js",
                                1,
                                null);
        // At this point promise should be resolved
        assertNotNull(o);
        assertEquals(true, o.get("resolved", o));
        assertEquals(true, o.get("resolvedAgain", o));
    }

    @Test
    public void testImmediatePromiseResolution() {
        var o =
                (Scriptable)
                        cx.evaluateString(
                                scope,
                                Utils.lines(
                                        "var o = {};",
                                        "var p = new Promise((resolve) => { o.resolve = resolve; });",
                                        "p.then((v) => { o.resolved = true; });",
                                        "o.resolve();",
                                        "o"),
                                "test.js",
                                1,
                                null);
        // At this point promise should be resolved
        assertNotNull(o);
        assertEquals(true, o.get("resolved", o));
        // Promise should still be resolved now
        cx.evaluateString(scope, "p.then((v) => { o.resolvedAgain = true; });", "test.js", 1, null);
        assertEquals(true, o.get("resolvedAgain", o));
    }

    @Test
    public void testDeferredPromiseResolution() {
        cx.suspendMicrotaskProcessing();
        var o =
                (Scriptable)
                        cx.evaluateString(
                                scope,
                                Utils.lines(
                                        "var o = {};",
                                        "var p = new Promise((resolve) => { o.resolve = resolve; });",
                                        "p.then((v) => { o.resolved = true; });",
                                        "o.resolve();",
                                        "o"),
                                "test.js",
                                1,
                                null);
        // Promise should not be resolved yet because microtasks didn't run
        assertNotNull(o);
        assertFalse(o.has("resolved", o));
        // Promise should still not be resolved
        cx.evaluateString(scope, "p.then((v) => { o.resolvedAgain = true; });", "test.js", 1, null);
        assertFalse(o.has("resolvedAgain", o));
        // Wake up microtasks and resolve both functions
        cx.resumeMicrotaskProcessing();
        assertEquals(true, o.get("resolved", o));
        assertEquals(true, o.get("resolvedAgain", o));
    }
}
