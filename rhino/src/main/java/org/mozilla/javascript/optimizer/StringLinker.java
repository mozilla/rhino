package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;

@SuppressWarnings("AndroidJdkLibsChecker")
class StringLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        return String.class.equals(type);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc)
            throws Exception {
        if (req.isCallSiteUnstable()) {
            return null;
        }
        Object arg2 = null;
        if (req.getArguments().length > 1) {
            arg2 = req.getArguments()[1];
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        ParsedOperation op = new ParsedOperation(req.getCallSiteDescriptor().getOperation());
        MethodType mType = req.getCallSiteDescriptor().getMethodType();
        MethodHandle mh = null;
        MethodHandle guard = null;

        if (op.isNamespace(RhinoNamespace.MATH)) {
            if (op.isOperation(RhinoOperation.ADD)) {
                MethodType guardType = mType.changeReturnType(Boolean.TYPE);
                if (arg2 instanceof CharSequence) {
                    mh = lookup.findStatic(StringLinker.class, "add", mType);
                    guard = lookup.findStatic(StringLinker.class, "testAdd", guardType);
                }
            } else if (op.isOperation(RhinoOperation.EQ, RhinoOperation.SHALLOWEQ)
                    && (arg2 instanceof String)) {
                mh = lookup.findStatic(StringLinker.class, "eq", mType);
                guard = lookup.findStatic(StringLinker.class, "testEq", mType);
            }
        }

        if (mh != null) {
            assert guard != null;
            if (DefaultLinker.DEBUG) {
                System.out.println(op + " string operation");
            }
            return new GuardedInvocation(mh, guard);
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static boolean testAdd(Object lval, Object rval, Context cx) {
        return lval instanceof String && rval instanceof CharSequence;
    }

    @SuppressWarnings("unused")
    private static Object add(Object lval, Object rval, Context cx) {
        return new ConsString((String) lval, ((CharSequence) rval).toString());
    }

    @SuppressWarnings("unused")
    private static boolean testEq(Object lVal, Object rval) {
        return lVal instanceof String && rval instanceof String;
    }

    @SuppressWarnings("unused")
    private static boolean eq(Object lVal, Object rval) {
        return Objects.equals(lVal, rval);
    }
}
