package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import org.mozilla.javascript.Token;
import org.mozilla.javascript.config.RhinoConfig;

/**
 * This linker is the last one in the chain, and as such it must be able to link every type of
 * operation that we support. It links every operation to the corresponding ScriptRuntime or
 * OptRuntime operation that was used in the bytecode before we introduced dynamic linking.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class DefaultLinker implements GuardingDynamicLinker {
    static final boolean DEBUG = RhinoConfig.get("rhino.debugLinker", false);

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest req, LinkerServices svc)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mType = req.getCallSiteDescriptor().getMethodType();
        ParsedOperation op = new ParsedOperation(req.getCallSiteDescriptor().getOperation());

        GuardedInvocation invocation = getInvocation(lookup, mType, op);
        if (DEBUG) {
            String t1 =
                    req.getReceiver() == null
                            ? "null"
                            : req.getReceiver().getClass().getSimpleName();
            String t2 = "";
            if (req.getArguments().length > 1 && req.getArguments()[1] != null) {
                t2 = req.getArguments()[1].getClass().getSimpleName();
            }
            System.out.println(op + "(" + t1 + ", " + t2 + "): default link");
        }
        return invocation;
    }

    private GuardedInvocation getInvocation(
            MethodHandles.Lookup lookup, MethodType mType, ParsedOperation op)
            throws NoSuchMethodException, IllegalAccessException {
        if (op.isNamespace(StandardNamespace.PROPERTY)) {
            return getPropertyInvocation(lookup, mType, op);
        } else if (op.isNamespace(RhinoNamespace.NAME)) {
            return getNameInvocation(lookup, mType, op);
        } else if (op.isNamespace(RhinoNamespace.MATH)) {
            return getMathInvocation(lookup, mType, op);
        }
        throw new UnsupportedOperationException(op.toString());
    }

    private GuardedInvocation getPropertyInvocation(
            MethodHandles.Lookup lookup, MethodType mType, ParsedOperation op)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandle mh = null;

        // The name of the property to look up is now not on the Java stack,
        // but was passed as part of the operation name in the bytecode.
        // Put the property name back in the right place in the parameter list
        // so that we can invoke the operation normally.
        if (op.isOperation(StandardOperation.GET)) {
            mh =
                    bindStringParameter(
                            lookup, mType, ScriptRuntime.class, "getObjectProp", 1, op.getName());
        } else if (op.isOperation(RhinoOperation.GETNOWARN)) {
            mh =
                    bindStringParameter(
                            lookup,
                            mType,
                            ScriptRuntime.class,
                            "getObjectPropNoWarn",
                            1,
                            op.getName());
        } else if (op.isOperation(RhinoOperation.GETSUPER)) {
            mh =
                    bindStringParameter(
                            lookup, mType, ScriptRuntime.class, "getSuperProp", 1, op.getName());
        } else if (op.isOperation(RhinoOperation.GETWITHTHIS)) {
            mh =
                    bindStringParameter(
                            lookup,
                            mType,
                            ScriptRuntime.class,
                            "getPropFunctionAndThis",
                            1,
                            op.getName());
        } else if (op.isOperation(RhinoOperation.GETWITHTHISOPTIONAL)) {
            mh =
                    bindStringParameter(
                            lookup,
                            mType,
                            ScriptRuntime.class,
                            "getPropFunctionAndThisOptional",
                            1,
                            op.getName());
        } else if (op.isOperation(StandardOperation.SET)) {
            mh =
                    bindStringParameter(
                            lookup, mType, ScriptRuntime.class, "setObjectProp", 1, op.getName());
        } else if (op.isOperation(RhinoOperation.SETSUPER)) {
            mh =
                    bindStringParameter(
                            lookup, mType, ScriptRuntime.class, "setSuperProp", 1, op.getName());
        } else if (op.isOperation(RhinoOperation.GETELEMENT)) {
            mh = lookup.findStatic(ScriptRuntime.class, "getObjectElem", mType);
        } else if (op.isOperation(RhinoOperation.GETELEMENTSUPER)) {
            mh = lookup.findStatic(ScriptRuntime.class, "getSuperElem", mType);
        } else if (op.isOperation(RhinoOperation.GETINDEX)) {
            mh = lookup.findStatic(ScriptRuntime.class, "getObjectIndex", mType);
        } else if (op.isOperation(RhinoOperation.SETELEMENT)) {
            mh = lookup.findStatic(ScriptRuntime.class, "setObjectElem", mType);
        } else if (op.isOperation(RhinoOperation.SETELEMENTSUPER)) {
            mh = lookup.findStatic(ScriptRuntime.class, "setSuperElem", mType);
        } else if (op.isOperation(RhinoOperation.SETINDEX)) {
            mh = lookup.findStatic(ScriptRuntime.class, "setObjectIndex", mType);
        }

        if (mh != null) {
            return new GuardedInvocation(mh);
        }
        // We will only get here if a new operation was introduced
        // without appropriate changes. This particular linker must never return null.
        throw new UnsupportedOperationException(op.toString());
    }

    private GuardedInvocation getNameInvocation(
            MethodHandles.Lookup lookup, MethodType mType, ParsedOperation op)
            throws NoSuchMethodException, IllegalAccessException {
        MethodType tt;
        MethodHandle mh = null;
        String name = op.getName();

        // Like above for properties, the name to handle is not on the Java stack,
        // but is something that we parsed from the name of the invokedynamic operation.
        if (op.isOperation(RhinoOperation.BIND)) {
            tt =
                    MethodType.methodType(
                            Scriptable.class, Context.class, Scriptable.class, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "bind", tt);
            mh = MethodHandles.insertArguments(mh, 2, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (op.isOperation(StandardOperation.GET)) {
            tt = MethodType.methodType(Object.class, Context.class, Scriptable.class, String.class);
            mh = lookup.findStatic(ScriptRuntime.class, "name", tt);
            mh = MethodHandles.insertArguments(mh, 2, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (op.isOperation(RhinoOperation.GETWITHTHIS)) {
            tt =
                    MethodType.methodType(
                            Callable.class, String.class, Context.class, Scriptable.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getNameFunctionAndThis", tt);
            mh = MethodHandles.insertArguments(mh, 0, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (op.isOperation(RhinoOperation.GETWITHTHISOPTIONAL)) {
            tt =
                    MethodType.methodType(
                            Callable.class, String.class, Context.class, Scriptable.class);
            mh = lookup.findStatic(ScriptRuntime.class, "getNameFunctionAndThisOptional", tt);
            mh = MethodHandles.insertArguments(mh, 0, name);
            mh = MethodHandles.permuteArguments(mh, mType, 1, 0);
        } else if (op.isOperation(StandardOperation.SET)) {
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "setName", 4, name);
        } else if (op.isOperation(RhinoOperation.SETSTRICT)) {
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "strictSetName", 4, name);
        } else if (op.isOperation(RhinoOperation.SETCONST)) {
            mh = bindStringParameter(lookup, mType, ScriptRuntime.class, "setConst", 3, name);
        }

        if (mh != null) {
            return new GuardedInvocation(mh);
        }
        throw new UnsupportedOperationException(op.toString());
    }

    private GuardedInvocation getMathInvocation(
            MethodHandles.Lookup lookup, MethodType mType, ParsedOperation op)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandle mh = null;

        if (op.isOperation(RhinoOperation.ADD)) {
            mh = lookup.findStatic(ScriptRuntime.class, "add", mType);
        } else if (op.isOperation(RhinoOperation.TONUMBER)) {
            mh = lookup.findStatic(ScriptRuntime.class, "toNumber", mType);
        } else if (op.isOperation(RhinoOperation.TONUMERIC)) {
            mh = lookup.findStatic(ScriptRuntime.class, "toNumeric", mType);
        } else if (op.isOperation(RhinoOperation.TOBOOLEAN)) {
            mh = lookup.findStatic(ScriptRuntime.class, "toBoolean", mType);
        } else if (op.isOperation(RhinoOperation.TOINT32)) {
            mh = lookup.findStatic(ScriptRuntime.class, "toInt32", mType);
        } else if (op.isOperation(RhinoOperation.TOUINT32)) {
            mh = lookup.findStatic(ScriptRuntime.class, "toUint32", mType);
        } else if (op.isOperation(RhinoOperation.EQ)) {
            mh = lookup.findStatic(ScriptRuntime.class, "eq", mType);
        } else if (op.isOperation(RhinoOperation.SHALLOWEQ)) {
            mh = lookup.findStatic(ScriptRuntime.class, "shallowEq", mType);
        } else if (op.isOperation(RhinoOperation.COMPARE_GT)) {
            mh = makeCompare(lookup, Token.GT);
        } else if (op.isOperation(RhinoOperation.COMPARE_LT)) {
            mh = makeCompare(lookup, Token.LT);
        } else if (op.isOperation(RhinoOperation.COMPARE_GE)) {
            mh = makeCompare(lookup, Token.GE);
        } else if (op.isOperation(RhinoOperation.COMPARE_LE)) {
            mh = makeCompare(lookup, Token.LE);
        }

        if (mh != null) {
            return new GuardedInvocation(mh);
        }
        throw new UnsupportedOperationException(op.toString());
    }

    /**
     * The "compare" operation in ScriptRuntime uses an integer flag to determine how to compare,
     * but in our bytecode we've split this into four separate instructions for future optimization
     * -- here, construct a method handle back to doing the things the old way as a fallback.
     */
    private MethodHandle makeCompare(MethodHandles.Lookup lookup, int op)
            throws NoSuchMethodException, IllegalAccessException {
        MethodType tt =
                MethodType.methodType(Boolean.TYPE, Object.class, Object.class, Integer.TYPE);
        MethodHandle mh = lookup.findStatic(ScriptRuntime.class, "compare", tt);
        mh = MethodHandles.insertArguments(mh, 2, op);
        return mh;
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
