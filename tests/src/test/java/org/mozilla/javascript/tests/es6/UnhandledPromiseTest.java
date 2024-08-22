package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.shell.Global;

public class UnhandledPromiseTest {
    private Context cx;
    private Scriptable scope;

    @Before
    public void init() {
        cx = Context.enter();
        cx.setGeneratingDebug(true);
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setTrackUnhandledPromiseRejections(true);
        scope = new Global(cx);
    }

    @After
    public void terminate() {
        cx.close();
    }

    @Test
    public void simpleRejection() {
        exec("new Promise((resolve, reject) => { reject(); });");
        assertEquals(1, cx.getUnhandledPromiseTracker().enumerate().size());
        assertTrue(Undefined.isUndefined(cx.getUnhandledPromiseTracker().enumerate().get(0)));
        AtomicBoolean handled = new AtomicBoolean();
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertFalse(handled.get());
                            assertTrue(Undefined.isUndefined(o));
                            handled.set(true);
                        });
        assertTrue(handled.get());
    }

    @Test
    public void simpleRejectionHandled() {
        scope.put("caught", scope, Boolean.FALSE);
        exec(
                "let p = new Promise((resolve, reject) => { reject(); });\n"
                        + "p.catch((e) => { caught = true; });\n");
        assertTrue(cx.getUnhandledPromiseTracker().enumerate().isEmpty());
        AtomicBoolean handled = new AtomicBoolean();
        // We should actually never see anything in the tracker here
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertFalse(handled.get());
                            handled.set(true);
                        });
        assertFalse(handled.get());
        assertTrue(Context.toBoolean(scope.get("caught", scope)));
    }

    @Test
    public void rejectionHandledAfterThen() {
        scope.put("caught", scope, Boolean.FALSE);
        exec(
                "new Promise((resolve, reject) => { reject(); })."
                        + "then(() => {})."
                        + "catch((e) => { caught = true; });\n");
        assertTrue(cx.getUnhandledPromiseTracker().enumerate().isEmpty());
        AtomicBoolean handled = new AtomicBoolean();
        // We should actually never see anything in the tracker here
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertFalse(handled.get());
                            handled.set(true);
                        });
        assertFalse(handled.get());
        assertTrue(Context.toBoolean(scope.get("caught", scope)));
    }

    @Test
    public void thenRejectsButCatchHandles() {
        scope.put("caught", scope, Boolean.FALSE);
        exec(
                "new Promise((resolve, reject) => { resolve(); })."
                        + "then((e) => { return Promise.reject(); })."
                        + "catch((e) => { caught = true; });\n");
        assertTrue(cx.getUnhandledPromiseTracker().enumerate().isEmpty());
        AtomicBoolean handled = new AtomicBoolean();
        // We should actually never see anything in the tracker here
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertFalse(handled.get());
                            handled.set(true);
                        });
        assertFalse(handled.get());
        assertTrue(Context.toBoolean(scope.get("caught", scope)));
    }

    @Test
    public void rejectionObject() {
        exec("new Promise((resolve, reject) => { reject('rejected'); });");
        assertEquals(1, cx.getUnhandledPromiseTracker().enumerate().size());
        assertEquals("rejected", cx.getUnhandledPromiseTracker().enumerate().get(0));
        AtomicBoolean handled = new AtomicBoolean();
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertEquals("rejected", o);
                            handled.set(true);
                            assertEquals("rejected", o);
                        });
        assertTrue(handled.get());
    }

    @Test
    public void simpleThrowHandled() {
        scope.put("caught", scope, Boolean.FALSE);
        exec(
                "let p = new Promise((resolve, reject) => { throw 'threw'; });\n"
                        + "p.catch((e) => { assertEquals('threw', e); caught = true; });\n");
        assertTrue(cx.getUnhandledPromiseTracker().enumerate().isEmpty());
        AtomicBoolean handled = new AtomicBoolean();
        // We should actually never see anything in the tracker here
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertFalse(handled.get());
                            handled.set(true);
                        });
        assertFalse(handled.get());
        assertTrue(Context.toBoolean(scope.get("caught", scope)));
    }

    @Test
    public void throwInThen() {
        exec(
                "new Promise((resolve, reject) => { resolve() }).then(() => { throw 'threw in then' });");
        assertEquals(1, cx.getUnhandledPromiseTracker().enumerate().size());
        AtomicBoolean handled = new AtomicBoolean();
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertFalse(handled.get());
                            handled.set(true);
                            assertEquals("threw in then", o);
                        });
        assertTrue(handled.get());
    }

    @Test
    public void thenReturnsRejectedPromise() {
        exec(
                "new Promise((resolve, reject) => { resolve() }).then(() => { return Promise.reject('rejected'); });");
        assertEquals(1, cx.getUnhandledPromiseTracker().enumerate().size());
        AtomicBoolean handled = new AtomicBoolean();
        cx.getUnhandledPromiseTracker()
                .process(
                        (Object o) -> {
                            assertFalse(handled.get());
                            handled.set(true);
                            assertEquals("rejected", o);
                        });
        assertTrue(handled.get());
    }

    private void exec(String script) {
        cx.evaluateString(scope, "load('./testsrc/assert.js'); " + script, "test.js", 0, null);
    }
}
