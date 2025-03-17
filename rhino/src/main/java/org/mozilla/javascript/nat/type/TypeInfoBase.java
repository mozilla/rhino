package org.mozilla.javascript.nat.type;

import org.mozilla.javascript.nat.ByteAsBool;

import java.lang.reflect.Array;
import java.util.Map;

public abstract class TypeInfoBase implements TypeInfo {
    private TypeInfo asArray;
    private Object emptyArray;

    @Override
    public TypeInfo asArray() {
        if (asArray == null) {
            asArray = new ArrayTypeInfo(this);
        }

        return asArray;
    }

    @Override
    public Object newArray(int length) {
        if (length == 0) {
            if (emptyArray == null) {
                emptyArray = Array.newInstance(asClass(), 0);
            }

            return emptyArray;
        }

        return Array.newInstance(asClass(), length);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        append(TypeFormatContext.DEFAULT, builder);
        return builder.toString();
    }

    public static abstract class OptionallyConsolidatable extends TypeInfoBase {
        private byte consolidatable = ByteAsBool.UNKNOWN;

        @Override
        public final TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
            if (ByteAsBool.isUnknown(consolidatable)) {
                var consolidated = consolidateImpl(mapping);
                consolidatable = ByteAsBool.fromBool(consolidated != this);
                return consolidated;
            }
            return ByteAsBool.isTrue(consolidatable)
                ? consolidateImpl(mapping)
                : this;
        }

        protected abstract TypeInfo consolidateImpl(Map<VariableTypeInfo, TypeInfo> mapping);
    }
}
