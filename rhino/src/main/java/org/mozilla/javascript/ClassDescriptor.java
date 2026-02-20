package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import org.mozilla.javascript.ScriptableObject.DescriptorInfo;
import org.mozilla.javascript.ScriptableObject.LambdaGetterFunction;
import org.mozilla.javascript.ScriptableObject.LambdaSetterFunction;

/**
 * A descriptor for a constructor and its prototype methods which can then be used to build a real
 * constructor function and prototype during standard object initialisation. This should be built by
 * creating a {@link Builder} and using the methods there to declare the required methods. This
 * should normally be done once during static initialisation of a class and then utilised when
 * initialising within a scope.
 *
 * <p>For example:
 *
 * <pre>
 * final class NativeBoolean extends ScriptableObject {
 *   private static final String CLASS_NAME = "Boolean";
 *
 *   private static final ClassDescriptor DESCRIPTOR;
 *
 *   static {
 *       DESCRIPTOR =
 *               new ClassDescriptor.Builder(
 *                               CLASS_NAME,
 *                               1,
 *                               NativeBoolean::js_constructorFunc,
 *                               NativeBoolean::js_constructor)
 *                       .withMethod(PROTO, "toString", 0, NativeBoolean::js_toString)
 *                       .withMethod(PROTO, "toSource", 0, NativeBoolean::js_toSource)
 *                       .withMethod(PROTO, "valueOf", 0, NativeBoolean::js_valueOf)
 *                       .build();
 *
 *   static void init(Scriptable scope, boolean sealed) {
 *       // Boolean is an unusual object in that the prototype is itself a Boolean
 *       var constructor = DESCRIPTOR.buildConstructor(scope, new NativeBoolean(false), sealed);
 *   }
 * </pre>
 *
 * Properties can be defined using the {@link Builder#withProp(Destination, String,
 * LambdaGetterFunction, LambdaSetterFunction, int)} and the others with similar names. These either
 * take lambda getter and setter functions which will be converted into function objects at the
 * point the context is initialised, or take {@link ValueCreator}s which can populate a value
 * descriptor.
 *
 * <p>Convenience methods are provided to create aliases of properties ({@link
 * Builder#aliss(String)}) and simple values which are truly independent of the context ({@link
 * Builder#value(Object)}), along with variants that also accept attribute flags. More complex cases
 * can be handled by methods similar to {@link NativeArray#makeUnscopables(Context, Scriptable,
 * ScriptableObject)}.
 */
public class ClassDescriptor {

    /**
     * Holds the information required to turn a function descriptor into a property on an object or
     * scope.
     */
    private static class FuncPropDesc {
        private final Object name;
        private final JSDescriptor<JSFunction> funcDesc;
        private final int attributes;
        private final int stdAttrs;

        FuncPropDesc(Object name, JSDescriptor<JSFunction> funcDesc, int attributes, int stdAttrs) {
            this.name = name;
            this.funcDesc = funcDesc;
            this.attributes = attributes;
            this.stdAttrs = stdAttrs;
        }
    }

    private abstract static class PropDesc {
        final Object name;
        final int attributes;

        PropDesc(Object name, int attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        abstract void makeProp(Context cx, Scriptable scope, ScriptableObject object);
    }

    private static class LambdaGetSetPropDesc extends PropDesc {
        private final ScriptableObject.LambdaGetterFunction getter;
        private final ScriptableObject.LambdaSetterFunction setter;

        LambdaGetSetPropDesc(
                Object name,
                ScriptableObject.LambdaGetterFunction getter,
                ScriptableObject.LambdaSetterFunction setter,
                int attributes) {
            super(name, attributes);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void makeProp(Context cx, Scriptable scope, ScriptableObject obj) {
            if (name instanceof String) {
                obj.defineProperty(cx, (String) name, getter, setter, attributes);
            } else {
                obj.defineProperty(cx, (SymbolKey) name, getter, setter, attributes);
            }
        }
    }

    public interface ValueCreator {
        DescriptorInfo apply(Context cx, Scriptable Scope, ScriptableObject obj);
    }

    private static class CreateValuePropDesc extends PropDesc {
        private final ValueCreator creator;

