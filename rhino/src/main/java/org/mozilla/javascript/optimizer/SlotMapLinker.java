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
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptRuntime.LookupResult;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SlotMapOwner;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;

/**
 * Optimizes named property reads on objects backed by a slot map (every {@code ScriptableObject},
 * via {@link SlotMapOwner}) by querying the owner's own slot map directly instead of going through
 * the generic prototype-chain walk in {@code ScriptableObject.getProperty}. The common case -- an
 * own data property on a plain object -- resolves in a single hash probe and a field load, and the
 * direct call lets the JIT inline the lookup.
 *
 * <p>As with the other linkers, the guard on each guarded invocation checks every precondition the
 * target method assumes ({@link #testSlotMapReceiver}): the receiver is a slot map owner that is
 * not a scope object. When the guard fails at runtime the call site is re-linked and the rest of
 * the linker chain (ending in {@code DefaultLinker}) handles the receiver: scope objects get the
 * variable-lookup semantics of {@code ScriptRuntime.getObjectProp}, and non-slot-map receivers
 * (primitives, Java objects, null) get the generic conversion and error handling. The target
 * methods therefore never fall back based on the receiver's type. Operations this linker does not
 * handle return {@code null} so that the other linkers can claim the call site.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class SlotMapLinker implements TypeBasedGuardingDynamicLinker {
    @Override
    public boolean canLinkType(Class<?> type) {
        // Scope objects must keep the variable-lookup semantics that
        // ScriptRuntime.getObjectProp implements for VarScope receivers.
        return SlotMapOwner.class.isAssignableFrom(type) && !VarScope.class.isAssignableFrom(type);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc)
            throws NoSuchMethodException, IllegalAccessException {
        if (req.isCallSiteUnstable()) {
            return null;
        }

        ParsedOperation op = new ParsedOperation(req.getCallSiteDescriptor().getOperation());
        if (!op.isNamespace(StandardNamespace.PROPERTY)) {
            return null;
        }

        boolean isGet = op.isOperation(StandardOperation.GET);
        boolean isGetNoWarn = op.isOperation(RhinoOperation.GETNOWARN);
        boolean isGetWithThis = op.isOperation(RhinoOperation.GETWITHTHIS);
        boolean isGetWithThisOptional = op.isOperation(RhinoOperation.GETWITHTHISOPTIONAL);
        if (!(isGet || isGetNoWarn || isGetWithThis || isGetWithThisOptional)) {
            return null;
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mType = req.getCallSiteDescriptor().getMethodType();
        String method;
        if (isGetWithThis) {
            method = "getPropAndThis";
        } else if (isGetWithThisOptional) {
            method = "getPropAndThisOptional";
        } else if (isGetNoWarn) {
            method = "getPropNoWarn";
        } else {
            method = "getProp";
        }
        MethodType actualType = mType.insertParameterTypes(1, String.class);
        MethodHandle mh = lookup.findStatic(SlotMapLinker.class, method, actualType);
        mh = MethodHandles.insertArguments(mh, 1, op.getName());
        // The guard must check every precondition the target methods assume; when it fails the
        // call site is re-linked and the other linkers get a chance at this receiver.
        MethodHandle guard =
                lookup.findStatic(
                        SlotMapLinker.class,
                        "testSlotMapReceiver",
                        mType.changeReturnType(Boolean.TYPE));
        if (DefaultLinker.DEBUG) {
            System.out.println(op + " slot map get");
        }
        return new GuardedInvocation(mh, guard);
    }

    /**
     * Guard for every target method below: the receiver must be a slot map owner and not a scope
     * object. The only SlotMapOwners that are not VarScopes are ScriptableObjects, so the targets
     * may cast the receiver to Scriptable.
     */
    @SuppressWarnings("unused")
    private static boolean testSlotMapReceiver(Object obj, Context cx, VarScope scope) {
        return obj instanceof SlotMapOwner && !(obj instanceof VarScope);
    }

    /**
     * Reads a named property directly from the owner's slot map, then walks the prototype chain
     * without re-querying the owner's own map. The name parameter is bound from the call site
     * descriptor before this method is invoked.
     *
     * <p>This and the other target methods below are referenced by name via {@code
     * MethodHandles.findStatic} in {@link #getGuardedInvocation}, so the "unused" suppression is
     * required.
     */
    @SuppressWarnings({"unused", "unchecked"})
    private static Object getProp(Object obj, String name, Context cx, VarScope scope) {
        Scriptable sobj = (Scriptable) obj;
        SlotMapOwner<Scriptable> owner = (SlotMapOwner<Scriptable>) obj;
        Object value = owner.get(name, sobj);
        Scriptable proto = sobj.getPrototype();
        while (proto != null && value == Scriptable.NOT_FOUND) {
            value = proto.get(name, sobj);
            proto = proto.getPrototype();
        }
        if (value == Scriptable.NOT_FOUND) {
            if (cx.hasFeature(Context.FEATURE_STRICT_MODE)) {
                Context.reportWarning(ScriptRuntime.getMessageById("msg.ref.undefined.prop", name));
            }
            return Undefined.instance;
        }
        return value;
    }

    @SuppressWarnings({"unused", "unchecked"})
    private static Object getPropNoWarn(Object obj, String name, Context cx, VarScope scope) {
        Scriptable sobj = (Scriptable) obj;
        SlotMapOwner<Scriptable> owner = (SlotMapOwner<Scriptable>) obj;
        Object value = owner.get(name, sobj);
        Scriptable proto = sobj.getPrototype();
        while (proto != null && value == Scriptable.NOT_FOUND) {
            value = proto.get(name, sobj);
            proto = proto.getPrototype();
        }
        if (value == Scriptable.NOT_FOUND) {
            return Undefined.instance;
        }
        return value;
    }

    @SuppressWarnings({"unused", "unchecked"})
    private static LookupResult getPropAndThis(
            Object obj, String name, Context cx, VarScope scope) {
        Scriptable sobj = (Scriptable) obj;
        SlotMapOwner<Scriptable> owner = (SlotMapOwner<Scriptable>) obj;
        Object value = owner.get(name, sobj);
        Scriptable proto = sobj.getPrototype();
        while (proto != null && value == Scriptable.NOT_FOUND) {
            value = proto.get(name, sobj);
            proto = proto.getPrototype();
        }
        if (value == Scriptable.NOT_FOUND) {
            // Complete miss: fall back to the full lookup, which also performs the
            // legacy __noSuchMethod__ search.
            return ScriptRuntime.getPropAndThis(obj, name, cx, scope);
        }
        return LookupResult.newLookupResult(value, sobj, name);
    }

    @SuppressWarnings({"unused", "unchecked"})
    private static LookupResult getPropAndThisOptional(
            Object obj, String name, Context cx, VarScope scope) {
        Scriptable sobj = (Scriptable) obj;
        SlotMapOwner<Scriptable> owner = (SlotMapOwner<Scriptable>) obj;
        Object value = owner.get(name, sobj);
        Scriptable proto = sobj.getPrototype();
        while (proto != null && value == Scriptable.NOT_FOUND) {
            value = proto.get(name, sobj);
            proto = proto.getPrototype();
        }
        if (value == Scriptable.NOT_FOUND) {
            // Complete miss: fall back to the full lookup, which also performs the
            // legacy __noSuchMethod__ search.
            return ScriptRuntime.getPropAndThisOptional(obj, name, cx, scope);
        }
        if (!(value instanceof Callable) && (value == null || Undefined.isUndefined(value))) {
            return null;
        }
        return LookupResult.newLookupResult(value, sobj, name);
    }
}
