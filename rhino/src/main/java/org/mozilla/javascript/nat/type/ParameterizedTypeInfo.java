package org.mozilla.javascript.nat.type;

import org.mozilla.javascript.FunctionObject;

import java.util.Collection;
import java.util.List;

public final class ParameterizedTypeInfo extends TypeInfoBase {
	private final TypeInfo rawType;
	private final List<TypeInfo> params;
	private int hashCode;

	ParameterizedTypeInfo(TypeInfo rawType, TypeInfo[] params) {
		this.rawType = rawType;
		this.params = List.of(params);
	}

	@Override
	public Class<?> asClass() {
		return rawType.asClass();
	}

	@Override
	public boolean is(Class<?> c) {
		return rawType.is(c);
	}

	@Override
	public TypeInfo param(int index) {
		if (index < 0 || index >= params.size()) {
			return TypeInfo.NONE;
		}
		var got = params.get(index);
		return got == TypeInfo.OBJECT ? TypeInfo.NONE : got;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = rawType.hashCode() * 31 + params.hashCode();
		}

		return hashCode;
	}

	@Override
	public boolean equals(Object object) {
		return this == object || object instanceof ParameterizedTypeInfo
			&& rawType.equals(((ParameterizedTypeInfo) object).rawType)
            && params.equals(((ParameterizedTypeInfo) object).params);
	}

	@Override
	public void append(TypeFormatContext ctx, StringBuilder builder) {
		ctx.formatParameterized(builder, this);
	}

	@Override
	public String signature() {
		return rawType.signature();
	}

	public TypeInfo rawType() {
		return rawType;
	}

	public List<TypeInfo> params() {
		return params;
	}

	@Override
	public Object newArray(int length) {
		return rawType.newArray(length);
	}

	@Override
	public TypeInfo withParams(TypeInfo... params) {
		return rawType.withParams(params);
	}

	@Override
	public boolean isFunctionalInterface() {
		return rawType.isFunctionalInterface();
	}

	@Override
	public List<Object> enumConstants() {
		return rawType.enumConstants();
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
		rawType.collectContainedComponentClasses(classes);

		for (var param : params) {
			param.collectContainedComponentClasses(classes);
		}
	}

	@Override
	public boolean isInterface() {
		return this.rawType.isInterface();
	}

	/**
	 * none of the base types is parameterized
	 */
	@Override
	public int getTypeTag() {
		return FunctionObject.JAVA_UNSUPPORTED_TYPE;
	}
}