        CreateValuePropDesc(Object name, ValueCreator creator) {
            super(name, 0);
            this.creator = creator;
        }

        @Override
        void makeProp(Context cx, Scriptable scope, ScriptableObject obj) {
            if (name instanceof String) {
                obj.defineOwnProperty(cx, name, creator.apply(cx, scope, obj), false);
            } else {
                obj.defineOwnProperty(cx, name, creator.apply(cx, scope, obj), false);
            }
        }
    }

    private final FuncPropDesc ctorDesc;
    private final List<FuncPropDesc> ctorDescs;
    private final List<PropDesc> ctorProps;
    private final List<FuncPropDesc> protoDescs;
    private final List<PropDesc> protoProps;

    private ClassDescriptor(
            FuncPropDesc ctorDesc,
            List<FuncPropDesc> ctorDescs,
            List<PropDesc> ctorProps,
            List<FuncPropDesc> protoDescs,
            List<PropDesc> protoProps) {
        this.ctorDesc = ctorDesc;
        this.ctorDescs = ctorDescs;
        this.ctorProps = ctorProps;
        this.protoDescs = protoDescs;
        this.protoProps = protoProps;
    }

    /** Build a constructor from this descriptor. */
    public JSFunction buildConstructor(
            Context cx, Scriptable scope, ScriptableObject proto, boolean sealed) {
        return buildConstructor(cx, scope, proto, sealed, (c, s) -> {});
    }

    public ScriptableObject populateGlobal(
            Context cx, Scriptable scope, ScriptableObject global, boolean sealed) {
        var objProto = ScriptableObject.getObjectPrototype(scope);
        global.setPrototype(objProto);
        ScriptableObject.defineProperty(
                scope, ctorDesc.name.toString(), global, ctorDesc.attributes);
        for (var e : ctorDescs) {
            var f = new JSFunction(scope, e.funcDesc, null, null);
            f.setStandardPropertyAttributes(e.stdAttrs);
            if (e.name instanceof String) {
                global.put((String) e.name, global, f);
                global.setAttributes((String) e.name, e.attributes);
            } else {
                global.put((SymbolKey) e.name, global, f); // , e.attributes);
                global.setAttributes((SymbolKey) e.name, e.attributes);
            }
        }

        for (var p : ctorProps) {
            p.makeProp(cx, scope, global);
        }
        if (sealed) {
            global.sealObject();
        }
        return global;
    }

    /**
     * Build a constructor from this descriptor. The customStep will be called just before the
     * objects would be sealed.
     */
    public JSFunction buildConstructor(
            Context cx,
            Scriptable scope,
            ScriptableObject proto,
            boolean sealed,
            BiConsumer<Context, JSFunction> customStep) {
        var ctor = new JSFunction(scope, ctorDesc.funcDesc, null, null);

        if (proto != null) {
            ctor.setPrototypeProperty(proto);
            ctor.setPrototypePropertyAttributes(ctorDesc.stdAttrs);
            proto.setParentScope(scope);
            proto.put("constructor", proto, ctor);
        }

        if (ctorDesc.name instanceof String) {
            ScriptableObject.defineProperty(
                    scope, (String) ctorDesc.name, ctor, ctorDesc.attributes);
        } else {
            ScriptableObject.defineProperty(
                    scope, (SymbolKey) ctorDesc.name, ctor, ctorDesc.attributes);
        }

        for (var e : ctorDescs) {
            var f = new JSFunction(scope, e.funcDesc, null, null);
            f.setStandardPropertyAttributes(e.stdAttrs);
            if (e.name instanceof String) {
                ctor.put((String) e.name, ctor, f);
                ctor.setAttributes((String) e.name, e.attributes);
            } else {
                ctor.put((SymbolKey) e.name, ctor, f); // , e.attributes);
                ctor.setAttributes((SymbolKey) e.name, e.attributes);
            }
        }

        for (var p : ctorProps) {
            p.makeProp(cx, scope, ctor);
        }

        if (proto != null) {
            var objProto = ScriptableObject.getObjectPrototype(scope);
            if (proto != objProto) {
                proto.setPrototype(objProto);
            }
            proto.setAttributes("constructor", DONTENUM);
            for (var e : protoDescs) {
                var f = new JSFunction(scope, e.funcDesc, null, null);
                f.setStandardPropertyAttributes(e.stdAttrs);
                if (e.name instanceof String) {
                    proto.put((String) e.name, proto, f);
                    proto.setAttributes((String) e.name, e.attributes);
                } else {
                    proto.put((SymbolKey) e.name, proto, f);
                    proto.setAttributes((SymbolKey) e.name, e.attributes);
                }
            }
            for (var p : protoProps) {
                p.makeProp(cx, scope, proto);
            }
        }

        customStep.accept(cx, ctor);
        if (sealed) {
            ctor.sealObject();
            if (proto != null) {
                proto.sealObject();
            }
        }
        return ctor;
    }

