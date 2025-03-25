package org.mozilla.javascript.nat.type.definition;

import org.mozilla.javascript.nat.type.TypeInfo;

import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * @see ParameterizedType
 * @author ZZZank
 */
public interface ParameterizedTypeInfo extends TypeInfo {

    /**
     * @see ParameterizedType#getRawType()
     */
    TypeInfo rawType();

    /**
     * @see ParameterizedType#getActualTypeArguments()
     */
    List<TypeInfo> params();

    @Override
    default TypeInfo param(int index) {
        final var params = params();
        return index >= 0 && index < params.size() ? params.get(index) : TypeInfo.NONE;
    }
}
