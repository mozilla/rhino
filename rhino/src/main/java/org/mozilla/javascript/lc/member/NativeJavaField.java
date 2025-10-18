package org.mozilla.javascript.lc.member;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author ZZZank
 */
public final class NativeJavaField {
    private final Field field;
    private final TypeInfo type;

    public NativeJavaField(Field field, TypeInfoFactory typeFactory) {
        this.field = field;
        this.type = typeFactory.create(field.getGenericType());
    }

    public TypeInfo type() {
        return type;
    }

    public Object get(Object javaObject) {
        try {
            return field.get(javaObject);
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
    }

    public void set(Object javaObject, Object value) {
        try {
            field.set(javaObject, value);
        } catch (IllegalAccessException accessEx) {
            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                // treat Java final the same as JavaScript [[READONLY]]
                return;
            }
            throw Context.throwAsScriptRuntimeEx(accessEx);
        } catch (IllegalArgumentException argEx) {
            throw Context.reportRuntimeErrorById(
                "msg.java.internal.field.type",
                value.getClass().getName(),
                field,
                javaObject.getClass().getName());
        }
    }
}
