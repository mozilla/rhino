/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.EnumSet;

/**
 * The base class for Function objects. That is one of two purposes. It is also the prototype for
 * every "function" defined except those that are used as GeneratorFunctions via the ES6 "function
 * *" syntax.
 *
 * <p>See ECMA 15.3.
 *
 * @author Norris Boyd
 */
public class BaseFunction extends IdScriptableObject implements Function {
    private static final long serialVersionUID = 5311394446546053859L;

    private static final Object FUNCTION_TAG = "Function";
    private static final String FUNCTION_CLASS = "Function";
    static final String GENERATOR_FUNCTION_CLASS = "__GeneratorFunction";

    static void init(Context cx, Scriptable scope, boolean sealed) {
        BaseFunction obj = new BaseFunction();
        // Function.prototype attributes: see ECMA 15.3.3.1
        obj.prototypePropertyAttributes = DONTENUM | READONLY | PERMANENT;
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            obj.setStandardPropertyAttributes(READONLY | DONTENUM);
        }
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    /**
     * @deprecated Use {@link #init(Context, Scriptable, boolean)} instead
     */
    @Deprecated
    static void init(Scriptable scope, boolean sealed) {
        init(Context.getContext(), scope, sealed);
    }

    static Object initAsGeneratorFunction(Scriptable scope, boolean sealed) {
        BaseFunction obj = new BaseFunction(true);
        // Function.prototype attributes: see ECMA 15.3.3.1
        obj.prototypePropertyAttributes = READONLY | PERMANENT;
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
        // The "GeneratorFunction" name actually never appears in the global scope.
        // Return it here so it can be cached as a "builtin"
        return ScriptableObject.getProperty(scope, GENERATOR_FUNCTION_CLASS);
    }

    public BaseFunction() {}

    public BaseFunction(boolean isGenerator) {
        this.isGeneratorFunction = isGenerator;
    }

    public BaseFunction(Scriptable scope, Scriptable prototype) {
        super(scope, prototype);
    }

    @Override
    public String getClassName() {
        return isGeneratorFunction() ? GENERATOR_FUNCTION_CLASS : FUNCTION_CLASS;
    }

    // Generated code will override this
    protected boolean isGeneratorFunction() {
        return isGeneratorFunction;
    }

    // Generated code will override this
    protected boolean hasDefaultParameters() {
        return false;
    }

    /**
     * Gets the value returned by calling the typeof operator on this object.
     *
     * @see ScriptableObject#getTypeOf()
     * @return "function" or "undefined" if {@link #avoidObjectDetection()} returns <code>true
     *     </code>
     */
    @Override
    public String getTypeOf() {
        return avoidObjectDetection() ? "undefined" : "function";
    }

    /**
     * Implements the instanceof operator for JavaScript Function objects.
     *
     * <p><code>
     * foo = new Foo();<br>
     * foo instanceof Foo;  // true<br>
     * </code>
     *
     * @param instance The value that appeared on the LHS of the instanceof operator
     * @return true if the "prototype" property of "this" appears in value's prototype chain
     */
    @Override
    public boolean hasInstance(Scriptable instance) {
        Object protoProp = ScriptableObject.getProperty(this, "prototype");
        if (protoProp instanceof Scriptable) {
            return ScriptRuntime.jsDelegatesTo(instance, (Scriptable) protoProp);
        }
        throw ScriptRuntime.typeErrorById("msg.instanceof.bad.prototype", getFunctionName());
    }

    protected static final int Id_length = 1,
            Id_arity = 2,
            Id_name = 3,
            Id_prototype = 4,
            Id_arguments = 5,
            MAX_INSTANCE_ID = 5;

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        switch (s) {
            case "length":
                if (lengthPropertyAttributes >= 0) {
                    return instanceIdInfo(lengthPropertyAttributes, Id_length);
                }
                break;
            case "arity":
                if (arityPropertyAttributes >= 0) {
                    return instanceIdInfo(arityPropertyAttributes, Id_arity);
                }
                break;
            case "name":
                if (namePropertyAttributes >= 0) {
                    return instanceIdInfo(namePropertyAttributes, Id_name);
                }
                break;
            case "prototype":
                if (hasPrototypeProperty()) {
                    return instanceIdInfo(prototypePropertyAttributes, Id_prototype);
                }
                break;
            case "arguments":
                return instanceIdInfo(argumentsAttributes, Id_arguments);
            default:
                break;
        }

