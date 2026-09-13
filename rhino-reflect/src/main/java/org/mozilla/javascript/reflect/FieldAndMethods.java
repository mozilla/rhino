package org.mozilla.javascript.reflect;

import java.io.Serial;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.lc.member.ExecutableOverload;
import org.mozilla.javascript.lc.member.NativeJavaField;

class FieldAndMethods extends NativeJavaMethod {
    @Serial private static final long serialVersionUID = -9222428244284796755L;

    FieldAndMethods(VarScope scope, ExecutableOverload.WithField withField) {
        super(scope, withField.methods, withField.name);
        this.field = withField.field;
        setParentScope(scope);
        setPrototype(ScriptableObject.getFunctionPrototype(scope));
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == ScriptRuntime.FunctionClass) return this;
        Object rval;
        try {
            rval = field.get(javaObject);
        } catch (IllegalAccessException accEx) {
            throw Context.reportRuntimeErrorById(
                    "msg.java.internal.private", field.raw().getName());
        }
        Context cx = Context.getCurrentContext();
        rval = cx.getWrapFactory().wrap(cx, this.getParentScope(), rval, field.type());
        if (rval instanceof Scriptable) {
            rval = ((Scriptable) rval).getDefaultValue(hint);
        }
        return rval;
    }

    NativeJavaField field;
    Object javaObject;
}
