package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.junit.Test;
import org.mozilla.javascript.optimizer.FallbackCallSite;
import org.mozilla.javascript.optimizer.FallbackException;

public class FallbackCallSiteTest {
    private static final MethodType testMethodType = MethodType.methodType(String.class);

    private static int alwaysFailInvokeCount = 0;

    private static String getMessage() {
        return "Hello, World!";
    }

    private static String alwaysFail() {
        throw new FallbackException();
    }

    private static String sometimesFail() {
        if (alwaysFailInvokeCount++ % 3 == 0) {
            throw new FallbackException();
        }
        return "Hello, World!";
    }

    private static String fallback(FallbackException fe, FallbackCallSite site, int nonce)
            throws Throwable {
        MethodHandle nextCall = site.fallBack(nonce);
        return (String) nextCall.invoke();
    }

    // Just test with one method that always works
    @Test
    public void testNoFallback() throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        FallbackCallSite site =
                new FallbackCallSite(
                        lookup.findStatic(
                                FallbackCallSiteTest.class,
                                "fallback",
                                FallbackCallSite.makeFallbackCallType(testMethodType)),
                        lookup.findStatic(
                                FallbackCallSiteTest.class, "getMessage", testMethodType));
        MethodHandle invoker = site.dynamicInvoker();

        // We should be able to invoke over and over and eventually we will
        // fail over, but every call should work
        for (int i = 0; i < 20; i++) {
            try {
                Object result = invoker.invoke();
                assertEquals("Hello, World!", result);
            } catch (Throwable t) {
                fail("Did not expect an exception: " + t);
            }
        }
    }

    // Test a method that always fails and ensure that it always falls back
    // to the method that does not
    @Test
    public void testFallback() throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        FallbackCallSite site =
                new FallbackCallSite(
                        lookup.findStatic(
                                FallbackCallSiteTest.class,
                                "fallback",
                                FallbackCallSite.makeFallbackCallType(testMethodType)),
                        lookup.findStatic(FallbackCallSiteTest.class, "alwaysFail", testMethodType),
                        lookup.findStatic(
                                FallbackCallSiteTest.class, "getMessage", testMethodType));
        MethodHandle invoker = site.dynamicInvoker();

        // We should be able to invoke over and over and eventually we will
        // fail over, but every call should work
        for (int i = 0; i < 20; i++) {
            try {
                Object result = invoker.invoke();
                assertEquals("Hello, World!", result);
            } catch (Throwable t) {
                fail("Did not expect an exception: " + t);
            }
        }
    }

    // Test a chain of three calls, with one in the middle that sometimes fails
    @Test
    public void testFallbackThree() throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        FallbackCallSite site =
                new FallbackCallSite(
                        lookup.findStatic(
                                FallbackCallSiteTest.class,
                                "fallback",
                                FallbackCallSite.makeFallbackCallType(testMethodType)),
                        lookup.findStatic(FallbackCallSiteTest.class, "alwaysFail", testMethodType),
                        lookup.findStatic(
                                FallbackCallSiteTest.class, "sometimesFail", testMethodType),
                        lookup.findStatic(
                                FallbackCallSiteTest.class, "getMessage", testMethodType));
        MethodHandle invoker = site.dynamicInvoker();

        // We should be able to invoke over and over and eventually we will
        // fail over, but every call should work
        for (int i = 0; i < 100; i++) {
            try {
                Object result = invoker.invoke();
                assertEquals("Hello, World!", result);
            } catch (Throwable t) {
                fail("Did not expect an exception: " + t);
            }
        }
    }
}