    public JSDescriptor<JSFunction> findProtoDesc(Object name) {
        for (var d : protoDescs) {
            if (name.equals(d.name)) {
                return d.funcDesc;
            }
        }
        return null;
    }

    public JSDescriptor<JSFunction> findCtorDesc(Object name) {
        for (var d : ctorDescs) {
            if (name.equals(d.name)) {
                return d.funcDesc;
            }
        }
        return null;
    }

    public JSDescriptor<JSFunction> ctorDesc() {
        return ctorDesc.funcDesc;
    }

    /**
     * Enum indicating the destination for an operation. CTOR indicates the constructor, and PROTO
     * indicates the proto.
     */
    public enum Destination {
        CTOR {
            @Override
            public Props get(Builder builder) {
                return builder.ctorProps;
            }
        },
        PROTO {
            @Override
            public Props get(Builder builder) {
                return builder.protoProps;
            }
        };

        public abstract Props get(Builder builder);
    }

    private static class Props {
        final List<FuncPropDesc> funcs = new ArrayList<>();
        final List<PropDesc> props = new ArrayList<>();
        final int attrs;

        Props(int attrs) {
            this.attrs = attrs;
        }
    }

    /**
     * A builder for creating class descriptors. Usually created during static initialisation
     * methods can be added before finally calling {@link Builder#build()} to create the final
     * descriptor.
     */
    public static class Builder {
        private final JSDescriptor<JSFunction> ctor;
        private final Props ctorProps;
        private final Props protoProps;
        private final Object name;

        public Builder(String name) {
            this.name = name;
            this.ctor = null;
            this.ctorProps = new Props(DONTENUM);
            this.protoProps = new Props(DONTENUM | READONLY | PERMANENT);
        }

        public Builder(SymbolKey name) {
            this.name = name;
            this.ctor = null;
            this.ctorProps = new Props(DONTENUM);
            this.protoProps = new Props(DONTENUM | READONLY | PERMANENT);
        }

        /**
         * Create a builder for a constructor with the standard attributes for a built in EcmaScript
         * class that can only be called as a constructor.
         *
         * @param name the constructor's name
         * @param length the constructor's length property
         * @param ctor should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}
         */
        public Builder(String name, int length, BuiltInJSCodeExec<JSFunction> ctor) {
            this.name = name;
            this.ctor = buildDescriptor(name, length, JSCode.NOT_CALLABLE, buildOptJSCode(ctor));
            this.ctorProps = new Props(DONTENUM);
            this.protoProps = new Props(DONTENUM | READONLY | PERMANENT);
        }

        public Builder(SymbolKey name, int length, BuiltInJSCodeExec<JSFunction> ctor) {
            this.name = name;
            this.ctor = buildDescriptor(name, length, JSCode.NOT_CALLABLE, buildOptJSCode(ctor));
            this.ctorProps = new Props(DONTENUM);
            this.protoProps = new Props(DONTENUM | READONLY | PERMANENT);
        }

