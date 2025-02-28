package org.mozilla.javascript.nat.type;

import java.util.Collection;
import java.util.Set;

public final class NoTypeInfo implements TypeInfo {
	static final NoTypeInfo INSTANCE = new NoTypeInfo();

	private NoTypeInfo() {}

	@Override
	public Class<?> asClass() {
		return Object.class;
	}

	@Override
	public boolean shouldConvert() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "?";
	}

	@Override
	public void append(TypeFormatContext ctx, StringBuilder builder) {
		builder.append('?');
	}

	@Override
	public TypeInfo asArray() {
		return this;
	}

	@Override
	public TypeInfo withParams(TypeInfo... params) {
		return this;
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		return Set.of();
	}
}
