package org.mozilla.javascript.lc.member;

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

    public Field raw() {
        return field;
    }

    public TypeInfo type() {
        return type;
    }

    public Object get(Object javaObject) throws IllegalAccessException {
        return field.get(javaObject);
    }

    /**
     * Note: will do nothing when called on a final field
     */
    public void set(Object javaObject, Object value) throws IllegalAccessException {
        if ((field.getModifiers() & Modifier.FINAL) != 0) {
            // treat Java final the same as JavaScript [[READONLY]]
            return;
        }
        field.set(javaObject, value);
    }
}