        /**
         * Create a builder for a constructor with non-standard attributes that can only be called
         * as a constructor.
         *
         * @param name the constructor's name
         * @param length the constructor's length property
         * @param ctor should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}
         * @param ctorAttrs the attributes for the constructor property in its owning scope
         * @param protoAttrs the attributes for the constructor's prototype property
         */
        public Builder(
                String name,
                int length,
                BuiltInJSCodeExec<JSFunction> ctor,
                int ctorAttrs,
                int protoAttrs) {
            this.name = name;
            this.ctor = buildDescriptor(name, length, buildOptJSCode(ctor));
            this.ctorProps = new Props(ctorAttrs);
            this.protoProps = new Props(protoAttrs);
        }

        /**
         * Create a builder for a constructor with the standard attributes for a built in EcmaScript
         * class that can be called as either a constructor of as a functio., which may have
         * different behaviours.
         *
         * @param name the constructor's name
         * @param length the constructor's length property
         * @param call should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed if the constructor is used as
         *     a normal function.
         * @param construct should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>} This is what will be executed if the constructor is used as a
         *     constructor.
         */
        public Builder(
                String name,
                int length,
                BuiltInJSCodeExec<JSFunction> call,
                BuiltInJSCodeExec<JSFunction> construct) {
            this(name, length, call, construct, DONTENUM, READONLY | DONTENUM | PERMANENT);
        }

        public Builder(
                SymbolKey name,
                int length,
                BuiltInJSCodeExec<JSFunction> call,
                BuiltInJSCodeExec<JSFunction> construct) {
            this(name, length, call, construct, DONTENUM, READONLY | DONTENUM | PERMANENT);
        }

        public Builder(
                SymbolKey name,
                String ctorName,
                int length,
                BuiltInJSCodeExec<JSFunction> call,
                BuiltInJSCodeExec<JSFunction> construct) {
            this(
                    name,
                    ctorName,
                    length,
                    call,
                    construct,
                    DONTENUM,
                    READONLY | DONTENUM | PERMANENT);
        }

        /**
         * Create a builder for a constructor with the standard attributes that can be called as
         * either a constructor of as a functio., which may have different behaviours.
         *
         * @param name the constructor's name
         * @param length the constructor's length property
         * @param call should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed if the constructor is used as
         *     a normal function.
         * @param construct should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>} This is what will be executed if the constructor is used as a
         *     constructor.
         * @param ctorAttrs the attributes for the constructor property in its owning scope
         * @param protoAttrs the attributes for the constructor's prototype property
         */
        public Builder(
                String name,
                int length,
                BuiltInJSCodeExec<JSFunction> call,
                BuiltInJSCodeExec<JSFunction> construct,
                int ctorAttrs,
                int protoAttrs) {
            this.name = name;
            this.ctor =
                    buildDescriptor(name, length, buildOptJSCode(call), buildOptJSCode(construct));
            this.ctorProps = new Props(ctorAttrs);
            this.protoProps = new Props(protoAttrs);
        }

        public Builder(
                SymbolKey name,
                int length,
                BuiltInJSCodeExec<JSFunction> call,
                BuiltInJSCodeExec<JSFunction> construct,
                int ctorAttrs,
                int protoAttrs) {
            this.name = name;
            this.ctor =
                    buildDescriptor(name, length, buildOptJSCode(call), buildOptJSCode(construct));
            this.ctorProps = new Props(ctorAttrs);
            this.protoProps = new Props(protoAttrs);
        }

        public Builder(
                SymbolKey name,
                String ctorName,
                int length,
                BuiltInJSCodeExec<JSFunction> call,
                BuiltInJSCodeExec<JSFunction> construct,
                int ctorAttrs,
                int protoAttrs) {
            this.name = name;
            this.ctor =
                    buildDescriptor(
                            ctorName, length, buildOptJSCode(call), buildOptJSCode(construct));
            this.ctorProps = new Props(ctorAttrs);
            this.protoProps = new Props(protoAttrs);
        }

        /**
         * Adds a method on the constructor with the standard property attributes for a method on a
         * built in constructor.
         *
         * @param name the method's name. The symbol will be used as the method's key in the owning
         *     scope, and text of that symbol will be used as the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @return this {@link Builder}
         */
        public Builder withMethod(
                Destination dest, SymbolKey name, int length, BuiltInJSCodeExec<JSFunction> code) {
            return withMethod(dest, name, length, code, DONTENUM, DONTENUM | READONLY);
        }

