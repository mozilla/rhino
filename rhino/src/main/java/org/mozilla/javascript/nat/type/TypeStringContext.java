package org.mozilla.javascript.nat.type;

public interface TypeStringContext {
	TypeStringContext DEFAULT = new TypeStringContext() {
	};

	default String toString(TypeInfo info) {
		var sb = new StringBuilder();
		append(sb, info);
		return sb.toString();
	}

	default void append(StringBuilder sb, TypeInfo type) {
		type.append(this, sb);
	}

	default void appendClassName(StringBuilder sb, ClassTypeInfo type) {
		sb.append(type.asClass().getName());
	}

	default void appendSpace(StringBuilder sb) {
		sb.append(' ');
	}
}
