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
public class BaseFunction extends ScriptableObject implements Function {
    private static final long serialVersionUID = 5311394446546053859L;

    private static final Object FUNCTION_TAG = "Function";
    private static final String FUNCTION_CLASS = "Function";
    static final String GENERATOR_FUNCTION_CLASS = "__GeneratorFunction";

    private static final String APPLY_TAG = "APPLY_TAG";
    private static final String CALL_TAG = "CALL_TAG";
    private static final String PROTOTYPE_PROPERTY_NAME = "prototype";

    static LambdaConstructor init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor ctor =
                new LambdaConstructor(
                        scope,
                        FUNCTION_CLASS,
                        1,
                        BaseFunction::js_constructorCall,
                        BaseFunction::js_constructor);

        var proto =
                new LambdaFunction(
                        scope, "", 0, null, (lcx, lscope, lthisObj, largs) -> Undefined.instance);

        proto.defineProperty("constructor", ctor, DONTENUM);
        // Set the constructor correctly here. i.e. ctor.prototype.constructor == ctor
        // Redo the stuff about setupDefaultPrototype.

        ctor.setPrototypeProperty(proto);
        // Do this early, so that the functions on the prototype get
        // the right prototype...
        ScriptableObject.defineProperty(scope, FUNCTION_CLASS, ctor, DONTENUM);
        ctor.setPrototype((Scriptable) ctor.getPrototypeProperty());

        defKnownBuiltInOnProto(ctor, APPLY_TAG, scope, "apply", 2, BaseFunction::js_apply);
        defOnProto(ctor, scope, "bind", 1, BaseFunction::js_bind);
        defKnownBuiltInOnProto(ctor, CALL_TAG, scope, "call", 1, BaseFunction::js_call);
        defOnProto(ctor, scope, "toSource", 1, BaseFunction::js_toSource);
        defOnProto(ctor, scope, "toString", 0, BaseFunction::js_toString);
        defOnProto(
                ctor,
                scope,
                SymbolKey.HAS_INSTANCE,
                1,
                BaseFunction::js_hasInstance,
                DONTENUM | READONLY | PERMANENT);