        /**
         * Adds a method on the constructor with the custom property attributes.
         *
         * @param name the method's name. The symbol will be used as the method's key in the owning
         *     scope, and text of that symbol will be used as the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @param attributes the attributes for the method's property on the constructor
         * @param stdAttrs the attributes for the method's standard properties
         * @return this {@link Builder}
         */
        public Builder withMethod(
                Destination dest,
                SymbolKey name,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            return withMethodInt(
                    dest, name, "[" + name.getName() + "]", length, code, attributes, stdAttrs);
        }

        /**
         * Adds a method on the constructor with the standard property attributes for a method on a
         * built in constructor.
         *
         * @param name the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @return this {@link Builder}
         */
        public Builder withMethod(
                Destination dest, String name, int length, BuiltInJSCodeExec<JSFunction> code) {
            return withMethod(dest, name, length, code, DONTENUM, DONTENUM | READONLY);
        }

        /**
         * Adds a method on the constructor with the custom property attributes.
         *
         * @param name the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @param attributes the attributes for the method's property on the constructor
         * @param stdAttrs the attributes for the method's standard properties
         * @return this {@link Builder}
         */
        public Builder withMethod(
                Destination dest,
                String name,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            return withMethodInt(dest, name, name, length, code, attributes, stdAttrs);
        }

        private Builder withMethodInt(
                Destination dest,
                Object name,
                String descName,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            dest.get(this)
                    .funcs
                    .add(
                            new FuncPropDesc(
                                    name,
                                    buildDescriptor(name, length, buildOptJSCode(code)),
                                    attributes,
                                    stdAttrs));
            return this;
        }

        /**
         * Adds an accessor based property to the constructor or prototype with custom property
         * attributes.
         *
         * @param dest destination for this property
         * @param name the property's name.
         * @param getter should usually be a static method reference that can be cast as a {@link
         *     LambdaSetterFunction}. This is what will be executed when the property is set..
         * @param attributes the attributes for the property on the prototype
         * @return this {@link Builder}
         */
        public Builder withProp(
                Destination dest,
                String name,
                LambdaGetterFunction getter,
                LambdaSetterFunction setter,
                int attributes) {
            dest.get(this).props.add(new LambdaGetSetPropDesc(name, getter, setter, attributes));
            return this;
        }

        /**
         * Version of {@link #withProp(Destination, String, LambdaGetterFunction,
         * LambdaSetterFunction, int)} that takes a {@link SymbolKey} as the name.
         */
        public Builder withProp(
                Destination dest,
                SymbolKey name,
                LambdaGetterFunction getter,
                LambdaSetterFunction setter,
                int attributes) {
            dest.get(this).props.add(new LambdaGetSetPropDesc(name, getter, setter, attributes));
            return this;
        }

        /**
         * Version of {@link #withProp(Destination, String, LambdaGetterFunction,
         * LambdaSetterFunction, int)} that takes a {@link ValueCreator} rather than a getter and
         * setter.
         */
        public Builder withProp(Destination dest, String name, ValueCreator creator) {
            dest.get(this).props.add(new CreateValuePropDesc(name, creator));
            return this;
        }

        /**
         * Version of {@link #withProp(Destination, SymbolKey, LambdaGetterFunction,
         * LambdaSetterFunction, int)} that takes a {@link ValueCreator} rather than a getter and
         * setter.
         */
        public Builder withProp(Destination dest, SymbolKey name, ValueCreator creator) {
            dest.get(this).props.add(new CreateValuePropDesc(name, creator));
            return this;
        }

        /**
         * Convenience method to create an alias of the orignally named slot with the specified
         * attributes.
         */
        public static ValueCreator alias(String original, int attributes) {
            return (cx, scope, obj) -> new DescriptorInfo(obj.get(original, obj), attributes, true);
        }

        /** Convenience method to create an alias of the orignally named slot. */
        public static ValueCreator aliss(String original) {
            return alias(original, DONTENUM | READONLY | PERMANENT);
        }

