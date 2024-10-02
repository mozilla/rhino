package org.mozilla.javascript.optimizer;

/**
 * This class defines the method signatures for the properties used by the invokedynamic
 * instructions in the bytecode. This helps us identify what each bytecode operation means, and what
 * the method signature should be. The method signatures here don't necessarily map 1:1 with
 * ScriptRuntime operations -- the runtime will insert the value of the "name" part of the operation
 * name before making the call.
 */
interface Signatures {
    /**
     * PROP:GET:{name}: Looks up the object property named "name". Falls back to
     * ScriptRuntime.getObjectPropNoWarn.
     */
    String PROP_GET =
            "(Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Ljava/lang/Object;";

    /**
     * PROP:GETNOWARN:{name}: Looks up the object property named "name" and does not warn if it does
     * not exist. Falls back to ScriptRuntime.getObjectProp.
     */
    String PROP_GET_NOWARN =
            "(Ljava/lang/Object;Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";

    /**
     * PROP:GETWITHTHIS:{name}: Looks up an object property like PROP:GET, and sets "this" in the
     * "last stored scriptable." Falls back to ScriptRuntime.getPropFunctionAndThis.
     */
    String PROP_GET_THIS =
            "(Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Lorg/mozilla/javascript/Callable;";

    /**
     * PROP:GETINDEX: Get a property from an object based on a numeric index. Falls back to
     * ScriptRuntime.getObjectIndex.
     */
    String PROP_GET_INDEX =
            "(Ljava/lang/Object;D"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";

    /**
     * PROP:GETELEMENT: Get a property from an object based on an element ID, which could be a
     * string, number, or symbol. Falls back to ScriptRuntime.getObjectElem.
     */
    String PROP_GET_ELEMENT =
            "(Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Ljava/lang/Object;";

    /**
     * PROP:SET:{name}: Sets the named property on an object. Falls back to
     * ScriptRuntime.setObjectProp.
     */
    String PROP_SET =
            "(Ljava/lang/Object;Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";

    /**
     * PROP:SETINDEX: Set a property on an object based on a numeric index. Falls back to
     * ScriptRuntime.setObjectIndex.
     */
    String PROP_SET_INDEX =
            "(Ljava/lang/Object;"
                    + "D"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Ljava/lang/Object;";

    /**
     * PROP:SETELEMENT: Set a property on an object based on an identifier. Falls back to
     * ScriptRuntime.setObjectElem.
     */
    String PROP_SET_ELEMENT =
            "(Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Ljava/lang/Object;";

    /**
     * NAME:GET:{name}: Looks up a the named value from the scope. Falls back to ScriptRuntime.name.
     */
    String NAME_GET =
            "(Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Ljava/lang/Object;";

    /**
     * NAME:GETWITHTHIS:{name}: Looks up a name in the scope like NAME:GET, and sets "this" in the
     * "last stored scriptable." Falls back to ScriptRuntime.getNameFunctionAndThis.
     */
    String NAME_GET_THIS =
            "(Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Lorg/mozilla/javascript/Callable;";

    /** NAME:SET:{name}: Sets the named value in the scope. Falls back to ScriptRuntime.setName. */
    String NAME_SET =
            "(Lorg/mozilla/javascript/Scriptable;"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Ljava/lang/Object;";

    /**
     * NAME:BIND:{name}: Bind the named value into the current scope. Falls back to
     * ScriptRuntime.bind.
     */
    String NAME_BIND =
            "(Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Lorg/mozilla/javascript/Scriptable;";

    /**
     * NAME:SETSTRICT:{name}: Sets the named value in the scope, and enforces strict mode. Falls
     * back to ScriptRuntime.strictSetName.
     */
    String NAME_SET_STRICT =
            "(Lorg/mozilla/javascript/Scriptable;"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")Ljava/lang/Object;";

    /**
     * NAME:SETCONST:{name}: Sets the named constant in the scope. Falls back to
     * ScriptRuntime.setConst.
     */
    String NAME_SET_CONST =
            "(Lorg/mozilla/javascript/Scriptable;"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Context;"
                    + ")Ljava/lang/Object;";
}
