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
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * This linker optimizes:
 *
 * <ul>
 *   <li>"+" operations when the LHS is a ConsString and the RHS is any kind of CharSequence object
 *       (either a String or ConsString)
 *   <li>accesses to the "length" property of a ConsString.
 * </ul>
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class ConsStringLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        return ConsString.class.equals(type);
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
                    mh = lookup.findStatic(ConsStringLinker.class, "add", mType);
                    guard = lookup.findStatic(ConsStringLinker.class, "testAdd", guardType);
                }
            }
        } else if (op.isNamespace(StandardNamespace.PROPERTY)) {
            if (op.isOperation(StandardOperation.GET, RhinoOperation.GETNOWARN)
                    && "length".equals(op.getName())) {
                mh = lookup.findStatic(ConsStringLinker.class, "getLength", mType);
                guard = Guards.getInstanceOfGuard(ConsString.class);
            }
        }

        if (mh != null) {
            assert guard != null;
            if (DefaultLinker.DEBUG) {
                System.out.println(op + " ConsString operation");
            }
            return new GuardedInvocation(mh, guard);
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static boolean testAdd(Object lval, Object rval, Context cx) {
        return lval instanceof ConsString && rval instanceof CharSequence;
    }

    @SuppressWarnings("unused")
    private static Object add(Object lval, Object rval, Context cx) {
        return new ConsString((ConsString) lval, ((CharSequence) rval).toString());
    }

    @SuppressWarnings("unused")
    private static Object getLength(Object o, Context cx, Scriptable scope) {
        return ((ConsString) o).length();
    }
}
