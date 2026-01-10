package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 *                       .withProtoMethod("toString", 0, NativeBoolean::js_toString)
 *                       .withProtoMethod("toSource", 0, NativeBoolean::js_toSource)
 *                       .withProtoMethod("valueOf", 0, NativeBoolean::js_valueOf)
 *                       .build();
 *
 *   static void init(Scriptable scope, boolean sealed) {
 *       // Boolean is an unusual object in that the prototype is itself a Boolean
 *       var constructor = DESCRIPTOR.buildConstructor(scope, new NativeBoolean(false), sealed);
 *   }
 * </pre>
 *
 * Then
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

    private static class PropDesc {
        private final Object name;
        private final ScriptableObject.LambdaGetterFunction getter;
        private final ScriptableObject.LambdaSetterFunction setter;
        private final int attributes;

        PropDesc(
                Object name,
                ScriptableObject.LambdaGetterFunction getter,
                ScriptableObject.LambdaSetterFunction setter,
                int attributes) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.attributes = attributes;
        }
    }

    private final FuncPropDesc ctorDesc;
    private final List<FuncPropDesc> ctorDescs;
    private final List<FuncPropDesc> protoDescs;
    private final List<PropDesc> protoProps;

    private ClassDescriptor(
            FuncPropDesc ctorDesc,
            List<FuncPropDesc> ctorDescs,
            List<FuncPropDesc> protoDescs,
            List<PropDesc> protoProps) {
        this.ctorDesc = ctorDesc;
        this.ctorDescs = ctorDescs;
        this.protoDescs = protoDescs;
        this.protoProps = protoProps;
    }

    /** Build a constructor from this descriptor. */
    public JSFunction buildConstructor(
            Context cx, Scriptable scope, ScriptableObject proto, boolean sealed) {
        var ctor = new JSFunction(scope, ctorDesc.funcDesc, null, null);
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
        ctor.setPrototypeProperty(proto);
        ctor.setPrototypePropertyAttributes(ctorDesc.stdAttrs);
        proto.setParentScope(scope);
        proto.put("constructor", proto, ctor);
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
            proto.defineProperty(cx, (String) p.name, p.getter, p.setter, p.attributes);
        }

        ScriptableObject.defineProperty(scope, ctorDesc.name.toString(), ctor, ctorDesc.attributes);
        if (sealed) {
            ctor.sealObject();
            proto.sealObject();
        }
        return ctor;
    }

    /**
     * A builder for creating class descriptors. Usually created during static initialisation
     * methods can be added before finally calling {@link Builder#build()} to create the final
     * descriptor.
     */
    public static class Builder {
        private final JSDescriptor<JSFunction> ctor;
        private final List<FuncPropDesc> ctorDescs = new ArrayList<>();
        private final List<FuncPropDesc> protoDescs = new ArrayList<>();
        private final List<PropDesc> protoProps = new ArrayList<>();
        private final String name;
        private final int ctorAttrs;
        private final int protoAttrs;

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
            this.ctor = buildDescriptor(name, length, buildOptJSCode(ctor));
            this.ctorAttrs = DONTENUM;
            this.protoAttrs = DONTENUM | READONLY | PERMANENT;
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
            this.ctorAttrs = ctorAttrs;
            this.protoAttrs = protoAttrs;
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
            this.ctorAttrs = ctorAttrs;
            this.protoAttrs = protoAttrs;
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
        public Builder withCtorMethod(
                SymbolKey name, int length, BuiltInJSCodeExec<JSFunction> code) {
            return withCtorMethod(name, length, code, DONTENUM, DONTENUM | READONLY);
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
        public Builder withCtorMethod(
                SymbolKey name,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            return withCtorMethodInt(name, name.getName(), length, code, attributes, stdAttrs);
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
        public Builder withCtorMethod(String name, int length, BuiltInJSCodeExec<JSFunction> code) {
            return withCtorMethod(name, length, code, DONTENUM, DONTENUM | READONLY);
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
        public Builder withCtorMethod(
                String name,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            return withCtorMethodInt(name, name, length, code, attributes, stdAttrs);
        }

        private Builder withCtorMethodInt(
                Object name,
                String descName,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            ctorDescs.add(
                    new FuncPropDesc(
                            name,
                            buildDescriptor(descName, length, buildOptJSCode(code)),
                            attributes,
                            stdAttrs));
            return this;
        }

        /**
         * Adds a method on the prototype with the standard property attributes for a method on a
         * built in constructor.
         *
         * @param name the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @return this {@link Builder}
         */
        public Builder withProtoMethod(
                String name, int length, BuiltInJSCodeExec<JSFunction> code) {
            return withProtoMethod(name, length, code, DONTENUM, DONTENUM | READONLY);
        }

        /**
         * Adds a method on the prototype with the custom property attributes.
         *
         * @param name the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @param attributes the attributes for the method's property on the prototype
         * @param stdAttrs the attributes for the method's standard properties
         * @return this {@link Builder}
         */
        public Builder withProtoMethod(
                String name,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            return withProtoMethodInt(name, name, length, code, attributes, stdAttrs);
        }

        /**
         * Adds a method on the prototype with the standard property attributes for a method on a
         * built in constructor.
         *
         * @param name the method's name. The symbol will be used as the method's key in the owning
         *     scope, and text of that symbol will be used as the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @return this {@link Builder}
         */
        public Builder withProtoMethod(
                SymbolKey name, int length, BuiltInJSCodeExec<JSFunction> code) {
            return withProtoMethod(name, length, code, DONTENUM, DONTENUM | READONLY);
        }

        /**
         * Adds a method on the prototype with the custom property attributes.
         *
         * @param name the method's name. The symbol will be used as the method's key in the owning
         *     scope, and text of that symbol will be used as the method's name.
         * @param length the method's length property
         * @param code should usually be a static method reference that can be cast as a {@link
         *     JSCodeExec<JSFunction>}. This is what will be executed when the method is called.
         * @param attributes the attributes for the method's property on the prototype
         * @param stdAttrs the attributes for the method's standard properties
         * @return this {@link Builder}
         */
        public Builder withProtoMethod(
                SymbolKey name,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            return withProtoMethodInt(name, name.getName(), length, code, attributes, stdAttrs);
        }

        private Builder withProtoMethodInt(
                Object name,
                String descName,
                int length,
                BuiltInJSCodeExec<JSFunction> code,
                int attributes,
                int stdAttrs) {
            protoDescs.add(
                    new FuncPropDesc(
                            name,
                            buildDescriptor(descName, length, buildOptJSCode(code)),
                            attributes,
                            stdAttrs));
            return this;
        }

        /**
         * Adds an accessor based property to the prototype with custom property attributes.
         *
         * @param name the property's name.
         * @param getter should usually be a static method reference that can be cast as a {@link
         *     LambdaGetterFunction}. This is what will be executed when the property is got..
         * @param getter should usually be a static method reference that can be cast as a {@link
         *     LambdaSetterFunction}. This is what will be executed when the property is set..
         * @param attributes the attributes for the property on the prototype
         * @return this {@link Builder}
         */
        public Builder withPrototypeProperty(
                String name,
                LambdaGetterFunction getter,
                LambdaSetterFunction setter,
                int attributes) {
            protoProps.add(new PropDesc(name, getter, setter, attributes));
            return this;
        }

        @SuppressWarnings("unchecked")
        private JSCode<JSFunction> buildOptJSCode(BuiltInJSCodeExec<JSFunction> code) {
            return new BuiltInJSCode(code, (JSCodeResume<JSFunction>) JSCodeResume.NULL_RESUMABLE);
        }

        private JSDescriptor<JSFunction> buildDescriptor(
                String name, int length, JSCode<JSFunction> code) {
            return buildDescriptor(name, length, code, null);
        }

        private JSDescriptor<JSFunction> buildDescriptor(
                String name, int length, JSCode<JSFunction> call, JSCode<JSFunction> construct) {
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

        ClassDescriptor build() {
            return new ClassDescriptor(
                    new FuncPropDesc(name, ctor, ctorAttrs, protoAttrs),
                    List.copyOf(ctorDescs),
                    List.copyOf(protoDescs),
                    List.copyOf(protoProps));
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
