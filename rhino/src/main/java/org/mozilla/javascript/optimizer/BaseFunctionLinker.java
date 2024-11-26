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
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * This linker optimizes accesses to the "prototype" property of any standard Rhino function so that
 * it calls the native function rather than going through' a property name match.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class BaseFunctionLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        return BaseFunction.class.isAssignableFrom(type);
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
                    && "prototype".equals(op.getName())) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodType mType = req.getCallSiteDescriptor().getMethodType();
                mh = lookup.findStatic(BaseFunctionLinker.class, "getPrototype", mType);
                guard = Guards.getInstanceOfGuard(BaseFunction.class);
            }
        }

        if (mh != null) {
            assert guard != null;
            if (DefaultLinker.DEBUG) {
                System.out.println(op + " base function operation");
            }
            return new GuardedInvocation(mh, guard);
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static Object getPrototype(Object o, Context cx, Scriptable scope) {
        return ((BaseFunction) o).getPrototypeProperty();
    }
}
