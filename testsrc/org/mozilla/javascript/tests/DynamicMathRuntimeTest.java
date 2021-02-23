package org.mozilla.javascript.tests;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.optimizer.DynamicMathRuntime;
import org.mozilla.javascript.optimizer.FallbackCallSite;

import static org.junit.Assert.*;

public class DynamicMathRuntimeTest {

  private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
  private static final MethodType invokeType =
      MethodType.methodType(Object.class, Object.class, Object.class, Context.class);

  private Context cx;

  @Before
  public void init() {
    cx = Context.enter();
  }

  @After
  public void close() {
    Context.exit();
  }

  @Test
  public void testIntegerAdd() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.ADD_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    // Integer ops
    assertEquals(0, call.invoke(0, 0, cx));
    assertEquals(4, call.invoke(2, 2, cx));
    assertEquals(4, call.invoke(6, -2, cx));
    // Overflow
    double bigResult = (double) ((long) Integer.MAX_VALUE + (long) 10);
    assertEquals(bigResult, call.invoke(Integer.MAX_VALUE, 10, cx));
    double tinyResult = (double) ((long) Integer.MIN_VALUE - 10000);
    assertEquals(tinyResult, call.invoke(Integer.MIN_VALUE, -10000, cx));
    // All of this should have been handled by the fast path
    assertEquals(0, site.getFailureCount());
  }

  @Test
  public void testMixedAdd() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.ADD_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    assertEquals(0, call.invoke(0, 0, cx));
    assertEquals(4.0, call.invoke(2.0, 2, cx));
    assertEquals(4.0, call.invoke(6, -2.0, cx));
    double bigResult = (double) ((long) Integer.MAX_VALUE + (long) 10);
    assertEquals(bigResult, call.invoke(Integer.MAX_VALUE, 10, cx));
    double tinyResult = (double) ((long) Integer.MIN_VALUE - 10000);
    assertEquals(tinyResult, call.invoke(Integer.MIN_VALUE, -10000, cx));
    assertEquals("123 Sesame Street", call.invoke(123, " Sesame Street", cx).toString());
    assertEquals(100.0, call.invoke(100, null, cx));
    assertTrue(Double.isNaN((double) call.invoke(Undefined.instance, 100, cx)));
    // Should not have gone over yet
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);

    // Now switch over to the slow path
    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(3.14, call.invoke(3.0, 0.14, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }

  @Test
  public void testMixedSubtract() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.SUBTRACT_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    assertEquals(0, call.invoke(0, 0, cx));
    assertEquals(0.0, call.invoke(2.0, 2, cx));
    assertEquals(8.0, call.invoke(6, -2.0, cx));
    double tinyResult = (double) ((long) Integer.MIN_VALUE - 10000);
    assertEquals(tinyResult, call.invoke(Integer.MIN_VALUE, 10000, cx));
    // Should not have gone over yet
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);

    // Now switch over to the slow path
    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(3.0, call.invoke(3.14, 0.14, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }

  @Test
  public void testMixedMultiply() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.MULTIPLY_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    assertEquals(0, call.invoke(0, 0, cx));
    assertEquals(16, call.invoke(4, 4, cx));
    assertEquals(4.0, call.invoke(2.0, 2, cx));
    assertEquals(-12, call.invoke(6, -2, cx));
    assertEquals(-12.0, call.invoke(6, -2.0, cx));
    double bigResult = (double) ((long) Integer.MAX_VALUE * 4);
    assertEquals(bigResult, call.invoke(Integer.MAX_VALUE, 4, cx));
    assertEquals(0.0, call.invoke(100, null, cx));
    assertTrue(Double.isNaN((double) call.invoke(Undefined.instance, 100, cx)));
    // Should not have gone over yet
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);

    // Now switch over to the slow path
    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(36.0, call.invoke(6.0, 6.0, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }

  @Test
  public void testMixedDivide() throws Throwable {
    // No fallback for this operation -- all done using floating-point
    CallSite site = DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.DIVIDE_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    assertEquals(4.0, call.invoke(16, 4, cx));
    assertEquals(4.0, call.invoke(8.0, 2, cx));
    assertEquals(-3.0, call.invoke(6, -2, cx));
    assertEquals(-3.0, call.invoke(6, -2.0, cx));
    assertTrue(Double.isInfinite((double) call.invoke(100, 0, cx)));
    assertTrue(Double.isInfinite((double) call.invoke(100, 0.0, cx)));
    assertTrue(Double.isInfinite((double) call.invoke(100, null, cx)));
    assertEquals(0.0, call.invoke(null, 10, cx));
    assertTrue(Double.isNaN((double) call.invoke(Undefined.instance, 100, cx)));
    assertTrue(Double.isNaN((double) call.invoke(100, Undefined.instance, cx)));
  }

  @Test
  public void testMixedMod() throws Throwable {
    // No fallback for this operation -- all done using floating-point
    CallSite site = DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.MOD_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    assertEquals(0.0, call.invoke(16, 4, cx));
    assertEquals(2.0, call.invoke(12.0, 5, cx));
    assertEquals(2.0, call.invoke(12, 5.0, cx));
    assertEquals(2.0, call.invoke(12.0, 5.0, cx));
    assertTrue(Double.isNaN((double) call.invoke(100, 0, cx)));
    assertTrue(Double.isNaN((double) call.invoke(100, 0.0, cx)));
    assertTrue(Double.isNaN((double) call.invoke(100, null, cx)));
    assertEquals(0.0, call.invoke(null, 100, cx));
    assertTrue(Double.isNaN((double) call.invoke(100, Undefined.instance, cx)));
    assertTrue(Double.isNaN((double) call.invoke(Undefined.instance, 100, cx)));
  }

  @Test
  public void testIntegerAnd() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.AND_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    assertEquals(0, call.invoke(2, 4, cx));
    assertEquals(4, call.invoke(15, 4, cx));
    assertEquals(0, call.invoke(1 << 31, 1, cx));
    assertEquals(0xffff, call.invoke(0xefffffff, 0xffff, cx));
    assertEquals(0, site.getFailureCount());
  }

  @Test
  public void testMixedAnd() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.AND_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    // Even mixed operations end up as Integers
    assertEquals(0, call.invoke(2, 4, cx));
    assertEquals(4, call.invoke(15, 4.0, cx));
    assertEquals(0, call.invoke(1 << 31, 1.0, cx));
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);
    assertEquals(0, call.invoke(100, null, cx));
    assertEquals(0, call.invoke(null, 100, cx));
    assertEquals(0, call.invoke(100, Undefined.instance, cx));
    assertEquals(0, call.invoke(Undefined.instance, 100, cx));

    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(4, call.invoke(15, 4.0, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }

  @Test
  public void testMixedOr() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.OR_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    // Even mixed operations end up as Integers
    assertEquals(6, call.invoke(2, 4, cx));
    assertEquals(15, call.invoke(15, 4.0, cx));
    assertEquals(1 << 30 | 4, call.invoke(1 << 30, 4.0, cx));
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);

    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(15, call.invoke(15, 4.0, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }

  @Test
  public void testMixedXor() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.XOR_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    // Even mixed operations end up as Integers
    assertEquals(6, call.invoke(2, 4, cx));
    assertEquals(11, call.invoke(15, 4.0, cx));
    assertEquals(1 << 30 ^ 4, call.invoke(1 << 30, 4.0, cx));
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);

    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(11, call.invoke(15, 4.0, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }

  @Test
  public void testMixedLsh() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.LSH_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    // Even mixed operations end up as Integers
    assertEquals(1024, call.invoke(1, 10, cx));
    assertEquals(1 << 31, call.invoke(1.0, 31, cx));
    assertEquals(1, call.invoke(1, 32.0, cx));
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);
    assertEquals(1, call.invoke(1, null, cx));
    assertEquals(0, call.invoke(null, 1, cx));
    assertEquals(1, call.invoke(1, Undefined.instance, cx));
    assertEquals(0, call.invoke(Undefined.instance, 1, cx));

    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(1024, call.invoke(1, 10.0, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }

  @Test
  public void testMixedRsh() throws Throwable {
    FallbackCallSite site = (FallbackCallSite) DynamicMathRuntime.bootstrap(lookup,
        DynamicMathRuntime.RSH_OP, invokeType);
    MethodHandle call = site.dynamicInvoker();
    // Even mixed operations end up as Integers
    assertEquals(64, call.invoke(1024, 4, cx));
    assertEquals(64, call.invoke(1024, 4.0, cx));
    assertEquals(0, call.invoke(1024.0, 31, cx));
    assertTrue(site.getFailureCount() < FallbackCallSite.MAX_FAILURES);

    for (int i = 0; i < FallbackCallSite.MAX_FAILURES * 2; i++) {
      assertEquals(255, call.invoke(0xffff, 8.0, cx));
    }
    assertTrue(site.getFailureCount() >= FallbackCallSite.MAX_FAILURES);
  }
}
