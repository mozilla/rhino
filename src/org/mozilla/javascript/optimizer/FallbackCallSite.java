package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

/*
 * This class keeps track of a set of fallback handles. When a call fails,
 * it switches to a new handle.
 */
public class FallbackCallSite
  extends MutableCallSite
{
  public static final int MAX_FAILURES = 10;

  public FallbackCallSite(MethodType type) {
    super(type);
  }

  void handleFailure() {
    failureCount++;
    if (failureCount >= MAX_FAILURES) {
      setTarget(fallbackHandle);
    }
  }

  public int getFailureCount() {
    return failureCount;
  }

  void setFallbackHandle(MethodHandle mh) {
    this.fallbackHandle = mh;
  }

  MethodHandle getFallbackHandle() { return fallbackHandle; }

  protected int failureCount = 0;
  protected MethodHandle fallbackHandle;
}
