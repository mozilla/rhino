package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.testutils.Utils;

public class NativePromiseTest {
    private Context cx;
    private VarScope scope;

    @BeforeEach
    public void init() {
        cx = Context.enter();
        scope = cx.initStandardObjects();
    }

    @AfterEach
    public void destroy() {
        Context.exit();
    }

    @Test
    public void testNativePromise() {
        var p = NativePromise.create(scope);
        ScriptableObject.defineProperty(scope, "p", p, 0);
        assertFalse(p.isHandled());
        assertEquals(NativePromise.State.PENDING, p.getState());
        cx.evaluateString(
                scope,
                "if (!(p instanceof Promise)) { throw 'Should be a promise'; }",
                "test.js",
                1,
                null);
        p.resolve(cx, scope, "foo");
        assertEquals(NativePromise.State.FULFILLED, p.getState());
        assertEquals("foo", p.getResult());
    }

    @Test
    public void testNativePromiseReject() {
        var p = NativePromise.create(scope);
        assertFalse(p.isHandled());
        assertEquals(NativePromise.State.PENDING, p.getState());
        p.reject(cx, scope, "foo");
        assertEquals(NativePromise.State.REJECTED, p.getState());
        assertEquals("foo", p.getResult());
    }

    @Test
    public void testNativeResolveScript() {
        var p = NativePromise.create(scope);
        ScriptableObject.defineProperty(scope, "p", p, 0);
        ScriptableObject.defineProperty(scope, "resolved", false, 0);
        ScriptableObject.defineProperty(scope, "rejected", false, 0);
        ScriptableObject.defineProperty(scope, "result", Undefined.instance, 0);
        cx.evaluateString(
                scope,
                Utils.lines(
                        "p.then((r) => { resolved = true; result = r; },",
                        "() => { rejected = true; });"),
                "test.js",
                1,
                null);
        p.resolve(cx, scope, "foo");
        cx.processMicrotasks();
        assertEquals(Boolean.FALSE, ScriptableObject.getProperty(scope, "rejected"));
        assertEquals(Boolean.TRUE, ScriptableObject.getProperty(scope, "resolved"));
        assertEquals("foo", ScriptableObject.getProperty(scope, "result"));
    }

    @Test
    public void testNativeRejectScript() {
        var p = NativePromise.create(scope);
        ScriptableObject.defineProperty(scope, "p", p, 0);
        ScriptableObject.defineProperty(scope, "resolved", false, 0);
        ScriptableObject.defineProperty(scope, "rejected", false, 0);
        ScriptableObject.defineProperty(scope, "result", Undefined.instance, 0);
        cx.evaluateString(
                scope,
                Utils.lines(
                        "p.then(() => { resolved = true; },",
                        "(r) => { rejected = true; result = r; });"),
                "test.js",
                1,
                null);
        p.reject(cx, scope, "foo");
        cx.processMicrotasks();
        assertEquals(Boolean.TRUE, ScriptableObject.getProperty(scope, "rejected"));
        assertEquals(Boolean.FALSE, ScriptableObject.getProperty(scope, "resolved"));
        assertEquals("foo", ScriptableObject.getProperty(scope, "result"));
    }

