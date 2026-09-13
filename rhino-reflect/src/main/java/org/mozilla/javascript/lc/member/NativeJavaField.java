package org.mozilla.javascript.lc.member;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
public final class NativeJavaField {
    private final Field field;
    private final boolean isFinal;
    private final TypeInfo type;

    public NativeJavaField(Field field, TypeInfoFactory typeFactory) {
        this.field = field;
        this.isFinal = Modifier.isFinal(field.getModifiers());
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

    /** Note: will do nothing when called on a final field */
    public void set(Object javaObject, Object value) throws IllegalAccessException {
        if (isFinal) {
            // treat Java final the same as JavaScript [[READONLY]]
            return;
        }
        field.set(javaObject, value);
    }
}
