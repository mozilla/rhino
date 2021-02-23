package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

/*
 * This class keeps track of a set of fallback handles. When a call fails,
 * it switches to a new handle.
 */
public class DoubleFallbackCallSite
    extends FallbackCallSite
{
  public static final int MAX_FAILURES = 10;

  public DoubleFallbackCallSite(MethodType type) {
    super(type);
  }

  void handleFailure() {
    failureCount++;
    if (failureCount >= MAX_FAILURES) {
      if (firstFallback) {
        firstFallback = false;
        failureCount = 0;
        setTarget(firstFallbackHandle);
      } else {
        setTarget(fallbackHandle);
      }
    }
  }

  void setFirstFallbackHandle(MethodHandle mh) {
    this.firstFallbackHandle = mh;
  }

  private boolean firstFallback = true;
  private MethodHandle firstFallbackHandle;
}
