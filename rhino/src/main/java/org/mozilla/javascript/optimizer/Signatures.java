package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import java.lang.invoke.MethodType;

/**
 * This class defines the method signatures for the properties used by the invokedynamic
 * instructions in the bytecode. This helps us identify what each bytecode operation means, and what
 * the method signature should be. The method signatures here don't necessarily map 1:1 with
 * ScriptRuntime operations -- the runtime will insert the value of the "name" part of the operation
 * name before making the call. Also, many of the "name" operations have a different signature than
 * the ScriptRuntime equivalents because in these signatures we are trying to make the "target" of
 * each operation the first argument to make future optimizations easier.
 */
interface Signatures {
    static String sig(Class<?> returnType, Class<?>... paramTypes) {
        return MethodType.methodType(returnType, paramTypes).toMethodDescriptorString();
    }

    /**
     * PROP:GET:{name}: Looks up the object property named "name". Falls back to
     * ScriptRuntime.getObjectPropNoWarn.
     */
    String PROP_GET = sig(Object.class, Object.class, Context.class, Scriptable.class);

    /**
     * PROP:GETNOWARN:{name}: Looks up the object property named "name" and does not warn if it does
     * not exist. Falls back to ScriptRuntime.getObjectProp.
     */
    String PROP_GET_NOWARN = sig(Object.class, Object.class, Context.class, Scriptable.class);

    /**
     * PROP:GET_SUPER:{name}: Looks up the super property named "name". Falls back to
     * ScriptRuntime.getSuperProp.
     */
    String PROP_GET_SUPER =
            sig(
                    Object.class,
                    Object.class, // superObj
                    Context.class, // cx
                    Scriptable.class, // scope
                    Object.class, // thisObj
                    boolean.class // noWarn
                    );

    String PROP_GET_THIS =
            sig(ScriptRuntime.LookupResult.class, Object.class, Context.class, Scriptable.class);

    /**
     * PROP:GETINDEX: Get a property from an object based on a numeric index. Falls back to
     * ScriptRuntime.getObjectIndex.
     */
    String PROP_GET_INDEX =
            sig(Object.class, Object.class, double.class, Context.class, Scriptable.class);

    /**
     * PROP:GETELEMENT: Get a property from an object based on an element ID, which could be a
     * string, number, or symbol. Falls back to ScriptRuntime.getObjectElem.
     */
    String PROP_GET_ELEMENT =
            sig(Object.class, Object.class, Object.class, Context.class, Scriptable.class);

    /**
     * PROP:GETELEMENTSUPER: Get a property from super based on an element ID, which could be a
     * string, number, or symbol. Falls back to ScriptRuntime.getSuperElem.
     */
    String PROP_GET_ELEMENT_SUPER =
            sig(
                    Object.class,
                    Object.class,
                    Object.class,
                    Context.class,
                    Scriptable.class,
                    Object.class);

    /**
     * PROP:SET:{name}: Sets the named property on an object. Falls back to
     * ScriptRuntime.setObjectProp.
     */
    String PROP_SET = sig(Object.class, Object.class, Object.class, Context.class, Scriptable.class);

    /**
     * PROP:SETSUPER:{name}: Sets the named property on super. Falls back to
     * ScriptRuntime.setSuperProp.
     */
    String PROP_SET_SUPER =
            sig(
                    Object.class,
                    Object.class,
                    Object.class,
                    Context.class,
                    Scriptable.class,
                    Object.class);

    /**
     * PROP:SETINDEX: Set a property on an object based on a numeric index. Falls back to
     * ScriptRuntime.setObjectIndex.
     */
    String PROP_SET_INDEX =
            sig(
                    Object.class,
                    Object.class,
                    double.class,
                    Object.class,
                    Context.class,
                    Scriptable.class);

    /**
     * PROP:SETELEMENT: Set a property on an object based on an identifier. Falls back to
     * ScriptRuntime.setObjectElem.
     */
    String PROP_SET_ELEMENT =
            sig(
                    Object.class,
                    Object.class,
                    Object.class,
                    Object.class,
                    Context.class,
                    Scriptable.class);

