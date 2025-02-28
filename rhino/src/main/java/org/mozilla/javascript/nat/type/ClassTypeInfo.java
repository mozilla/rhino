package org.mozilla.javascript.nat.type;

import java.util.Collections;
import java.util.Set;

public abstract class ClassTypeInfo extends TypeInfoBase {
	private final Class<?> type;
	private Set<Class<?>> typeSet;

	ClassTypeInfo(Class<?> type) {
		this.type = type;
	}

	@Override
	public Class<?> asClass() {
		return type;
	}

	@Override
	public boolean is(TypeInfo info) {
		if (info instanceof ParameterizedTypeInfo) {
			return info.is(this);
		}
		return super.is(info);
	}

	@Override
	public boolean shouldConvert() {
		return type != Object.class;
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof ClassTypeInfo && type == ((ClassTypeInfo) o).type;
	}

	@Override
	public String toString() {
		return type.getName();
	}

	@Override
	public void append(TypeFormatContext ctx, StringBuilder builder) {
		builder.append(ctx.getClassName(this.type));
	}

	@Override
	public boolean isVoid() {
		return type == Void.class || type == Void.TYPE;
	}

	@Override
	public boolean isBoolean() {
		return type == Boolean.class || type == Boolean.TYPE;
	}

	@Override
	public boolean isNumber() {
		return Number.class.isAssignableFrom(type);
	}

	@Override
	public boolean isByte() {
		return type == Byte.class || type == Byte.TYPE;
	}

	@Override
	public boolean isShort() {
		return type == Short.class || type == Short.TYPE;
	}

	@Override
	public boolean isInt() {
		return type == Integer.class || type == Integer.TYPE;
	}

	@Override
	public boolean isLong() {
		return type == Long.class || type == Long.TYPE;
	}

	@Override
	public boolean isFloat() {
		return type == Float.class || type == Float.TYPE;
	}

	@Override
	public boolean isDouble() {
		return type == Double.class || type == Double.TYPE;
	}

	@Override
	public boolean isCharacter() {
		return type == Character.class || type == Character.TYPE;
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		if (typeSet == null) {
			typeSet = Collections.singleton(type);
		}
		return typeSet;
	}
}
