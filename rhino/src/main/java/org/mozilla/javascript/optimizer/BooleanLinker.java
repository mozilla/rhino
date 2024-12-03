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

/**
 * This linker short-circuits invocations of "==", "===", and "toBoolean" operations when the
 * argument is already a boolean.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class BooleanLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        return Boolean.class.equals(type);
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
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType mType = req.getCallSiteDescriptor().getMethodType();

            if (op.isOperation(RhinoOperation.EQ, RhinoOperation.SHALLOWEQ)
                    && req.getArguments()[1] instanceof Boolean) {
                mh = lookup.findStatic(BooleanLinker.class, "eq", mType);
                guard = lookup.findStatic(BooleanLinker.class, "testEq", mType);
            } else if (op.isOperation(RhinoOperation.TOBOOLEAN)) {
                mh = lookup.findStatic(BooleanLinker.class, "toBoolean", mType);
                guard = Guards.getInstanceOfGuard(Boolean.class);
            }
        }

        if (mh != null) {
            assert guard != null;
            if (DefaultLinker.DEBUG) {
                System.out.println(op + " boolean operation");
            }
            return new GuardedInvocation(mh, guard);
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static boolean testEq(Object lval, Object rval) {
        return lval instanceof Boolean && rval instanceof Boolean;
    }

    @SuppressWarnings("unused")
    private static boolean eq(Object lval, Object rval) {
        return Objects.equals(lval, rval);
    }

    @SuppressWarnings("unused")
    private static boolean toBoolean(Object raw) {
        return ((Boolean) raw);
    }
}
