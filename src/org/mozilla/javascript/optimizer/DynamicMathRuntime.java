/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;

/**
 * This class contains bootstrap methods for InvokeDynamic instructions for math operations.
 * The operations try to aggressively optimize for integer operations by using the
 * following algorithm:
 *
 * <ul>
 *   <li>Attempt the operation assuming that both operands are Integer objects</li>
 *   <li>Catch the ClassCastException that will result if this fails</li>
 *   <li>If the operation failed, retry using a more generic method, usually by
 *   converting both operands to double.</li>
 *   <li>After a number of failures, permanently switch to the fallback method
 *   and invoke it directly after that</li>
 * </ul>
 *
 * Since this works using InvokeDynamic's CallSite mechanism, the algorithm above is
 * executed for each place in the code. As a result, parts of scripts that usually see
 * integer objects created will continue to use integer operations, with the ability to
 * fall back to "correct" JavaScript operations at any point.
 */
public class DynamicMathRuntime {

  /** The signature of the "boostrap" method in this method */
  public static final String BOOTSTRAP_SIGNATURE =
      "(Ljava/lang/invoke/MethodHandles$Lookup;"
          + "Ljava/lang/String;"
          + "Ljava/lang/invoke/MethodType;"
          + ")Ljava/lang/invoke/CallSite;";
  /** The signature for the method that will be the target for all calls */
  public static final String METHOD_SIGNATURE =
      "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;";

  public static final String ADD_OP = "add";
  public static final String SUBTRACT_OP = "subtract";
  public static final String MULTIPLY_OP = "multiply";
  public static final String DIVIDE_OP = "divide";
  public static final String MOD_OP = "mod";
  public static final String AND_OP = "and";
  public static final String OR_OP = "or";
  public static final String XOR_OP = "xor";
  public static final String LSH_OP = "lsh";
  public static final String RSH_OP = "rsh";

  /** An object that can be used to write the InvokeDynamic instruction for the bootstrap */
  public static final ClassFileWriter.MHandle BOOTSTRAP_HANDLE =
      new ClassFileWriter.MHandle(ByteCode.MH_INVOKESTATIC,
          "org.mozilla.javascript.optimizer.DynamicMathRuntime",
          "bootstrap",
          DynamicMathRuntime.BOOTSTRAP_SIGNATURE);

  /**
   * Handle an exception in one of the optimized calls by causing a failure count to be incremented,
   * and if exceeded, then the method handle is reset to point permanently to the fallback handle.
   */
  public static Object fallback(MustFallbackException fe,
      Object o1, Object o2, Context cx,
      FallbackCallSite site) throws Throwable {
    site.handleFailure();
    return site.getFallbackHandle().invoke(o1, o2, cx);
  }

  /**
   * Construct a CallSite that invokes the main method, and then calls "fallback" when it
   * receives a MustFallbackException.
   */
  private static CallSite makeCallSite(MethodHandles.Lookup lookup,
      MethodHandle fastHandle,
      MethodHandle genericHandle, MethodType mType)
      throws NoSuchMethodException, IllegalAccessException {

    // Get the method handle of the "fallback" method, which prepends a MustFallbackException
    // to the argument list.
    MethodType fallbackType = mType.insertParameterTypes(0, MustFallbackException.class)
        .insertParameterTypes(4, FallbackCallSite.class);
    MethodHandle fallbackSub = lookup.findStatic(DynamicMathRuntime.class,
        "fallback", fallbackType);

    FallbackCallSite site = new FallbackCallSite(mType);
    // Bind the CallSite to the method invocation of the "fallback" method so that
    // it can be modified.
    MethodHandle fallbackSubInvoke = MethodHandles.insertArguments(fallbackSub, 4, site);
    site.setFallbackHandle(genericHandle);
    site.setTarget(MethodHandles.catchException(fastHandle,
        MustFallbackException.class, fallbackSubInvoke));
    return site;
  }

  /**
   * Add two objects, assuming that each is an Integer and return the result as an Integer or a
   * Double depending on size of the result. If either is not an Integer,
   * throw MustFallbackException.
   */
  public static Object integerAdd(Object o1, Object o2, Context cx) {
    try {
      // Do addition using 64-bit values to account for overflow
      // since JavaScript has only one "number" type
      final long i1 = (Integer) o1;
      final long i2 = (Integer) o2;
      final long r = i1 + i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        // Overflow, so return result as a Double.
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int) r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  /**
   * Same as integerAdd, but subtract.
   */
  public static Object integerSubtract(Object o1, Object o2, Context cx) {
    try {
      final long i1 = (Integer) o1;
      final long i2 = (Integer) o2;
      final long r = i1 - i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int) r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  /**
   * Subtract any two values by using JavaScript's ToNumber to turn them into floating-point
   * numbers, and return the result as a Double.
   */
  public static Object genericSubtract(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 - d2);
  }

  public static Object genericDivide(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 / d2);
  }

