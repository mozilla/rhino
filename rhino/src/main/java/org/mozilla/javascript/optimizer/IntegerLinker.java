package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;
import jdk.dynalink.linker.support.Guards;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

/**
 * This linker optimizes a suite of math operations when the LHS is an Integer object, and the RHS
 * (if any) is either an Integer or a Double. It avoids a gigantic set of "if...then" statements in
 * ScriptRuntime for the generic case. When operating on Integers (and there is no overflow) it and
 * ScriptRuntime contrive to return Integer results, which can result in faster operations later.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class IntegerLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        return Integer.class.equals(type);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc)
            throws Exception {
        if (req.isCallSiteUnstable()) {
            return null;
        }

        ParsedOperation op = new ParsedOperation(req.getCallSiteDescriptor().getOperation());
        MethodHandle mh = null;
        MethodHandle guard = null;

        if (op.isNamespace(RhinoNamespace.MATH)) {
            Object arg2 = null;
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType mType = req.getCallSiteDescriptor().getMethodType();

            if (req.getArguments().length > 1) {
                arg2 = req.getArguments()[1];
            }
            if (op.isOperation(RhinoOperation.ADD) && arg2 instanceof Integer) {
                mh = lookup.findStatic(IntegerLinker.class, "add", mType);
                MethodType guardType = mType.changeReturnType(Boolean.TYPE);
                guard = lookup.findStatic(IntegerLinker.class, "testAdd", guardType);
            } else if (op.isOperation(RhinoOperation.ADD) && arg2 instanceof Double) {
                mh = lookup.findStatic(IntegerLinker.class, "addDouble", mType);
                MethodType guardType = mType.changeReturnType(Boolean.TYPE);
                guard = lookup.findStatic(IntegerLinker.class, "testAddDouble", guardType);
            } else if (op.isOperation(RhinoOperation.EQ, RhinoOperation.SHALLOWEQ)
                    && arg2 instanceof Integer) {
                mh = lookup.findStatic(IntegerLinker.class, "eq", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwo", mType);
            } else if (op.isOperation(RhinoOperation.EQ, RhinoOperation.SHALLOWEQ)
                    && arg2 instanceof Double) {
                mh = lookup.findStatic(IntegerLinker.class, "eqDouble", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwoDouble", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_LT) && arg2 instanceof Integer) {
                mh = lookup.findStatic(IntegerLinker.class, "compareLT", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwo", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_GT) && arg2 instanceof Integer) {
                mh = lookup.findStatic(IntegerLinker.class, "compareGT", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwo", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_LE) && arg2 instanceof Integer) {
                mh = lookup.findStatic(IntegerLinker.class, "compareLE", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwo", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_GE) && arg2 instanceof Integer) {
                mh = lookup.findStatic(IntegerLinker.class, "compareGE", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwo", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_LT) && arg2 instanceof Double) {
                mh = lookup.findStatic(IntegerLinker.class, "compareLTDouble", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwoDouble", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_GT) && arg2 instanceof Double) {
                mh = lookup.findStatic(IntegerLinker.class, "compareGTDouble", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwoDouble", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_LE) && arg2 instanceof Double) {
                mh = lookup.findStatic(IntegerLinker.class, "compareLEDouble", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwoDouble", mType);
            } else if (op.isOperation(RhinoOperation.COMPARE_GE) && arg2 instanceof Double) {
                mh = lookup.findStatic(IntegerLinker.class, "compareGEDouble", mType);
                guard = lookup.findStatic(IntegerLinker.class, "testTwoDouble", mType);
            } else if (op.isOperation(RhinoOperation.TOBOOLEAN)) {
                mh = lookup.findStatic(IntegerLinker.class, "toBoolean", mType);
                guard = Guards.getInstanceOfGuard(Integer.class);
            } else if (op.isOperation(RhinoOperation.TONUMBER)) {
                mh = lookup.findStatic(IntegerLinker.class, "toNumber", mType);
                guard = Guards.getInstanceOfGuard(Integer.class);
            } else if (op.isOperation(RhinoOperation.TONUMERIC)) {
                mh = lookup.findStatic(IntegerLinker.class, "toNumeric", mType);
                guard = Guards.getInstanceOfGuard(Integer.class);
            } else if (op.isOperation(RhinoOperation.TOINT32)) {
                mh = lookup.findStatic(IntegerLinker.class, "toInt32", mType);
                guard = Guards.getInstanceOfGuard(Integer.class);
            } else if (op.isOperation(RhinoOperation.TOUINT32)) {
                mh = lookup.findStatic(IntegerLinker.class, "toUint32", mType);
                guard = Guards.getInstanceOfGuard(Integer.class);
            }
        }

        if (mh != null) {
            assert guard != null;
            if (DefaultLinker.DEBUG) {
                System.out.println(op + " integer operation");
            }
            return new GuardedInvocation(mh, guard);
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static boolean testAdd(Object lval, Object rval, Context cx) {
        return lval instanceof Integer && rval instanceof Integer;
    }

    @SuppressWarnings("unused")
    private static Object add(Object lval, Object rval, Context cx) {
        return ScriptRuntime.add((Integer) lval, (Integer) rval);
    }

    @SuppressWarnings("unused")
    private static boolean testAddDouble(Object lval, Object rval, Context cx) {
        return lval instanceof Integer && rval instanceof Double;
    }

    @SuppressWarnings("unused")
    private static Object addDouble(Object lval, Object rval, Context cx) {
        return ((Integer) lval).doubleValue() + (Double) rval;
    }

    @SuppressWarnings("unused")
    private static boolean testTwo(Object lval, Object rval) {
        return lval instanceof Integer && rval instanceof Integer;
    }

    @SuppressWarnings("unused")
    private static boolean testTwoDouble(Object lval, Object rval) {
        return lval instanceof Integer && rval instanceof Double;
    }

    @SuppressWarnings("unused")
    private static boolean eq(Object lval, Object rval) {
        return Objects.equals(lval, rval);
    }

    @SuppressWarnings("unused")
    private static boolean eqDouble(Object lval, Object rval) {
        return ((Integer) lval).doubleValue() == (Double) rval;
    }

    @SuppressWarnings("unused")
    private static boolean compareLT(Object lval, Object rval) {
        return ((Integer) lval) < ((Integer) rval);
    }

    @SuppressWarnings("unused")
    private static boolean compareGT(Object lval, Object rval) {
        return ((Integer) lval) > ((Integer) rval);
    }

    @SuppressWarnings("unused")
    private static boolean compareLE(Object lval, Object rval) {
        return ((Integer) lval) <= ((Integer) rval);
    }

    @SuppressWarnings("unused")
    private static boolean compareGE(Object lval, Object rval) {
        return ((Integer) lval) >= ((Integer) rval);
    }

    @SuppressWarnings("unused")
    private static boolean compareLTDouble(Object lval, Object rval) {
        return ((Integer) lval).doubleValue() < ((Double) rval);
    }

    @SuppressWarnings("unused")
    private static boolean compareGTDouble(Object lval, Object rval) {
        return ((Integer) lval).doubleValue() > ((Double) rval);
    }

    @SuppressWarnings("unused")
    private static boolean compareLEDouble(Object lval, Object rval) {
        return ((Integer) lval).doubleValue() <= ((Double) rval);
    }

    @SuppressWarnings("unused")
    private static boolean compareGEDouble(Object lval, Object rval) {
        return ((Integer) lval).doubleValue() >= ((Double) rval);
    }

    @SuppressWarnings("unused")
    private static double toNumber(Object raw) {
        return ((Integer) raw).doubleValue();
    }

    @SuppressWarnings("unused")
    private static Number toNumeric(Object raw) {
        return (Number) raw;
    }

    @SuppressWarnings("unused")
    private static boolean toBoolean(Object raw) {
        return ((Integer) raw) != 0;
    }

    @SuppressWarnings("unused")
    private static int toInt32(Object raw) {
        return ((Integer) raw);
    }

    @SuppressWarnings("unused")
    private static long toUint32(Object raw) {
        return Integer.toUnsignedLong((Integer) raw);
    }
}