        return super.findInstanceIdInfo(s);
    }

    @Override
    protected String getInstanceIdName(int id) {
        switch (id) {
            case SymbolId_hasInstance:
                return "SymbolId_hasInstance";
            case Id_length:
                return "length";
            case Id_arity:
                return "arity";
            case Id_name:
                return "name";
            case Id_prototype:
                return "prototype";
            case Id_arguments:
                return "arguments";
        }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        switch (id) {
            case Id_length:
                return lengthPropertyAttributes >= 0 ? getLength() : NOT_FOUND;
            case Id_arity:
                return arityPropertyAttributes >= 0 ? getArity() : NOT_FOUND;
            case Id_name:
                return namePropertyAttributes >= 0
                        ? (nameValue != null ? nameValue : getFunctionName())
                        : NOT_FOUND;
            case Id_prototype:
                return getPrototypeProperty();
            case Id_arguments:
                return getArguments();
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value) {
        switch (id) {
            case Id_prototype:
                if ((prototypePropertyAttributes & READONLY) == 0) {
                    prototypeProperty = (value != null) ? value : UniqueTag.NULL_VALUE;
                }
                return;
            case Id_arguments:
                if (value == NOT_FOUND) {
                    // This should not be called since "arguments" is PERMANENT
                    Kit.codeBug();
                }
                if (defaultHas("arguments")) {
                    defaultPut("arguments", value);
                } else if ((argumentsAttributes & READONLY) == 0) {
                    argumentsObj = value;
                }
                return;
            case Id_name:
                if (value == NOT_FOUND) {
                    namePropertyAttributes = -1;
                    nameValue = null;
                } else if (value instanceof CharSequence) {
                    nameValue = ScriptRuntime.toString(value);
                } else {
                    nameValue = "";
                }
                return;
            case Id_arity:
                if (value == NOT_FOUND) {
                    arityPropertyAttributes = -1;
                }
                return;
            case Id_length:
                if (value == NOT_FOUND) {
                    lengthPropertyAttributes = -1;
                }
                return;
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr) {
        switch (id) {
            case Id_prototype:
                prototypePropertyAttributes = attr;
                return;
            case Id_arguments:
                argumentsAttributes = attr;
                return;
            case Id_arity:
                arityPropertyAttributes = attr;
                return;
            case Id_name:
                namePropertyAttributes = attr;
                return;
            case Id_length:
                lengthPropertyAttributes = attr;
                return;
        }
        super.setInstanceIdAttributes(id, attr);
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        // Fix up bootstrapping problem: getPrototype of the IdFunctionObject
        // can not return Function.prototype because Function object is not
        // yet defined.
        ctor.setPrototype(this);
        super.fillConstructorProperties(ctor);
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_hasInstance) {
            initPrototypeMethod(
                    FUNCTION_TAG,
                    id,
                    SymbolKey.HAS_INSTANCE,
                    String.valueOf(SymbolKey.HAS_INSTANCE),
                    1,
                    CONST | DONTENUM);
            return;
        }

        String s;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 1;
                s = "constructor";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            case Id_toSource:
                arity = 1;
                s = "toSource";
                break;
            case Id_apply:
                arity = 2;
                s = "apply";
                break;
            case Id_call:
                arity = 1;
                s = "call";
                break;
            case Id_bind:
                arity = 1;
                s = "bind";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(FUNCTION_TAG, id, s, arity);
    }

    static boolean isApply(IdFunctionObject f) {
        return f.hasTag(FUNCTION_TAG) && f.methodId() == Id_apply;
    }

    static boolean isApplyOrCall(IdFunctionObject f) {
        if (f.hasTag(FUNCTION_TAG)) {
            switch (f.methodId()) {
                case Id_apply:
                case Id_call:
                    return true;
            }
        }
        return false;
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(FUNCTION_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                if (cx.isStrictMode()) {
                    // Disable strict mode forcefully, and restore it after the call
                    NativeCall activation = cx.currentActivationCall;
                    boolean strictMode = cx.isTopLevelStrict;
                    try {
                        cx.currentActivationCall = null;
                        cx.isTopLevelStrict = false;
                        return jsConstructor(cx, scope, args);
                    } finally {
                        cx.isTopLevelStrict = strictMode;
                        cx.currentActivationCall = activation;
                    }
                } else {
                    return jsConstructor(cx, scope, args);
                }

            case Id_toString:
                {
                    BaseFunction realf = realFunction(thisObj, f);
                    int indent = ScriptRuntime.toInt32(args, 0);
                    return realf.decompile(indent, EnumSet.noneOf(DecompilerFlag.class));
                }

            case Id_toSource:
                {
                    BaseFunction realf = realFunction(thisObj, f);
                    int indent = 0;
                    EnumSet<DecompilerFlag> flags = EnumSet.of(DecompilerFlag.TO_SOURCE);
                    if (args.length != 0) {
                        indent = ScriptRuntime.toInt32(args[0]);
                        if (indent >= 0) {
                            flags = EnumSet.noneOf(DecompilerFlag.class);
                        } else {
                            indent = 0;
                        }
                    }
                    return realf.decompile(indent, flags);
                }

            case Id_apply:
            case Id_call:
                return ScriptRuntime.applyOrCall(id == Id_apply, cx, scope, thisObj, args);

            case Id_bind:
                if (!(thisObj instanceof Callable)) {
                    throw ScriptRuntime.notFunctionError(thisObj);
                }
                Callable targetFunction = (Callable) thisObj;
                int argc = args.length;
                final Scriptable boundThis;
                final Object[] boundArgs;
                if (argc > 0) {
                    boundThis = ScriptRuntime.toObjectOrNull(cx, args[0], scope);
                    boundArgs = new Object[argc - 1];
                    System.arraycopy(args, 1, boundArgs, 0, argc - 1);
                } else {
                    boundThis = null;
                    boundArgs = ScriptRuntime.emptyArgs;
                }
                return new BoundFunction(cx, scope, targetFunction, boundThis, boundArgs);

            case SymbolId_hasInstance:
                if (thisObj != null && args.length == 1 && args[0] instanceof Scriptable) {
                    Scriptable obj = (Scriptable) args[0];
                    Object protoProp = null;
                    if (thisObj instanceof BoundFunction)
                        protoProp =
                                ((NativeFunction) ((BoundFunction) thisObj).getTargetFunction())
                                        .getPrototypeProperty();
                    else protoProp = ScriptableObject.getProperty(thisObj, "prototype");
                    if (protoProp instanceof NativeObject
                            || protoProp instanceof IdScriptableObject) {
                        return ScriptRuntime.jsDelegatesTo(obj, (Scriptable) protoProp);
                    }
                    throw ScriptRuntime.typeErrorById(
                            "msg.instanceof.bad.prototype", getFunctionName());
                }
                return false; // NOT_FOUND, null etc.
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private static BaseFunction realFunction(Scriptable thisObj, IdFunctionObject f) {
        if (thisObj == null) {
            throw ScriptRuntime.notFunctionError(null);
        }
        Object x = thisObj.getDefaultValue(ScriptRuntime.FunctionClass);
        if (x instanceof Delegator) {
            x = ((Delegator) x).getDelegee();
        }
        return ensureType(x, BaseFunction.class, f);
    }

    /** Make value as DontEnum, DontDelete, ReadOnly prototype property of this Function object */
    public void setImmunePrototypeProperty(Object value) {
        if ((prototypePropertyAttributes & READONLY) != 0) {
            throw new IllegalStateException();
        }
        prototypeProperty = (value != null) ? value : UniqueTag.NULL_VALUE;
        prototypePropertyAttributes = DONTENUM | PERMANENT | READONLY;
    }

    protected Scriptable getClassPrototype() {
        Object protoVal = getPrototypeProperty();
        if (protoVal instanceof Scriptable) {
            return (Scriptable) protoVal;
        }
        return ScriptableObject.getObjectPrototype(this);
    }

    /** Should be overridden. */
    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return Undefined.instance;
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && this.getHomeObject() != null) {
            // Only methods have home objects associated with them
            throw ScriptRuntime.typeErrorById("msg.not.ctor", getFunctionName());
        }

        Scriptable result = createObject(cx, scope);
        if (result != null) {
            Object val = call(cx, scope, result, args);
            if (val instanceof Scriptable) {
                result = (Scriptable) val;
            }
        } else {
            Object val = call(cx, scope, null, args);
            if (!(val instanceof Scriptable)) {
                // It is program error not to return Scriptable from
                // the call method if createObject returns null.
                throw new IllegalStateException(
                        "Bad implementation of call as constructor, name="
                                + getFunctionName()
                                + " in "
                                + getClass().getName());
            }
            result = (Scriptable) val;
            if (result.getPrototype() == null) {
                Scriptable proto = getClassPrototype();
                if (result != proto) {
                    result.setPrototype(proto);
                }
            }
            if (result.getParentScope() == null) {
                Scriptable parent = getParentScope();
                if (result != parent) {
                    result.setParentScope(parent);
                }
            }
        }
        return result;
    }

    /**
     * Creates new script object. The default implementation of {@link #construct} uses this method
     * to to get the value for <code>thisObj</code> argument when invoking {@link #call}. The method
     * is allowed to return <code>null</code> to indicate that {@link #call} will create a new
     * object itself. In this case {@link #construct} will set scope and prototype on the result
     * {@link #call} unless they are already set.
     */
    public Scriptable createObject(Context cx, Scriptable scope) {
        Scriptable newInstance = new NativeObject();
        newInstance.setPrototype(getClassPrototype());
        newInstance.setParentScope(getParentScope());
        return newInstance;
    }

    /**
     * Decompile the source information associated with this js function/script back into a string.
     *
     * @param indent How much to indent the decompiled result.
     * @param flags Flags specifying format of decompilation output.
     */
    String decompile(int indent, EnumSet<DecompilerFlag> flags) {
        StringBuilder sb = new StringBuilder();
        boolean justbody = flags.contains(DecompilerFlag.ONLY_BODY);
        if (!justbody) {
            sb.append("function ");
            sb.append(getFunctionName());
            sb.append("() {\n\t");
        }
        sb.append("[native code]\n");
        if (!justbody) {
            sb.append("}\n");
        }
        return sb.toString();
    }

    public int getArity() {
        return 0;
    }

    public int getLength() {
        return 0;
    }

    public String getFunctionName() {
        return "";
    }

    /**
     * Sets the attributes of the "name", "length", and "arity" properties, which differ for many
     * native objects.
     */
    public void setStandardPropertyAttributes(int attributes) {
        namePropertyAttributes = attributes;
        lengthPropertyAttributes = attributes;
        arityPropertyAttributes = attributes;
    }

    public void setPrototypePropertyAttributes(int attributes) {
        prototypePropertyAttributes = attributes;
    }

    protected boolean hasPrototypeProperty() {
        return prototypeProperty != null || this instanceof NativeFunction;
    }

    public Object getPrototypeProperty() {
        Object result = prototypeProperty;
        if (result == null) {
            // only create default prototype on native JavaScript functions,
            // not on built-in functions, java methods, host objects etc.
            if (this instanceof NativeFunction) {
                result = setupDefaultPrototype();
            } else {
                result = Undefined.instance;
            }
        } else if (result == UniqueTag.NULL_VALUE) {
            result = null;
        }
        return result;
    }

    protected void setPrototypeProperty(Object prototype) {
        this.prototypeProperty = prototype;
    }

    protected synchronized Object setupDefaultPrototype() {
        if (prototypeProperty != null) {
            return prototypeProperty;
        }
        NativeObject obj = new NativeObject();
        obj.setParentScope(getParentScope());

        // put the prototype property into the object now, then in the
        // wacky case of a user defining a function Object(), we don't
        // get an infinite loop trying to find the prototype.
        prototypeProperty = obj;
        Scriptable proto = getObjectPrototype(this);
        if (proto != obj) {
            // not the one we just made, it must remain grounded
            obj.setPrototype(proto);
        }

        obj.defineProperty("constructor", this, DONTENUM);
        return obj;
    }

    private Object getArguments() {
        // <Function name>.arguments is deprecated, so we use a slow
        // way of getting it that doesn't add to the invocation cost.
        // TODO: add warning, error based on version
        Object value = defaultHas("arguments") ? defaultGet("arguments") : argumentsObj;
        if (value != NOT_FOUND) {
            // Should after changing <Function name>.arguments its
            // activation still be available during Function call?
            // This code assumes it should not:
            // defaultGet("arguments") != NOT_FOUND
            // means assigned arguments
            return value;
        }
        Context cx = Context.getContext();
        NativeCall activation = ScriptRuntime.findFunctionActivation(cx, this);
        return (activation == null) ? null : activation.get("arguments", activation);
    }

    private Object jsConstructor(Context cx, Scriptable scope, Object[] args) {
        int arglen = args.length;
        StringBuilder sourceBuf = new StringBuilder();

        sourceBuf.append("function ");
        if (isGeneratorFunction()) {
            sourceBuf.append("* ");
        }
        /* version != 1.2 Function constructor behavior -
         * print 'anonymous' as the function name if the
         * version (under which the function was compiled) is
         * less than 1.2... or if it's greater than 1.2, because
         * we need to be closer to ECMA.
         */
        if (cx.getLanguageVersion() != Context.VERSION_1_2) {
            sourceBuf.append("anonymous");
        }
        sourceBuf.append('(');

        // Append arguments as coma separated strings
        for (int i = 0; i < arglen - 1; i++) {
            if (i > 0) {
                sourceBuf.append(',');
            }
            sourceBuf.append(ScriptRuntime.toString(args[i]));
        }
        sourceBuf.append(") {");
        if (arglen != 0) {
            // append function body
            String funBody = ScriptRuntime.toString(args[arglen - 1]);
            sourceBuf.append(funBody);
        }
        sourceBuf.append("\n}");
        String source = sourceBuf.toString();

        int[] linep = new int[1];
        String filename = Context.getSourcePositionFromStack(linep);
        if (filename == null) {
            filename = "<eval'ed string>";
            linep[0] = 1;
        }

        String sourceURI = ScriptRuntime.makeUrlForGeneratedScript(false, filename, linep[0]);

        Scriptable global = ScriptableObject.getTopLevelScope(scope);

        ErrorReporter reporter;
        reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());

        Evaluator evaluator = Context.createInterpreter();
        if (evaluator == null) {
            throw new JavaScriptException("Interpreter not present", filename, linep[0]);
        }

        // Compile with explicit interpreter instance to force interpreter
        // mode.
        return cx.compileFunction(global, source, evaluator, reporter, sourceURI, 1, null);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.HAS_INSTANCE.equals(k)) return SymbolId_hasInstance;
        return 0;
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "toString":
                id = Id_toString;
                break;
            case "toSource":
                id = Id_toSource;
                break;
            case "apply":
                id = Id_apply;
                break;
            case "call":
                id = Id_call;
                break;
            case "bind":
                id = Id_bind;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    public void setHomeObject(Scriptable homeObject) {
        this.homeObject = homeObject;
    }

    public Scriptable getHomeObject() {
        return homeObject;
    }

    private static final int Id_constructor = 1,
            Id_toString = 2,
            Id_toSource = 3,
            Id_apply = 4,
            Id_call = 5,
            Id_bind = 6,
            SymbolId_hasInstance = 7,
            MAX_PROTOTYPE_ID = SymbolId_hasInstance;

    private Object prototypeProperty;
    private Object argumentsObj = NOT_FOUND;
    private String nameValue = null;
    private boolean isGeneratorFunction = false;
    private Scriptable homeObject = null;

    // For function object instances, attributes are
    //  {configurable:false, enumerable:false};
    // see ECMA 15.3.5.2
    private int prototypePropertyAttributes = PERMANENT | DONTENUM;
    private int argumentsAttributes = PERMANENT | DONTENUM;
    private int arityPropertyAttributes = PERMANENT | READONLY | DONTENUM;
    private int namePropertyAttributes = READONLY | DONTENUM;
    private int lengthPropertyAttributes = PERMANENT | READONLY | DONTENUM;
}