        // Function.prototype attributes: see ECMA 15.3.3.1
        ctor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            ctor.setStandardPropertyAttributes(READONLY | DONTENUM);
        }

        if (!cx.isStrictMode() && cx.getLanguageVersion() >= Context.VERSION_ES6) {
            ctor.definePrototypeProperty(
                    cx,
                    "arguments",
                    BaseFunction::js_protoArgumentsGetter,
                    BaseFunction::js_protoArgumentsSetter,
                    DONTENUM | READONLY);
        }

        ScriptableObject.defineProperty(scope, FUNCTION_CLASS, ctor, DONTENUM);
        if (sealed) {
            ctor.sealObject();
            ((ScriptableObject) ctor.getPrototypeProperty()).sealObject();
        }
        return ctor;
    }

    private static void defOnProto(
            LambdaConstructor constructor,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        constructor.definePrototypeMethod(scope, name, length, target);
    }

    private static void defKnownBuiltInOnProto(
            LambdaConstructor constructor,
            Object tag,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target) {
        constructor.defineKnownBuiltInPrototypeMethod(
                tag, scope, name, length, null, target, DONTENUM, DONTENUM | READONLY);
    }

    private static void defOnProto(
            LambdaConstructor constructor,
            Scriptable scope,
            SymbolKey name,
            int length,
            SerializableCallable target,
            int attributes) {
        constructor.definePrototypeMethod(
                scope, name, length, null, target, attributes, DONTENUM | READONLY);
    }

    /**
     * @deprecated Use {@link #init(Context, Scriptable, boolean)} instead
     */
    @Deprecated
    static void init(Scriptable scope, boolean sealed) {
        init(Context.getContext(), scope, sealed);
    }

    static Object initAsGeneratorFunction(Scriptable scope, boolean sealed) {
        var proto = new NativeObject();
        Scriptable top = ScriptableObject.getTopLevelScope(scope);

        var function = (Scriptable) ScriptableObject.getProperty(scope, FUNCTION_CLASS);
        var functionProto =
                (Scriptable) ScriptableObject.getProperty(function, PROTOTYPE_PROPERTY_NAME);
        proto.setPrototype(functionProto);

        var iterator = (Scriptable) ScriptableObject.getProperty(scope, "Iterator");
        ScriptableObject.putProperty(
                proto,
                PROTOTYPE_PROPERTY_NAME,
                ScriptableObject.getTopScopeValue(top, ES6Generator.GENERATOR_TAG));

        LambdaConstructor ctor =
                new LambdaConstructor(
                        scope,
                        GENERATOR_FUNCTION_CLASS,
                        1,
                        proto,
                        BaseFunction::js_gen_constructorCall,
                        BaseFunction::js_gen_constructor);

        proto.defineProperty("constructor", ctor, READONLY | DONTENUM);

        // Function.prototype attributes: see ECMA 15.3.3.1
        ctor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        proto.defineProperty(SymbolKey.TO_STRING_TAG, "GeneratorFunction", READONLY | DONTENUM);
        ScriptableObject.putProperty(scope, GENERATOR_FUNCTION_CLASS, ctor);
        // Function.prototype attributes: see ECMA 15.3.3.1
        // The "GeneratorFunction" name actually never appears in the global scope.
        // Return it here so it can be cached as a "builtin"
        return ctor;
    }

    public BaseFunction() {
        createProperties();
    }

    public BaseFunction(boolean isGenerator) {
        createProperties();
        this.isGeneratorFunction = isGenerator;
    }

    public BaseFunction(Scriptable scope, Scriptable prototype) {
        super(scope, prototype);
        createProperties();
        ScriptRuntime.setBuiltinProtoAndParent(this, scope, TopLevel.Builtins.Function);
    }

    protected void createProperties() {
        ScriptableObject.defineBuiltInProperty(
                this, "length", DONTENUM | READONLY, BaseFunction::lengthGetter);
        ScriptableObject.defineBuiltInProperty(
                this,
                "name",
                DONTENUM | READONLY,
                BaseFunction::nameGetter,
                BaseFunction::nameSetter);

        Context cx = Context.getCurrentContext();
        if (cx == null || !cx.isStrictMode()) {
            ScriptableObject.defineBuiltInProperty(
                    this, "arity", PERMANENT | DONTENUM | READONLY, BaseFunction::arityGetter);

            if (cx == null || cx.getLanguageVersion() < Context.VERSION_ES6) {
                ScriptableObject.defineBuiltInProperty(
                        this,
                        "arguments",
                        PERMANENT | DONTENUM,
                        BaseFunction::argumentsGetter,
                        BaseFunction::argumentsSetter);
            }
        }
    }

    private static Object lengthGetter(BaseFunction function, Scriptable start) {
        return function.getLength();
    }

    private static Object arityGetter(BaseFunction function, Scriptable start) {
        return function.getArity();
    }

    private static Object argumentsGetter(BaseFunction function, Scriptable start) {
        return function.getArguments();
    }

    private static boolean argumentsSetter(
            BaseFunction function,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        function.argumentsObj = value;
        return true;
    }

    private static Object nameGetter(BaseFunction function, Scriptable start) {
        return function.nameValue != null ? function.nameValue : function.getFunctionName();
    }

    private static boolean nameSetter(
            BaseFunction function,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        function.nameValue = value;
        return true;
    }

    /** Forces setting the function's name, bypassing all "readonly" checks. */
    void setFunctionName(String name) {
        nameValue = name;
    }

    protected void createPrototypeProperty() {
        try (var map = startCompoundOp(true)) {
            createPrototypeProperty(map);
        }
    }

    protected void createPrototypeProperty(CompoundOperationMap compoundOp) {
        compoundOp.compute(
                this,
                compoundOp,
                PROTOTYPE_PROPERTY_NAME,
                0,
                (k, i, s, m, o) -> {
                    if (s == null) {
                        return new BuiltInSlot<BaseFunction>(
                                PROTOTYPE_PROPERTY_NAME,
                                0,
                                prototypePropertyAttributes,
                                this,
                                BaseFunction::prototypeGetter,
                                BaseFunction::prototypeSetter,
                                BaseFunction::prototypeAttrSetter,
                                BaseFunction::prototypeDescSetter);
                    }
                    return s;
                });
    }

    private static Object prototypeGetter(BaseFunction function, Scriptable start) {
        return function.getPrototypeProperty();
    }

    private static boolean prototypeSetter(
            BaseFunction function,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        function.prototypeProperty = value == null ? UniqueTag.NULL_VALUE : value;
        return true;
    }

    private static void prototypeAttrSetter(BaseFunction function, int attributes) {
        function.prototypePropertyAttributes = attributes;
    }

    protected static boolean prototypeDescSetter(
            BaseFunction builtIn,
            BuiltInSlot<BaseFunction> current,
            Object id,
            ScriptableObject.DescriptorInfo info,
            boolean checkValid,
            Object key,
            int index) {
        try (var map = builtIn.startCompoundOp(true)) {
            return ScriptableObject.defineOrdinaryProperty(
                    (o, i, k, e, m, s) -> {
                        if (i.value != NOT_FOUND) {
                            builtIn.prototypeProperty =
                                    i.value == null ? UniqueTag.NULL_VALUE : i.value;
                        }
                        return s;
                    },
                    builtIn,
                    map,
                    id,
                    info,
                    checkValid,
                    key,
                    index);
        }
    }

    protected final boolean defaultHas(String name) {
        return super.has(name, this);
    }

    protected final Object defaultGet(String name) {
        return super.get(name, this);
    }

    protected final void defaultPut(String name, Object value) {
        super.put(name, this, value);
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
     * @return "function" or "undefined" if {@link #avoidObjectDetection()} returns {@code true}
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
        Context cx = Context.getCurrentContext();

        // Attempt to call custom Symbol.hasInstance implementation if present
        Object hasInstanceMethod = ScriptRuntime.getObjectElem(this, SymbolKey.HAS_INSTANCE, cx);
        if (hasInstanceMethod instanceof Callable) {
            return ScriptRuntime.toBoolean(
                    ((Callable) hasInstanceMethod)
                            .call(cx, getParentScope(), this, new Object[] {instance}));
        }

        Object protoProp = ScriptableObject.getProperty(this, PROTOTYPE_PROPERTY_NAME);
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

    static boolean isApply(KnownBuiltInFunction f) {
        return f.getTag() == APPLY_TAG;
    }

    static boolean isApplyOrCall(KnownBuiltInFunction f) {
        var tag = f.getTag();
        return tag == APPLY_TAG || tag == CALL_TAG;
    }

    private static Object js_hasInstance(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!(thisObj instanceof Callable)) {
            return false;
        }
        Object protoProp = null;
        if (thisObj instanceof BoundFunction)
            protoProp =
                    ((JSFunction) ((BoundFunction) thisObj).getTargetFunction())
                            .getPrototypeProperty();
        else {
            protoProp = ScriptableObject.getProperty(thisObj, PROTOTYPE_PROPERTY_NAME);
        }

        if (ScriptRuntime.isObject(protoProp)) {
            if (args.length > 0 && args[0] instanceof Scriptable) {
                Scriptable obj = (Scriptable) args[0];

                return ScriptRuntime.jsDelegatesTo(obj, (Scriptable) protoProp);
            }
            return false; // NOT_FOUND, null etc.
        }

        throw ScriptRuntime.typeErrorById(
                "msg.instanceof.bad.prototype",
                (thisObj instanceof BaseFunction)
                        ? ((BaseFunction) thisObj).getFunctionName()
                        : "unknown");
    }

    private static Object js_bind(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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
    }

    private static Object js_apply(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ScriptRuntime.applyOrCall(true, cx, scope, thisObj, args);
    }

    private static Object js_call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ScriptRuntime.applyOrCall(false, cx, scope, thisObj, args);
    }

    private static Object js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        BaseFunction realf = realFunction(thisObj, "toSource");
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

    private static Object js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        BaseFunction realf = realFunction(thisObj, "toString");
        int indent = ScriptRuntime.toInt32(args, 0);
        return realf.decompile(indent, EnumSet.noneOf(DecompilerFlag.class));
    }

    private static Scriptable js_gen_constructorCall(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return js_gen_constructor(cx, scope, args);
    }

    private static Scriptable js_constructor(Context cx, Scriptable scope, Object[] args) {
        if (cx.isStrictMode()) {
            // Disable strict mode forcefully, and restore it after the call
            NativeCall activation = cx.currentActivationCall;
            boolean strictMode = cx.isTopLevelStrict;
            try {
                cx.currentActivationCall = null;
                cx.isTopLevelStrict = false;
                return jsConstructor(cx, scope, args, false);
            } finally {
                cx.isTopLevelStrict = strictMode;
                cx.currentActivationCall = activation;
            }
        } else {
            return jsConstructor(cx, scope, args, false);
        }
    }

    private static Scriptable js_constructorCall(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return js_constructor(cx, scope, args);
    }

    private static Scriptable js_gen_constructor(Context cx, Scriptable scope, Object[] args) {
        if (cx.isStrictMode()) {
            // Disable strict mode forcefully, and restore it after the call
            NativeCall activation = cx.currentActivationCall;
            boolean strictMode = cx.isTopLevelStrict;
            try {
                cx.currentActivationCall = null;
                cx.isTopLevelStrict = false;
                return jsConstructor(cx, scope, args, true);
            } finally {
                cx.isTopLevelStrict = strictMode;
                cx.currentActivationCall = activation;
            }
        } else {
            return jsConstructor(cx, scope, args, true);
        }
    }

    private static BaseFunction realFunction(Scriptable thisObj, String functionName) {
        if (thisObj == null) {
            throw ScriptRuntime.notFunctionError(null);
        }
        Object x = thisObj.getDefaultValue(ScriptRuntime.FunctionClass);
        if (x instanceof Delegator) {
            x = ((Delegator) x).getDelegee();
        }
        return ensureType(x, BaseFunction.class, functionName);
    }

    /** Make value as DontEnum, DontDelete, ReadOnly prototype property of this Function object */
    public void setImmunePrototypeProperty(Object value) {
        if ((prototypePropertyAttributes & READONLY) != 0) {
            throw new IllegalStateException();
        }
        prototypeProperty = (value != null) ? value : UniqueTag.NULL_VALUE;
        createPrototypeProperty();
        setAttributes(PROTOTYPE_PROPERTY_NAME, DONTENUM | PERMANENT | READONLY);
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
        if (result == null) {
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
        } else {
            Object val = call(cx, scope, result, args);
            if (val instanceof Scriptable) {
                result = (Scriptable) val;
            }
        }
        return result;
    }

    /**
     * Creates new script object. The default implementation of {@link #construct} uses this method
     * to to get the value for {@code thisObj} argument when invoking {@link #call}. The method is
     * allowed to return {@code null} to indicate that {@link #call} will create a new object
     * itself. In this case {@link #construct} will set scope and prototype on the result {@link
     * #call} unless they are already set.
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

    private static Object js_protoArgumentsGetter(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, BaseFunction.class).getArguments();
    }

    private static void js_protoArgumentsSetter(Scriptable thisObj, Object value) {
        LambdaConstructor.convertThisObject(thisObj, BaseFunction.class).setArguments(value);
    }

    public String getFunctionName() {
        return "";
    }

    /**
     * Sets the attributes of the "name", "length", and "arity" properties, which differ for many
     * native objects.
     */
    public void setStandardPropertyAttributes(int attributes) {
        setAttributes("name", attributes);
        setAttributes("length", attributes);
        setAttributes("arity", attributes);
    }

    public void setPrototypePropertyAttributes(int attributes) {
        prototypePropertyAttributes = attributes;
        getMap().compute(
                        this,
                        PROTOTYPE_PROPERTY_NAME,
                        0,
                        (k, i, s, m, o) -> {
                            if (s != null) {
                                s.setAttributes(attributes);
                            }
                            return s;
                        });
    }

    protected boolean hasPrototypeProperty() {
        return prototypeProperty != null && prototypeProperty != UniqueTag.NOT_FOUND;
    }

    public Object getPrototypeProperty() {
        Object result = prototypeProperty;
        if (result == null || result == UniqueTag.NOT_FOUND) {
            result = Undefined.instance;
        } else if (result == UniqueTag.NULL_VALUE) {
            result = null;
        }
        return result;
    }

    protected void setPrototypeProperty(Object prototype) {
        if (prototype != null) {
            createPrototypeProperty();
            this.prototypeProperty = prototype;
        } else {
            prototypeProperty = UniqueTag.NOT_FOUND;
        }
    }

    protected synchronized Object setupDefaultPrototype(Scriptable scope) {
        if (!has(PROTOTYPE_PROPERTY_NAME, this)) {
            createPrototypeProperty();
        }
        NativeObject obj = new NativeObject();
        obj.setParentScope(getParentScope());

        // put the prototype property into the object now, then in the
        // wacky case of a user defining a function Object(), we don't
        // get an infinite loop trying to find the prototype.
        prototypeProperty = obj;
        Scriptable proto;
        if (isGeneratorFunction()) {
            // For generator functions, the .prototype property's [[Prototype]]
            // should be %GeneratorPrototype%, not Object.prototype
            Scriptable top = ScriptableObject.getTopLevelScope(scope);
            Object generatorProto =
                    ScriptableObject.getTopScopeValue(top, ES6Generator.GENERATOR_TAG);
            if (generatorProto instanceof Scriptable) {
                proto = (Scriptable) generatorProto;
            } else {
                proto = getObjectPrototype(this); // fallback
            }
        } else {
            proto = getObjectPrototype(this);
        }
        if (proto != obj) {
            // not the one we just made, it must remain grounded
            obj.setPrototype(proto);
        }

        obj.defineProperty("constructor", this, DONTENUM);
        return obj;
    }

    Object getArguments() {
        // <Function name>.arguments is deprecated, so we use a slow
        // way of getting it that doesn't add to the invocation cost.
        // TODO: add warning, error based on version
        if (argumentsObj != NOT_FOUND) {
            // Should after changing <Function name>.arguments its
            // activation still be available during Function call?
            // This code assumes it should not:
            // defaultGet("arguments") != NOT_FOUND
            // means assigned arguments
            return argumentsObj;
        }
        Context cx = Context.getContext();
        NativeCall activation = ScriptRuntime.findFunctionActivation(cx, this);
        // return (activation == null) ? null : activation.get("arguments", activation);
        if (activation == null) {
            return null;
        }
        if (activation.isStrict && cx.getLanguageVersion() >= Context.VERSION_ES6) {
            ScriptRuntime.ThrowTypeError.throwNotAllowed();
        }
        Object arguments = activation.get("arguments", activation);
        if (arguments instanceof Arguments && cx.getLanguageVersion() >= Context.VERSION_ES6) {
            return new Arguments.ReadonlyArguments((Arguments) arguments, cx);
        }
        return arguments;
    }

    void setArguments(Object caller) {}

    private static Scriptable jsConstructor(
            Context cx, Scriptable scope, Object[] args, boolean isGeneratorFunction) {
        int arglen = args.length;
        StringBuilder sourceBuf = new StringBuilder();

        sourceBuf.append("function ");
        if (isGeneratorFunction) {
            sourceBuf.append("* ");
        }
        sourceBuf.append("anonymous(");

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

    public void setHomeObject(Scriptable homeObject) {
        this.homeObject = homeObject;
    }

    public Scriptable getHomeObject() {
        return homeObject;
    }

    @Override
    public boolean isConstructor() {
        return !(Context.getCurrentContext().getLanguageVersion() >= Context.VERSION_ES6
                && this.getHomeObject() != null);
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
    private Object nameValue = null;
    private boolean isGeneratorFunction = false;
    private Scriptable homeObject = null;

    // For function object instances, attributes are
    //  {configurable:false, enumerable:false};
    // see ECMA 15.3.5.2
    private int prototypePropertyAttributes = PERMANENT | DONTENUM;
}
