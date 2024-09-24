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
import org.mozilla.javascript.ScriptRuntime;

/**
 * This linker is the last one in the chain, and as such it must be able to link every type of
 * operation that we support. It links every operation to the corresponding ScriptRuntime or
 * OptRuntime operation that was used in the bytecode before we introduced dynamic linking.
 */
class DefaultLinker implements GuardingDynamicLinker {
    static final boolean DEBUG;

    static {
        String debugVal = System.getProperty("RHINO_DEBUG_LINKER");
        if (debugVal == null) {
            debugVal = System.getenv("RHINO_DEBUG_LINKER");
        }
        DEBUG = Boolean.parseBoolean(debugVal);
    }

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Operation rootOp = req.getCallSiteDescriptor().getOperation();
        MethodType mType = req.getCallSiteDescriptor().getMethodType();

        // So far, every operation in our application is a NamedOperation, but if
        // we add non-named operations in the future, this will still work because
        // "getName" will return an empty string and "Op" will return "rootOp".
        String name = getName(rootOp);
        Operation op = NamedOperation.getBaseOperation(rootOp);

        // In our application, every operation has a namespace.
        if (!(op instanceof NamespaceOperation)) {
            throw new UnsupportedOperationException(op.toString());
        }
        NamespaceOperation nsOp = (NamespaceOperation) op;
        op = NamespaceOperation.getBaseOperation(op);

        GuardedInvocation invocation = getInvocation(lookup, mType, rootOp, nsOp, op, name);
        if (DEBUG) {
            System.out.println(rootOp + ": default link");
        }
        return invocation;
    }

    private GuardedInvocation getInvocation(
            MethodHandles.Lookup lookup,
            MethodType mType,
            Operation rootOp,
            NamespaceOperation nsOp,
            Operation op,
            String name)
            throws NoSuchMethodException, IllegalAccessException {
        if (nsOp.contains(StandardNamespace.PROPERTY)) {
            return getPropertyInvocation(lookup, mType, rootOp, op, name);
        } else if (nsOp.contains(RhinoNamespace.NAME)) {
            return getNameInvocation(lookup, mType, rootOp, op, name);
        }
        throw new UnsupportedOperationException(rootOp.toString());
    }

    private GuardedInvocation getPropertyInvocation(
            MethodHandles.Lookup lookup,
            MethodType mType,
            Operation rootOp,
            Operation op,
            String name)
            throws NoSuchMethodException, IllegalAccessException {
        MethodType tt;
        MethodHandle mh = null;

        // The name of the property to look up is now not on the Java stack,
        // but was passed as part of the operation name in the bytecode.
        // Put the property name back in the right place in the parameter list
        // so that we can invoke the operation normally.
        if (StandardOperation.GET.equals(op)) {
            tt = mType.insertParameterTypes(1, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getObjectProp", tt);
            mh = MethodHandles.insertArguments(mh, 1, name);
        } else if (RhinoOperation.GETNOWARN.equals(op)) {
            tt = mType.insertParameterTypes(1, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getObjectPropNoWarn", tt);
            mh = MethodHandles.insertArguments(mh, 1, name);
        } else if (RhinoOperation.GETWITHTHIS.equals(op)) {
            tt = mType.insertParameterTypes(1, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getPropFunctionAndThis", tt);
            mh = MethodHandles.insertArguments(mh, 1, name);
        } else if (StandardOperation.SET.equals(op)) {
            tt = mType.insertParameterTypes(1, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "setObjectProp", tt);
            mh = MethodHandles.insertArguments(mh, 1, name);
        }

        if (mh != null) {
            return new GuardedInvocation(mh);
        }
        // We will only get here if a new operation was introduced
        // without appropriate changes. This particular linker must never return null.
        throw new UnsupportedOperationException(rootOp.toString());
    }

    private GuardedInvocation getNameInvocation(
            MethodHandles.Lookup lookup,
            MethodType mType,
            Operation rootOp,
            Operation op,
            String name)
            throws NoSuchMethodException, IllegalAccessException {
        MethodType tt;
        MethodHandle mh = null;

        // Like above for properties, the name to handle is not on the Java stack,
        // but is something that we parsed from the name of the invokedynamic operation.
        if (RhinoOperation.BIND.equals(op)) {
            tt = mType.insertParameterTypes(2, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "bind", tt);
            mh = MethodHandles.insertArguments(mh, 2, name);
        } else if (StandardOperation.GET.equals(op)) {
            tt = mType.insertParameterTypes(2, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "name", tt);
            mh = MethodHandles.insertArguments(mh, 2, name);
        } else if (RhinoOperation.GETWITHTHIS.equals(op)) {
            tt = mType.insertParameterTypes(0, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getNameFunctionAndThis", tt);
            mh = MethodHandles.insertArguments(mh, 0, name);
        }

        if (mh != null) {
            return new GuardedInvocation(mh);
        }
        throw new UnsupportedOperationException(rootOp.toString());
    }

    // If the operation is a named operation, then return the name,
    // or the empty string if it's not.
    static String getName(Operation op) {
        Object nameObj = NamedOperation.getName(op);
        if (nameObj instanceof String) {
            return (String) nameObj;
        } else if (nameObj != null) {
            throw new UnsupportedOperationException(op.toString());
        } else {
            return "";
        }
    }
}
