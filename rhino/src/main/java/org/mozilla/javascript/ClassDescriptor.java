package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a descriptor for a constructor and its prototype methods which can then be
 * used to build a real constructor function and prototype during standard object initialisation.
 */
public class ClassDescriptor {

    private final JSDescriptor<JSFunction> ctorDesc;
    private final Map<String, JSDescriptor<JSFunction>> ctorDescs;
    private final Map<String, JSDescriptor<JSFunction>> protoDescs;

    private ClassDescriptor(
            JSDescriptor<JSFunction> ctorDesc,
            Map<String, JSDescriptor<JSFunction>> ctorDescs,
            Map<String, JSDescriptor<JSFunction>> protoDescs) {
        this.ctorDesc = ctorDesc;
        this.ctorDescs = ctorDescs;
        this.protoDescs = protoDescs;
    }

    public JSFunction buildConstructor(Scriptable scope, ScriptableObject proto) {
        var ctor = new JSFunction(scope, ctorDesc, null, null);
        for (var e : ctorDescs.entrySet()) {
            var f = new JSFunction(scope, e.getValue(), null, null);
            f.setStandardPropertyAttributes(DONTENUM | READONLY);
            ctor.put(e.getKey(), f, DONTENUM);
        }
        ctor.setPrototypeProperty(proto);
        proto.put("constructor", proto, ctor);
        var objProto = ScriptableObject.getObjectPrototype(scope);
        if (proto != objProto) {
            proto.setPrototype(objProto);
        }
        proto.setAttributes("constructor", DONTENUM);
        for (var e : protoDescs.entrySet()) {
            var f = new JSFunction(scope, e.getValue(), null, null);
            f.setStandardPropertyAttributes(DONTENUM | READONLY);
            proto.put(e.getKey(), proto, f);
            proto.setAttributes(e.getKey(), DONTENUM);
        }
        return ctor;
    }

    public static class Builder {
        private final JSDescriptor<JSFunction> ctor;
        private final HashMap<String, JSDescriptor<JSFunction>> ctorDescs = new HashMap<>();
        private final HashMap<String, JSDescriptor<JSFunction>> protoDescs = new HashMap<>();

        public Builder(int length, BuiltInJSCodeExec<JSFunction> ctor) {
            this.ctor = buildDescriptor("constructor", 1, buildOptJSCode(ctor));
        }

        public Builder(
                int length,
                BuiltInJSCodeExec<JSFunction> call,
                BuiltInJSCodeExec<JSFunction> construct) {
            this.ctor =
                    buildDescriptor(
                            "constructor", 1, buildOptJSCode(call), buildOptJSCode(construct));
        }

        public void addCtroMethod(String name, int length, BuiltInJSCodeExec<JSFunction> code) {
            ctorDescs.put(name, buildDescriptor(name, length, buildOptJSCode(code)));
        }

        public void addProtoMethod(String name, int length, BuiltInJSCodeExec<JSFunction> code) {
            protoDescs.put(name, buildDescriptor(name, length, buildOptJSCode(code)));
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
            builder.code = identitiyBuilder(call);
            builder.constructor = identitiyBuilder(construct);
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

            builder.hasPrototype = true;
            return builder.build(desc -> {});
        }

        ClassDescriptor build() {
            return new ClassDescriptor(ctor, Map.copyOf(ctorDescs), Map.copyOf(protoDescs));
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

    private static JSCode.Builder<JSFunction> identitiyBuilder(JSCode<JSFunction> code) {
        return new JSCode.Builder<JSFunction>() {
            @Override
            public JSCode<JSFunction> build() {
                return code;
            }
        };
    }
}
