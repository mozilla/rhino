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
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/**
 * This linker is the last one in the chain, and as such it must be able to link every type of
 * operation that we support. It links every operation to the corresponding ScriptRuntime or
 * OptRuntime operation that was used in the bytecode before we introduced dynamic linking.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
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
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "getObjectProp", 1, name);
        } else if (RhinoOperation.GETNOWARN.equals(op)) {
            mh =
                    bindStringParameter(
                            lookup, mType, ScriptRuntime.class, "getObjectPropNoWarn", 1, name);
        } else if (RhinoOperation.GETWITHTHIS.equals(op)) {
            mh =
                    bindStringParameter(
                            lookup, mType, ScriptRuntime.class, "getPropFunctionAndThis", 1, name);
        } else if (RhinoOperation.GETWITHTHISOPTIONAL.equals(op)) {
            mh =
                    bindStringParameter(
                            lookup,
                            mType,
                            ScriptRuntime.class,
                            "getPropFunctionAndThisOptional",
                            1,
                            name);
        } else if (StandardOperation.SET.equals(op)) {
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "setObjectProp", 1, name);
        } else if (RhinoOperation.GETELEMENT.equals(op)) {
            mh = lookup.findStatic(ScriptRuntime.class, "getObjectElem", mType);
        } else if (RhinoOperation.GETINDEX.equals(op)) {
            mh = lookup.findStatic(ScriptRuntime.class, "getObjectIndex", mType);
        } else if (RhinoOperation.SETELEMENT.equals(op)) {
            mh = lookup.findStatic(ScriptRuntime.class, "setObjectElem", mType);
        } else if (RhinoOperation.SETINDEX.equals(op)) {
            mh = lookup.findStatic(ScriptRuntime.class, "setObjectIndex", mType);
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
            tt =
                    MethodType.methodType(
                            Scriptable.class, Context.class, Scriptable.class, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "bind", tt);
            mh = MethodHandles.insertArguments(mh, 2, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (StandardOperation.GET.equals(op)) {
            tt = MethodType.methodType(Object.class, Context.class, Scriptable.class, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "name", tt);
            mh = MethodHandles.insertArguments(mh, 2, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (RhinoOperation.GETWITHTHIS.equals(op)) {
            tt =
                    MethodType.methodType(
                            Callable.class, String.class, Context.class, Scriptable.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getNameFunctionAndThis", tt);
            mh = MethodHandles.insertArguments(mh, 0, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (RhinoOperation.GETWITHTHISOPTIONAL.equals(op)) {
            tt =
                    MethodType.methodType(
                            Callable.class, String.class, Context.class, Scriptable.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getNameFunctionAndThisOptional", tt);
            mh = MethodHandles.insertArguments(mh, 0, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (StandardOperation.SET.equals(op)) {
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "setName", 4, name);
        } else if (RhinoOperation.SETSTRICT.equals(op)) {
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "strictSetName", 4, name);
        } else if (RhinoOperation.SETCONST.equals(op)) {
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "setConst", 3, name);
        }

        if (mh != null) {
            return new GuardedInvocation(mh);
        }
        throw new UnsupportedOperationException(rootOp.toString());
    }

    /** If the operation is a named operation, then return the name, */
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

    /**
     * Given a class name and a method name, do the following:
     *
     * <ol>
     *   <li>insert a String parameter into the method type at "index"
     *   <li>Lookup a named static method from the specified class using the new type
     *   <li>Bind a string constant to the method handle at "index"
     *   <li>Return the bound handle
     * </ol>
     */
    private static MethodHandle bindStringParameter(
            MethodHandles.Lookup lookup,
            MethodType mt,
            Class<?> cls,
            String method,
            int index,
            String nameParam)
            throws NoSuchMethodException, IllegalAccessException {
        MethodType actualType = mt.insertParameterTypes(index, String.class);
        MethodHandle mh = lookup.findStatic(cls, method, actualType);
        return MethodHandles.insertArguments(mh, index, nameParam);
    }
}
