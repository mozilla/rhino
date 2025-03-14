package org.mozilla.javascript.nat.type;

import java.util.IdentityHashMap;

public class BasicClassTypeInfo extends ClassTypeInfo {
    static final IdentityHashMap<Class<?>, BasicClassTypeInfo> CACHE = new IdentityHashMap<>();

    BasicClassTypeInfo(Class<?> type) {
        super(type);
    }
}
