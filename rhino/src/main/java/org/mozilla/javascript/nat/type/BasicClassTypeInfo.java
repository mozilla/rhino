package org.mozilla.javascript.nat.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicClassTypeInfo extends ClassTypeInfo {
    static final Map<Class<?>, BasicClassTypeInfo> CACHE = new ConcurrentHashMap<>();

    BasicClassTypeInfo(Class<?> type) {
        super(type);
    }
}