  /**
   * Same as integerAdd, but multiply.
   */
  public static Object integerMultiply(Object o1, Object o2, Context cx) {
    try {
      final long i1 = (Integer) o1;
      final long i2 = (Integer) o2;
      final long r = i1 * i2;
      if ((r < Integer.MIN_VALUE) || (r > Integer.MAX_VALUE)) {
        return ScriptRuntime.wrapNumber(r);
      }
      return ScriptRuntime.wrapInt((int) r);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericMultiply(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 * d2);
  }

  public static Object genericMod(Object o1, Object o2, Context cx) {
    final double d1 = ScriptRuntime.toNumber(o1);
    final double d2 = ScriptRuntime.toNumber(o2);
    return ScriptRuntime.wrapNumber(d1 % d2);
  }

  /**
   * Apply the bitwise "or" operation on two objects, assuming that both are Integers, and return
   * the result as an Integer. If either is not an Integer, throw MustFallbackException.
   */
  public static Object integerOr(Object o1, Object o2, Context cx) {
    try {
      final int i1 = (Integer) o1;
      final int i2 = (Integer) o2;
      return ScriptRuntime.wrapInt(i1 | i2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericOr(Object o1, Object o2, Context cx) {
    final int i1 = ScriptRuntime.toInt32(o1);
    final int i2 = ScriptRuntime.toInt32(o2);
    return ScriptRuntime.wrapInt(i1 | i2);
  }

  /**
   * Same as integerOr, but for an Xor.
   */
  public static Object integerXor(Object o1, Object o2, Context cx) {
    try {
      final int i1 = (Integer) o1;
      final int i2 = (Integer) o2;
      return ScriptRuntime.wrapInt(i1 ^ i2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericXor(Object o1, Object o2, Context cx) {
    final int i1 = ScriptRuntime.toInt32(o1);
    final int i2 = ScriptRuntime.toInt32(o2);
    return ScriptRuntime.wrapInt(i1 ^ i2);
  }

  /**
   * Same as integerOr, but for an "and".
   */
  public static Object integerAnd(Object o1, Object o2, Context cx) {
    try {
      final int i1 = (Integer) o1;
      final int i2 = (Integer) o2;
      return ScriptRuntime.wrapInt(i1 & i2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericAnd(Object o1, Object o2, Context cx) {
    final int i1 = ScriptRuntime.toInt32(o1);
    final int i2 = ScriptRuntime.toInt32(o2);
    return ScriptRuntime.wrapInt(i1 & i2);
  }

  /**
   * Same as integerOr, but for a right-shift.
   */
  public static Object integerRsh(Object o1, Object o2, Context cx) {
    try {
      final int i1 = (Integer) o1;
      final int i2 = (Integer) o2;
      return ScriptRuntime.wrapInt(i1 >> i2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericRsh(Object o1, Object o2, Context cx) {
    final int i1 = ScriptRuntime.toInt32(o1);
    final int i2 = ScriptRuntime.toInt32(o2);
    return ScriptRuntime.wrapInt(i1 >> i2);
  }

  /**
   * Same as integerOr, but for a left-shift.
   */
  public static Object integerLsh(Object o1, Object o2, Context cx) {
    try {
      final int i1 = (Integer) o1;
      final int i2 = (Integer) o2;
      return ScriptRuntime.wrapInt(i1 << i2);
    } catch (ClassCastException | NullPointerException e) {
      throw new MustFallbackException();
    }
  }

  public static Object genericLsh(Object o1, Object o2, Context cx) {
    final int i1 = ScriptRuntime.toInt32(o1);
    final int i2 = ScriptRuntime.toInt32(o2);
    return ScriptRuntime.wrapInt(i1 << i2);
  }

  /**
   * A bootstrap method that may be called by an InvokeDynamic instruction. The name
   * of the operation passed in the instruction will be used to construct the method
   * handle.
   */
  @SuppressWarnings("unused")
  public static CallSite bootstrap(MethodHandles.Lookup lookup,
      String name,
      MethodType mType) throws NoSuchMethodException, IllegalAccessException {
    switch (name) {
      case ADD_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerAdd", mType),
            lookup.findStatic(ScriptRuntime.class, "add", mType),
            mType);
      case SUBTRACT_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerSubtract", mType),
            lookup.findStatic(DynamicMathRuntime.class, "genericSubtract", mType),
            mType);
      case MULTIPLY_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerMultiply", mType),
            lookup.findStatic(DynamicMathRuntime.class, "genericMultiply", mType),
            mType);
      case DIVIDE_OP:
        // There is no integer optimization for divide and mod that makes sense to return
        // an integer result, so always use the generic method.
        return new ConstantCallSite(
            lookup.findStatic(DynamicMathRuntime.class, "genericDivide", mType));
      case MOD_OP:
        return new ConstantCallSite(
            lookup.findStatic(DynamicMathRuntime.class, "genericMod", mType));
      case AND_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerAnd", mType),
            lookup.findStatic(DynamicMathRuntime.class, "genericAnd", mType),
            mType);
      case OR_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerOr", mType),
            lookup.findStatic(DynamicMathRuntime.class, "genericOr", mType),
            mType);
      case XOR_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerXor", mType),
            lookup.findStatic(DynamicMathRuntime.class, "genericXor", mType),
            mType);
      case LSH_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerLsh", mType),
            lookup.findStatic(DynamicMathRuntime.class, "genericLsh", mType),
            mType);
      case RSH_OP:
        return makeCallSite(lookup,
            lookup.findStatic(DynamicMathRuntime.class, "integerRsh", mType),
            lookup.findStatic(DynamicMathRuntime.class, "genericRsh", mType),
            mType);
      default:
        throw Kit.codeBug("Invalid bootstrap operation: " + name);
    }
  }
}
