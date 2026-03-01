package org.mozilla.javascript.lc.member;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * @author ZZZank
 */
public final class NativeJavaField implements Serializable {

    private static final long serialVersionUID = -3440381785576412928L;

    private transient Field field;
    private final boolean isFinal;
    private transient TypeInfo type;

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

    private void init(Field field, TypeInfoFactory factory, Class<?> parent) {
        this.field = field;
        this.type = factory.create(field.getGenericType());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        var fieldName = (String) in.readObject();
        var declaringClass = (Class<?>) in.readObject();

        try {
            var field = declaringClass.getField(fieldName);
            init(field, TypeInfoFactory.GLOBAL, declaringClass);
        } catch (NoSuchFieldException e) {
            throw new IOException("Cannot find member: " + e);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(field.getName());
        out.writeObject(field.getDeclaringClass());
    }
}