    /**
     * PROP:SETELEMENTSUPER: Set a property on super based on an identifier. Falls back to
     * ScriptRuntime.setSuperElem.
     */
    String PROP_SET_ELEMENT_SUPER =
            sig(
                    Object.class,
                    Object.class,
                    Object.class,
                    Object.class,
                    Context.class,
                    Scriptable.class,
                    Object.class);

    /**
     * NAME:GET:{name}: Looks up a the named value from the scope. Falls back to ScriptRuntime.name.
     * Compared to that function, this version of the signature puts the "scope" first in the
     * argument list rather than second. This makes it easier for future linkers to work because
     * they can always assume that the "receiver" of an operation is the first argument.
     */
    String NAME_GET = sig(Object.class, Scriptable.class, Context.class);

    /**
     * NAME:GETWITHTHIS:{name}: Looks up a name in the scope like NAME:GET, and sets "this" in the
     * "last stored scriptable." Falls back to ScriptRuntime.getNameFunctionAndThis. Also, this
     * version of the signature makes the scope the first argument, as described above. Also,
     * NAME:GETWITHTHISOPTIONAL:{name} has different semantics for an optional function call, but it
     * uses this same signature.
     */
    String NAME_GET_THIS = sig(ScriptRuntime.LookupResult.class, Scriptable.class, Context.class);

    /** NAME:SET:{name}: Sets the named value in the scope. Falls back to ScriptRuntime.setName. */
    String NAME_SET =
            sig(Object.class, Scriptable.class, Object.class, Context.class, Scriptable.class);

    /**
     * NAME:BIND:{name}: Bind the named value into the current scope. Falls back to
     * ScriptRuntime.bind. Like some methods above, this version of the signature puts the scope
     * first in the argument list so that future linkers can have a consistent place to find the
     * "receiver".
     */
    String NAME_BIND = sig(Scriptable.class, Scriptable.class, Context.class);

    /**
     * NAME:SETSTRICT:{name}: Sets the named value in the scope, and enforces strict mode. Falls
     * back to ScriptRuntime.strictSetName.
     */
    String NAME_SET_STRICT =
            sig(Object.class, Scriptable.class, Object.class, Context.class, Scriptable.class);

    /**
     * NAME:SETCONST:{name}: Sets the named constant in the scope. Falls back to
     * ScriptRuntime.setConst.
     */
    String NAME_SET_CONST = sig(Object.class, Scriptable.class, Object.class, Context.class);

    /**
     * MATH:ADD: Add the first two arguments on the stack, which could be numbers, strings, or
     * really just about anything.
     */
    String MATH_ADD = sig(Object.class, Object.class, Object.class, Context.class);

    /** MATH:TOBOOLEAN: Make the object into a primitive boolean. */
    String MATH_TO_BOOLEAN = sig(boolean.class, Object.class);

    /** MATH:EQ: Are the two arguments equal? */
    String MATH_EQ = sig(boolean.class, Object.class, Object.class);

    /** MATH:SHALLOWEQ: Like EQ but not. */
    String MATH_SHALLOW_EQ = MATH_EQ;

    /**
     * A list of four possible compare operations, that all share the same signature:
     *
     * <ul>
     *   <li>MATH:COMPAREGT
     *   <li>MATH:COMPARELT
     *   <li>MATH:COMPAREGE
     *   <li>MATH:COMPARELE
     * </ul>
     */
    String MATH_COMPARE = sig(boolean.class, Object.class, Object.class);

    /** MATH:TONUMBER: Convert the object to a Java "double". */
    String MATH_TO_NUMBER = sig(double.class, Object.class);

    /** MATH:TONUMERIC: Convert the object to a Java "Number". */
    String MATH_TO_NUMERIC = sig(Number.class, Object.class);

    /** MATH:TOINT32: Convert the object to a Java "int". */
    String MATH_TO_INT32 = sig(int.class, Object.class);

    /** MATH:TOUINT32: Convert the object to a Java "long" that represents an unsigned integer. */
    String MATH_TO_UINT32 = sig(long.class, Object.class);
}
