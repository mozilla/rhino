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
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

/**
 * This linker optimizes accesses to constants, either as object properties or in the current scope.
 * These constants must be truly constants, which means neither "writable" nor "configurable," which
 * pretty much means that they were declared with the "const" keyword. In those cases, it will
 * replace the entire property lookup with code that directly returns the constant value, which is
 * much faster.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class ConstAwareLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        return ScriptableObject.class.isAssignableFrom(type)
                || NativeWith.class.isAssignableFrom(type);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc) {
        if (req.isCallSiteUnstable()) {
            return null;
        }

        ParsedOperation op = new ParsedOperation(req.getCallSiteDescriptor().getOperation());
        Object target = req.getReceiver();

        if ((op.isNamespace(RhinoNamespace.NAME) && op.isOperation(StandardOperation.GET))
                || (op.isNamespace(StandardNamespace.PROPERTY)
                        && op.isOperation(StandardOperation.GET, RhinoOperation.GETNOWARN))) {
            Object constValue = getConstValue(target, op.getName());
            if (constValue != null) {
                MethodType mType = req.getCallSiteDescriptor().getMethodType();
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
                    System.out.println(op + ": constant");
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
        assert t instanceof ScriptableObject;
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