    @Test
    public void testOnFulfillBeforeResolve() {
        var p = NativePromise.create(scope);
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onFulfill(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        p.resolve(cx, scope, "foo");
        assertEquals(0, count.get());
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("foo", value.get());
    }

    @Test
    public void testOnFulfillAfterResolve() {
        var p = NativePromise.create(scope);
        p.resolve(cx, scope, "foo");
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onFulfill(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        assertEquals(0, count.get());
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("foo", value.get());
    }

    @Test
    public void testOnFulfillNotCalledWhenRejected() {
        var p = NativePromise.create(scope);
        var count = new AtomicInteger();
        p.onFulfill(cx, v -> count.incrementAndGet());
        p.reject(cx, scope, "bar");
        cx.processMicrotasks();
        assertEquals(0, count.get());
        p.onFulfill(cx, v -> count.incrementAndGet());
        cx.processMicrotasks();
        assertEquals(0, count.get());
    }

    @Test
    public void testOnRejectBeforeReject() {
        var p = NativePromise.create(scope);
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onReject(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        p.reject(cx, scope, "bar");
        assertEquals(0, count.get());
        assertTrue(p.isHandled());
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("bar", value.get());
    }

    @Test
    public void testOnRejectAfterReject() {
        var p = NativePromise.create(scope);
        p.reject(cx, scope, "bar");
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onReject(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        assertEquals(0, count.get());
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("bar", value.get());
    }

    @Test
    public void testOnRejectNotCalledWhenFulfilled() {
        var p = NativePromise.create(scope);
        var count = new AtomicInteger();
        p.onReject(cx, v -> count.incrementAndGet());
        p.resolve(cx, scope, "foo");
        cx.processMicrotasks();
        assertEquals(0, count.get());
        p.onReject(cx, v -> count.incrementAndGet());
        cx.processMicrotasks();
        assertEquals(0, count.get());
    }

    @Test
    public void testOnResolutionPendingFulfilled() {
        var p = NativePromise.create(scope);
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onResolution(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        p.resolve(cx, scope, "foo");
        assertEquals(0, count.get());
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("foo", value.get());
    }

    @Test
    public void testOnResolutionPendingRejected() {
        var p = NativePromise.create(scope);
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onResolution(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        p.reject(cx, scope, "bar");
        assertEquals(0, count.get());
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("bar", value.get());
    }

    @Test
    public void testOnResolutionAfterSettled() {
        var fulfilled = NativePromise.create(scope);
        fulfilled.resolve(cx, scope, "foo");
        var fulfillValue = new AtomicReference<Object>();
        fulfilled.onResolution(cx, fulfillValue::set);
        cx.processMicrotasks();
        assertEquals("foo", fulfillValue.get());

        var rejected = NativePromise.create(scope);
        rejected.reject(cx, scope, "bar");
        var rejectValue = new AtomicReference<Object>();
        rejected.onResolution(cx, rejectValue::set);
        cx.processMicrotasks();
        assertEquals("bar", rejectValue.get());
    }

    @Test
    public void testMultipleJavaCallbacks() {
        var p = NativePromise.create(scope);
        var calls = new ArrayList<String>();
        p.onFulfill(cx, v -> calls.add("fulfill1:" + v));
        p.onResolution(cx, v -> calls.add("resolution:" + v));
        p.onReject(cx, v -> calls.add("reject:" + v));
        p.onFulfill(cx, v -> calls.add("fulfill2:" + v));
        p.resolve(cx, scope, "foo");
        cx.processMicrotasks();
        assertEquals(List.of("fulfill1:foo", "resolution:foo", "fulfill2:foo"), calls);
    }

    @Test
    public void testSettleTwiceThrows() {
        var p = NativePromise.create(scope);
        p.resolve(cx, scope, "foo");
        assertThrows(IllegalStateException.class, () -> p.resolve(cx, scope, "bar"));
        assertThrows(IllegalStateException.class, () -> p.reject(cx, scope, "bar"));
        assertEquals(NativePromise.State.FULFILLED, p.getState());
        assertEquals("foo", p.getResult());

        var q = NativePromise.create(scope);
        q.reject(cx, scope, "foo");
        assertThrows(IllegalStateException.class, () -> q.reject(cx, scope, "bar"));
        assertThrows(IllegalStateException.class, () -> q.resolve(cx, scope, "bar"));
        assertEquals(NativePromise.State.REJECTED, q.getState());
        assertEquals("foo", q.getResult());
    }

    @Test
    public void testJavaCallbackReceivesJavaScriptObject() {
        var p = NativePromise.create(scope);
        Object obj = cx.evaluateString(scope, "({a: 1})", "test.js", 1, null);
        var received = new AtomicReference<Object>();
        p.onFulfill(cx, received::set);
        p.resolve(cx, scope, obj);
        cx.processMicrotasks();
        assertSame(obj, received.get());
        assertEquals(1, ((Scriptable) received.get()).get("a", (Scriptable) received.get()));
    }

    @Test
    public void testJsCreatedFulfilledPromiseJavaOnFulfill() {
        cx.evaluateString(
                scope, "var p = new Promise(function(res) { res('foo'); });", "test.js", 1, null);
        var p = (NativePromise) ScriptableObject.getProperty(scope, "p");
        assertEquals(NativePromise.State.FULFILLED, p.getState());
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onFulfill(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        assertEquals(0, count.get());
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("foo", value.get());
    }

    @Test
    public void testJsCreatedRejectedPromiseJavaOnReject() {
        cx.evaluateString(
                scope,
                "var p = new Promise(function(res, rej) { rej('bar'); });",
                "test.js",
                1,
                null);
        var p = (NativePromise) ScriptableObject.getProperty(scope, "p");
        assertEquals(NativePromise.State.REJECTED, p.getState());
        var count = new AtomicInteger();
        var value = new AtomicReference<Object>();
        p.onReject(
                cx,
                v -> {
                    count.incrementAndGet();
                    value.set(v);
                });
        cx.processMicrotasks();
        assertEquals(1, count.get());
        assertEquals("bar", value.get());
    }

    @Test
    public void testJsCreatedPendingPromiseResolvedFromJava() {
        cx.evaluateString(
                scope,
                Utils.lines(
                        "var p = new Promise(function() {});",
                        "var result = undefined;",
                        "p.then(function(v) { result = v; });"),
                "test.js",
                1,
                null);
        var p = (NativePromise) ScriptableObject.getProperty(scope, "p");
        assertEquals(NativePromise.State.PENDING, p.getState());
        var fulfillValue = new AtomicReference<Object>();
        var resolutionValue = new AtomicReference<Object>();
        var rejectCount = new AtomicInteger();
        p.onFulfill(cx, fulfillValue::set);
        p.onResolution(cx, resolutionValue::set);
        p.onReject(cx, v -> rejectCount.incrementAndGet());
        p.resolve(cx, scope, "baz");
        cx.processMicrotasks();
        assertEquals(NativePromise.State.FULFILLED, p.getState());
        assertEquals("baz", ScriptableObject.getProperty(scope, "result"));
        assertEquals("baz", fulfillValue.get());
        assertEquals("baz", resolutionValue.get());
        assertEquals(0, rejectCount.get());
    }

    @Test
    public void testJsCreatedPendingPromiseRejectedFromJava() {
        cx.evaluateString(
                scope,
                Utils.lines(
                        "var p = new Promise(function() {});",
                        "var caught = undefined;",
                        "p.catch(function(r) { caught = r; });"),
                "test.js",
                1,
                null);
        var p = (NativePromise) ScriptableObject.getProperty(scope, "p");
        assertEquals(NativePromise.State.PENDING, p.getState());
        var rejectValue = new AtomicReference<Object>();
        var fulfillCount = new AtomicInteger();
        p.onReject(cx, rejectValue::set);
        p.onFulfill(cx, v -> fulfillCount.incrementAndGet());
        p.reject(cx, scope, "boom");
        cx.processMicrotasks();
        assertEquals(NativePromise.State.REJECTED, p.getState());
        assertEquals("boom", ScriptableObject.getProperty(scope, "caught"));
        assertEquals("boom", rejectValue.get());
        assertEquals(0, fulfillCount.get());
        assertTrue(p.isHandled());
    }

    @Test
    public void testJsCreatedPromiseResolvedByJsAfterJavaCallbackRegistered() {
        cx.evaluateString(
                scope,
                Utils.lines(
                        "var resolveFn;",
                        "var p = new Promise(function(res) { resolveFn = res; });"),
                "test.js",
                1,
                null);
        var p = (NativePromise) ScriptableObject.getProperty(scope, "p");
        assertEquals(NativePromise.State.PENDING, p.getState());
        var value = new AtomicReference<Object>();
        p.onFulfill(cx, value::set);
        assertNull(value.get());
        cx.evaluateString(scope, "resolveFn('qux');", "test.js", 1, null);
        assertEquals(NativePromise.State.FULFILLED, p.getState());
        assertEquals("qux", value.get());
    }

    @Test
    public void testResolveWithSelfRejects() {
        var p = NativePromise.create(scope);
        p.resolve(cx, scope, p);
        assertEquals(NativePromise.State.REJECTED, p.getState());
        var err = (Scriptable) p.getResult();
        assertEquals("TypeError", ScriptableObject.getProperty(err, "name"));
    }

    @Test
    public void testResolveWithPromiseAdoptsState() {
        var outer = NativePromise.create(scope);
        var inner = NativePromise.create(scope);
        inner.resolve(cx, scope, "inner");
        var received = new AtomicReference<Object>();
        outer.onFulfill(cx, received::set);
        outer.resolve(cx, scope, inner);
        cx.processMicrotasks();
        assertEquals(NativePromise.State.FULFILLED, outer.getState());
        assertEquals("inner", outer.getResult());
        assertEquals("inner", received.get());
    }

    @Test
    public void testResolveWithThenable() {
        Object thenable =
                cx.evaluateString(
                        scope,
                        "({then: function(res, rej) { res('adopted'); }})",
                        "test.js",
                        1,
                        null);
        var p = NativePromise.create(scope);
        var received = new AtomicReference<Object>();
        p.onFulfill(cx, received::set);
        p.resolve(cx, scope, thenable);
        cx.processMicrotasks();
        assertEquals(NativePromise.State.FULFILLED, p.getState());
        assertEquals("adopted", p.getResult());
        assertEquals("adopted", received.get());

        Object rejectingThenable =
                cx.evaluateString(
                        scope, "({then: function(res, rej) { rej('nope'); }})", "test.js", 1, null);
        var q = NativePromise.create(scope);
        var reason = new AtomicReference<Object>();
        q.onReject(cx, reason::set);
        q.resolve(cx, scope, rejectingThenable);
        cx.processMicrotasks();
        assertEquals(NativePromise.State.REJECTED, q.getState());
        assertEquals("nope", q.getResult());
        assertEquals("nope", reason.get());
    }

    @Test
    public void testSettleDuringThenableAdoptionThrows() {
        var outer = NativePromise.create(scope);
        var inner = NativePromise.create(scope);
        outer.resolve(cx, scope, inner);
        assertEquals(NativePromise.State.PENDING, outer.getState());
        assertThrows(IllegalStateException.class, () -> outer.resolve(cx, scope, "x"));
        assertThrows(IllegalStateException.class, () -> outer.reject(cx, scope, "x"));
        inner.resolve(cx, scope, "done");
        cx.processMicrotasks();
        assertEquals(NativePromise.State.FULFILLED, outer.getState());
        assertEquals("done", outer.getResult());
    }

    @Test
    public void testJsResolveIgnoredAfterJavaSettled() {
        cx.evaluateString(
                scope,
                Utils.lines(
                        "var fns;",
                        "var p = new Promise(function(res, rej) { fns = {res: res, rej: rej}; });"),
                "test.js",
                1,
                null);
        var p = (NativePromise) ScriptableObject.getProperty(scope, "p");
        p.resolve(cx, scope, "java");
        cx.evaluateString(scope, "fns.res('js');", "test.js", 1, null);
        cx.evaluateString(
                scope,
                "fns.rej({then: function(r, j) { r('js-thenable'); }});",
                "test.js",
                1,
                null);
        cx.processMicrotasks();
        assertEquals(NativePromise.State.FULFILLED, p.getState());
        assertEquals("java", p.getResult());
    }

    @Test
    public void testLateOnRejectMarksHandled() {
        var p = NativePromise.create(scope);
        p.reject(cx, scope, "bar");
        assertFalse(p.isHandled());
        var value = new AtomicReference<Object>();
        p.onReject(cx, value::set);
        assertTrue(p.isHandled());
        cx.processMicrotasks();
        assertEquals("bar", value.get());
    }

    @Test
    public void testLateOnResolutionMarksHandledOnlyOnRejection() {
        var rejected = NativePromise.create(scope);
        rejected.reject(cx, scope, "bar");
        assertFalse(rejected.isHandled());
        rejected.onResolution(cx, v -> {});
        assertTrue(rejected.isHandled());

        var fulfilled = NativePromise.create(scope);
        fulfilled.resolve(cx, scope, "foo");
        fulfilled.onResolution(cx, v -> {});
        assertFalse(fulfilled.isHandled());
    }
}
