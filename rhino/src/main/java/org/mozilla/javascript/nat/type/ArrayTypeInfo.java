package org.mozilla.javascript.nat.type;

import org.mozilla.javascript.FunctionObject;

import java.util.Collection;
import java.util.Set;

public final class ArrayTypeInfo extends TypeInfoBase {
	private final TypeInfo component;
	private Class<?> asClass;

	ArrayTypeInfo(TypeInfo component) {
		this.component = component;
	}

	@Override
	public boolean is(Class<?> c) {
		return c.isArray() && asClass() == c;
	}

	@Override
	public Class<?> asClass() {
		if (asClass == null) {
			asClass = component.newArray(0).getClass();
		}

		return asClass;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof ArrayTypeInfo && component.equals(((ArrayTypeInfo) obj).component);
	}

	@Override
	public int hashCode() {
		return component.hashCode();
	}

	@Override
	public String toString() {
		return component + "[]";
	}

	@Override
	public void append(TypeFormatContext ctx, StringBuilder builder) {
		ctx.formatArray(builder, this);
	}

	@Override
	public String signature() {
		return component.signature() + "[]";
	}

	@Override
	public TypeInfo componentType() {
		return component;
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
		component.collectContainedComponentClasses(classes);
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		return component.getContainedComponentClasses();
	}

	@Override
	public boolean isArray() {
		return true;
	}

	/**
	 * array type is not any of the base types
	 * @see TypeInfo#getTypeTag()
	 */
	@Override
	public int getTypeTag() {
		return FunctionObject.JAVA_UNSUPPORTED_TYPE;
	}
}
