package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;
import jdk.dynalink.linker.support.Guards;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;

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
        return ScriptableObject.class.isAssignableFrom(type);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc)
            throws Exception {
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

                WeakReference<Object> targetRef = new WeakReference<>(target);
                WeakReference<Object> valueRef = new WeakReference<>(constValue);

                // Create a guard that verifies that both the target and the constant
                // value are still reachable.
                MethodHandle guard =
                        MethodHandles.lookup()
                                .findStatic(
                                        ConstAwareLinker.class,
                                        "testConst",
                                        MethodType.methodType(
                                                boolean.class,
                                                WeakReference.class,
                                                WeakReference.class,
                                                Object.class));

                guard = MethodHandles.insertArguments(guard, 0, targetRef, valueRef);
                guard = Guards.asType(guard, mType);

                // Replace the actual method invocation with one that looks up the weak
                // reference, or falls back to the standard lookup if it has been collected.
                // The guard above only proves that the value was still reachable when the guard
                // ran; the garbage collector may clear the reference between the guard and this
                // call, so the fallback is essential. Without it a read of a constant property
                // could return "null" instead of the constant's value.
                MethodHandles.Lookup lk = MethodHandles.lookup();
                MethodHandle getConst;
                if (op.isNamespace(StandardNamespace.PROPERTY)) {
                    getConst =
                            lk.findStatic(
                                    ConstAwareLinker.class,
                                    "getConstProp",
                                    MethodType.methodType(
                                            Object.class,
                                            WeakReference.class,
                                            String.class,
                                            Object.class,
                                            Context.class,
                                            VarScope.class));
                } else {
                    getConst =
                            lk.findStatic(
                                    ConstAwareLinker.class,
                                    "getConstName",
                                    MethodType.methodType(
                                            Object.class,
                                            WeakReference.class,
                                            String.class,
                                            VarScope.class,
                                            Context.class));
                }
                MethodHandle mh =
                        MethodHandles.insertArguments(getConst, 0, valueRef, op.getName());
                mh = mh.asType(mType);
                if (DefaultLinker.DEBUG) {
                    System.out.println(op + ": constant");
                }
                return new GuardedInvocation(mh, guard);
            }
        }

        return null;
    }

    @SuppressWarnings("unused")
    private static boolean testConst(
            WeakReference<Object> targetRef, WeakReference<Object> valueRef, Object receiver) {
        return receiver == targetRef.get() && valueRef.get() != null;
    }

    @SuppressWarnings("unused")
    private static Object getConstProp(
            WeakReference<Object> valueRef,
            String name,
            Object receiver,
            Context cx,
            VarScope scope) {
        Object value = valueRef.get();
        if (value != null) {
            return value;
        }
        // The garbage collector cleared the reference after the guard was checked, so the value
        // has to be looked up again instead of returning null for a property that exists.
        return ScriptRuntime.getObjectProp(receiver, name, cx, scope);
    }

    @SuppressWarnings("unused")
    private static Object getConstName(
            WeakReference<Object> valueRef, String name, VarScope scope, Context cx) {
        Object value = valueRef.get();
        if (value != null) {
            return value;
        }
        return ScriptRuntime.name(cx, scope, name);
    }

    /**
     * Return the value of the specified property, but only if it's found in the root object that we
     * search, and only if it's a constant. Return null otherwise, which means that we can't handle
     * constants with a value of "null," which should not be a big loss.
     */
    private Object getConstValue(Object t, String name) {
        assert t instanceof ScriptableObject;
        try {
            ScriptableObject target = (ScriptableObject) t;
            // Just look in the root of the object -- don't mess around with
            // nested scopes and things, to keep this simple and foolproof.
            if (target.has(name, target)) {
                int attributes = target.getAttributes(name);
                if ((attributes & ScriptableObject.READONLY) != 0
                        && (attributes & ScriptableObject.PERMANENT) != 0
                        && (attributes & ScriptableObject.UNINITIALIZED_CONST) == 0
                        // An accessor property with a getter can return a different value
                        // on each invocation.
                        && target.getGetterOrSetter(name, 0, target, false) == Undefined.instance) {
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