        /**
         * Convenience method to create a slot with the specified value and attributes. This should
         * only be used for simply values such as strings, symbols, and numbers which remain truly
         * constant between contexts.
         */
        public static ValueCreator value(Object value, int attributes) {
            return (c, s, o) -> new DescriptorInfo(value, attributes, true);
        }

        /**
         * Convenience method to create a slot with the specified value. This should only be used
         * for simply values such as strings, symbols, and numbers which remain truly constant
         * between contexts.
         */
        public static ValueCreator value(Object value) {
            return value(value, DONTENUM | READONLY | PERMANENT);
        }

        @SuppressWarnings("unchecked")
        private JSCode<JSFunction> buildOptJSCode(BuiltInJSCodeExec<JSFunction> code) {
            return new BuiltInJSCode(code, (JSCodeResume<JSFunction>) JSCodeResume.NULL_RESUMABLE);
        }

        private JSDescriptor<JSFunction> buildDescriptor(
                Object id, int length, JSCode<JSFunction> code) {
            return buildDescriptor(id, length, code, null);
        }

        private JSDescriptor<JSFunction> buildDescriptor(
                Object id, int length, JSCode<JSFunction> call, JSCode<JSFunction> construct) {
            String name =
                    id instanceof String ? (String) id : ("[" + ((SymbolKey) id).getName() + "]");
            var builder = new JSDescriptor.Builder<JSFunction>();
            builder.code = identityBuilder(call);
            builder.constructor = identityBuilder(construct);
            builder.name = name;
            builder.arity = length;
            // Built in functions do not default `thisObj` or perform
            // `toObject` on it unless specified, so we'll mark them as strict.
            builder.isStrict = true;
            builder.paramCount = length;
            var paramNames = new String[length];
            Arrays.fill(paramNames, "");
            builder.paramAndVarNames = paramNames;
            builder.paramIsConst = new boolean[length];
            builder.paramAndVarCount = length;
            builder.rawSource = String.format("function %s() {\n\t[native code]\n}", name);
            builder.rawSourceStart = 0;
            builder.rawSourceEnd = builder.rawSource.length();

            builder.hasPrototype = true;
            return builder.build(desc -> {});
        }

        public ClassDescriptor build() {
            return new ClassDescriptor(
                    new FuncPropDesc(name, ctor, ctorProps.attrs, protoProps.attrs),
                    List.copyOf(ctorProps.funcs),
                    List.copyOf(ctorProps.props),
                    List.copyOf(protoProps.funcs),
                    List.copyOf(protoProps.props));
        }
    }

    private static class BuiltInJSCode extends JSCode<JSFunction> implements Serializable {

        private static final long serialVersionUID = 2691205302914111400L;

        private final JSCodeExec<JSFunction> exec;
        private final JSCodeResume<JSFunction> resume;

        private BuiltInJSCode(JSCodeExec<JSFunction> exec, JSCodeResume<JSFunction> resume) {
            this.exec = exec;
            this.resume = resume;
        }

        @Override
        public Object execute(
                Context cx,
                JSFunction executableObject,
                Object newTarget,
                Scriptable scope,
                Object thisObj,
                Object[] args) {
            return exec.execute(cx, executableObject, newTarget, scope, thisObj, args);
        }

        @Override
        public Object resume(
                Context cx,
                JSFunction executableObject,
                Object state,
                Scriptable scope,
                int operation,
                Object value) {
            return resume.resume(cx, executableObject, state, scope, operation, value);
        }
    }

    public static BuiltInJSCodeExec<JSFunction> typeError() {
        return (c, f, nt, s, thisObj, args) -> {
            throw ScriptRuntime.typeErrorById("msg.only.from.new", f.getFunctionName());
        };
    }

    public interface BuiltInJSCodeExec<U extends ScriptOrFn<U>>
            extends JSCodeExec<U>, Serializable {}

    public interface BuiltInJSCodeResume<U extends ScriptOrFn<U>>
            extends JSCodeExec<U>, Serializable {}

    private static JSCode.Builder<JSFunction> identityBuilder(JSCode<JSFunction> code) {
        return new JSCode.Builder<JSFunction>() {
            @Override
            public JSCode<JSFunction> build() {
                return code;
            }
        };
    }
}
