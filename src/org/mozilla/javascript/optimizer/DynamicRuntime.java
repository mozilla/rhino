/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public class DynamicRuntime {

  public static final String BOOTSTRAP_SIGNATURE =
      "(Ljava/lang/invoke/MethodHandles$Lookup;"
          + "Ljava/lang/String;"
          + "Ljava/lang/invoke/MethodType;"
          + ")Ljava/lang/invoke/CallSite;";

  public static Object fallback(MustFallbackException fe,
      Object o1, Object o2, Context cx,
      FallbackCallSite site) throws Throwable {
    site.handleFailure();
    return site.getFallbackHandle().invoke(o1, o2, cx);
  }

  private static CallSite makeCallSite(MethodHandles.Lookup lookup,
      MethodHandle fastHandle, MethodHandle slowerHandle,
      MethodHandle genericHandle, MethodType mType)
      throws NoSuchMethodException, IllegalAccessException {

    MethodType fallbackType = mType.insertParameterTypes(0, MustFallbackException.class)
        .insertParameterTypes(4, FallbackCallSite.class);
    MethodHandle fallbackSub = lookup.findStatic(DynamicRuntime.class,
        "fallback", fallbackType);

    FallbackCallSite site;

    if (slowerHandle == null) {
      site = new FallbackCallSite(mType);
    } else {
      site = new DoubleFallbackCallSite(mType);
    }

    MethodHandle fallbackSubInvoke = MethodHandles.insertArguments(fallbackSub, 4, site);
    site.setFallbackHandle(genericHandle);

    site.setTarget(MethodHandles.catchException(fastHandle,
        MustFallbackException.class, fallbackSubInvoke));

    if (slowerHandle != null) {
      ((DoubleFallbackCallSite)site).setFirstFallbackHandle(MethodHandles.catchException(
          slowerHandle, MustFallbackException.class, fallbackSubInvoke));
    }

    return site;
  }

  public static Object integerAdd(Object o1, Object o2, Context cx) {
    try {
      final long i1 = (Integer)o1;
      final long i2 = (Integer)o2;
      final long r = i1 + i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int)r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object numberAdd(Object o1, Object o2, Context cx) {
    try {
      final double d1 = ((Number)o1).doubleValue();
      final double d2 = ((Number)o2).doubleValue();
      return ScriptRuntime.wrapNumber(d1 + d2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object integerSubtract(Object o1, Object o2, Context cx) {
    try {
      final long i1 = (Integer)o1;
      final long i2 = (Integer)o2;
      final long r = i1 - i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int)r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object numberSubtract(Object o1, Object o2, Context cx) {
    try {
      final double d1 = ((Number)o1).doubleValue();
      final double d2 = ((Number)o2).doubleValue();
      return ScriptRuntime.wrapNumber(d1 - d2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericSubtract(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 - d2);
  }

  public static Object integerDivide(Object o1, Object o2, Context cx) {
    try {
      final long i1 = (Integer)o1;
      final long i2 = (Integer)o2;
      final long r = i1 / i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int)r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object numberDivide(Object o1, Object o2, Context cx) {
    try {
      final double d1 = ((Number)o1).doubleValue();
      final double d2 = ((Number)o2).doubleValue();
      return ScriptRuntime.wrapNumber(d1 / d2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericDivide(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 / d2);
  }

  public static Object integerMultiply(Object o1, Object o2, Context cx) {
    try {
      final long i1 = (Integer)o1;
      final long i2 = (Integer)o2;
      final long r = i1 * i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int)r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object numberMultiply(Object o1, Object o2, Context cx) {
    try {
      final double d1 = ((Number)o1).doubleValue();
      final double d2 = ((Number)o2).doubleValue();
      return ScriptRuntime.wrapNumber(d1 * d2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericMultiply(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 * d2);
  }

  public static Object integerMod(Object o1, Object o2, Context cx) {
    try {
      final long i1 = (Integer)o1;
      final long i2 = (Integer)o2;
      final long r = i1 % i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int)r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericMod(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 % d2);
  }

  @SuppressWarnings("unused")
  public static CallSite bootstrapAdd(MethodHandles.Lookup lookup,
      String name,
      MethodType mType) throws NoSuchMethodException, IllegalAccessException {
    MethodHandle fast = lookup.findStatic(DynamicRuntime.class,
        "integerAdd", mType);
    MethodHandle slower = lookup.findStatic(DynamicRuntime.class,
      "numberAdd", mType);
    MethodHandle slow = lookup.findStatic(ScriptRuntime.class,
        "add", mType);
    return makeCallSite(lookup, fast, slower, slow, mType);
  }

  @SuppressWarnings("unused")
  public static CallSite bootstrapSubtract(MethodHandles.Lookup lookup,
      String name,
      MethodType mType) throws NoSuchMethodException, IllegalAccessException {
    MethodHandle fast = lookup.findStatic(DynamicRuntime.class,
        "integerSubtract", mType);
    MethodHandle slower = lookup.findStatic(DynamicRuntime.class,
        "numberSubtract", mType);
    MethodHandle slow = lookup.findStatic(DynamicRuntime.class,
        "genericSubtract", mType);
    return makeCallSite(lookup, fast, slower, slow, mType);
  }

  @SuppressWarnings("unused")
  public static CallSite bootstrapMultiply(MethodHandles.Lookup lookup,
      String name,
      MethodType mType) throws NoSuchMethodException, IllegalAccessException {
    MethodHandle fast = lookup.findStatic(DynamicRuntime.class,
        "integerMultiply", mType);
    MethodHandle slower = lookup.findStatic(DynamicRuntime.class,
        "numberMultiply", mType);
    MethodHandle slow = lookup.findStatic(DynamicRuntime.class,
        "genericMultiply", mType);
    return makeCallSite(lookup, fast, slower, slow, mType);
  }

  @SuppressWarnings("unused")
  public static CallSite bootstrapDivide(MethodHandles.Lookup lookup,
      String name,
      MethodType mType) throws NoSuchMethodException, IllegalAccessException {
    MethodHandle fast = lookup.findStatic(DynamicRuntime.class,
        "integerDivide", mType);
    MethodHandle slower = lookup.findStatic(DynamicRuntime.class,
        "numberDivide", mType);
    MethodHandle slow = lookup.findStatic(DynamicRuntime.class,
        "genericDivide", mType);
    return makeCallSite(lookup, fast, slower, slow, mType);
  }

  @SuppressWarnings("unused")
  public static CallSite bootstrapMod(MethodHandles.Lookup lookup,
      String name,
      MethodType mType) throws NoSuchMethodException, IllegalAccessException {
    MethodHandle fast = lookup.findStatic(DynamicRuntime.class,
        "integerMod", mType);
    MethodHandle slow = lookup.findStatic(DynamicRuntime.class,
        "genericMod", mType);
    return makeCallSite(lookup, fast, null, slow, mType);
  }
}
