package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;
import jdk.dynalink.linker.support.Guards;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

/**
 * This linker optimizes accesses to the "length" property of native arrays by delegating directly
 * to the native code. It helps in the common case that code is iterating over the length of an
 * array.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class NativeArrayLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        return NativeArray.class.isAssignableFrom(type);
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

        if (op.isNamespace(StandardNamespace.PROPERTY)) {
            if (op.isOperation(StandardOperation.GET, RhinoOperation.GETNOWARN)
                    && "length".equals(op.getName())) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodType mType = req.getCallSiteDescriptor().getMethodType();
                mh = lookup.findStatic(NativeArrayLinker.class, "getLength", mType);
                guard = Guards.getInstanceOfGuard(NativeArray.class);
            }
        }

        if (mh != null) {
            assert guard != null;
            if (DefaultLinker.DEBUG) {
                System.out.println(op + " native array operation");
            }
            return new GuardedInvocation(mh, guard);
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static Object getLength(Object o, Context cx, Scriptable scope) {
        long length = ((NativeArray) o).getLength();
        if (length < Integer.MAX_VALUE) {
            return (int) length;
        }
        return (double) length;
    }
}
