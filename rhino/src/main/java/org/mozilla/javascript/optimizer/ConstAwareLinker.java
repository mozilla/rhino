package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import jdk.dynalink.NamedOperation;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.support.Guards;
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

@SuppressWarnings("AndroidJdkLibsChecker")
class ConstAwareLinker implements GuardingDynamicLinker {
    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc)
            throws Exception {
        if (req.isCallSiteUnstable()) {
            if (DefaultLinker.DEBUG) {
                System.out.println(
                        req.getCallSiteDescriptor().getOperation() + ": unstable call site");
            }
            return null;
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Operation rootOp = req.getCallSiteDescriptor().getOperation();
        MethodType mType = req.getCallSiteDescriptor().getMethodType();
        String name = DefaultLinker.getName(rootOp);
        Operation op = NamedOperation.getBaseOperation(rootOp);
        Object target = req.getReceiver();

        if (NamespaceOperation.contains(op, StandardOperation.GET, RhinoNamespace.NAME)
                || NamespaceOperation.contains(
                        op, StandardOperation.GET, StandardNamespace.PROPERTY)
                || NamespaceOperation.contains(
                        op, RhinoOperation.GETNOWARN, StandardNamespace.PROPERTY)) {
            Object constValue = getConstValue(target, name);
            if (constValue != null) {
                // The guard returns boolean and compares the first argument to the
                // target here. This works because the target is always our first argument.
                MethodHandle guard = Guards.asType(Guards.getIdentityGuard(target), mType);
                // Replace the actual method invocation with one that just returns a constant.
                // Works because we can drop all arguments here.
                MethodHandle mh =
                        MethodHandles.dropArguments(
                                MethodHandles.constant(Object.class, constValue),
                                0,
                                mType.parameterList());
                if (DefaultLinker.DEBUG) {
                    System.out.println(rootOp + " constant");
                }
                return new GuardedInvocation(mh, guard);
            }
        }

        return null;
    }

    /**
     * Return the value of the specified property, but only if it's found in the root object that we
     * search, and only if it's a constant. Return null otherwise, which means that we can't handle
     * constants with a value of "null," which should not be a big loss.
     */
    private Object getConstValue(Object t, String name) {
        if (t instanceof NativeWith) {
            // Support constants referenced from inside functions
            return getConstValue(((NativeWith) t).getPrototype(), name);
        }
        if (!(t instanceof ScriptableObject)) {
            return null;
        }
        try {
            ScriptableObject target = (ScriptableObject) t;
            // Just look in the root of the object -- don't mess around with
            // nested scopes and things, to keep this simple and foolproof.
            if (target.has(name, target)) {
                int attributes = target.getAttributes(name);
                if ((attributes & ScriptableObject.READONLY) != 0
                        && (attributes & ScriptableObject.PERMANENT) != 0
                        && (attributes & ScriptableObject.UNINITIALIZED_CONST) == 0) {
                    // If we get here then this object's value will not change for the
                    // lifetime of the target object.
                    return target.get(name, target);
                }
            }
        } catch (RhinoException re) {
            // Some implementations of ScriptableObject will fail on this operation with
            // an exception, so treat that as "not found".
        }
        return null;
    }
}
