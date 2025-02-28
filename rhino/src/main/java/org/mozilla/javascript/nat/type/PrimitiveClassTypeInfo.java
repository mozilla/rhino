package org.mozilla.javascript.nat.type;

public class PrimitiveClassTypeInfo extends ClassTypeInfo {
	private final Object defaultValue;

	public PrimitiveClassTypeInfo(Class<?> type, Object defaultValue) {
		super(type);
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	public Object createDefaultValue() {
		return defaultValue;
	}
}
