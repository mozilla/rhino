/*
 * Licensed Materials - Property of FOCONIS AG
 * (C) Copyright FOCONIS AG.
 */

package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Type lookup helper. This is used to read the generic info of Lists / Maps in NativeJavaList/Map.
 *
 * <p>If we have a class that implements <code>List&lt;SomeType&gt;</code>, we need to extract
 * <code>SomeType</code>, so that we can convert the value to the appropriate data type.
 *
 * <p>For example, if you have a <code>List&lt;Integer&gt;</code>, you expect, that the list only
 * contains integers. But there is no type checking at runtime. Rhino tries to read the type
 * information and tries to convert the values with {@link Context#jsToJava(Object, Class)}.
 *
 * <p><b>Example 1:</b> <code>integerList[1] = '123'</code> will convert the String '123' to an
 * integer, so the underlying java list will contain the correct data type.
 *
 * <p><b>Note 1:</b> Type information is stored only on fields and classes. If you define your
 * <code>List&lt;Integer&gt; myList = new ArrayList&lt;&gt;();</code> as local variable, no type
 * information is present, so this feature makes more sense on beans.
 *
 * <p><b>Note 2:</b> Conversion is limited to "common" datatypes, like String, Integer, Double or
 * Long
 *
 * <p>Searching the type can be a complex task (see
 * https://www.javacodegeeks.com/2013/12/advanced-java-generics-retreiving-generic-type-arguments.html)
 * You might have simple declarations like <code>List&lt;Integer&gt;</code> or
 * <li><code>ArrayList&lt;Integer&gt;</code>. But you may have generic classes that implements
 * <li><code>List&lt;E&gt;</code> and you have to figure out what type <code>E</code> is.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class JavaTypes {

    private static final Type[] NOT_FOUND = new Type[0];

    public static Type[] lookupType(
            Scriptable scope, Type type1, Type type2, Class<?> classOfInterest) {
        ClassCache cache = ClassCache.get(scope);
        Map<Type, Type[]> tc = cache.getTypeCacheMap();

        Type[] types1 = NOT_FOUND;
        Type[] types2 = NOT_FOUND;
        if (type1 != null) {
            types1 = tc.computeIfAbsent(type1, t -> lookupType(t, classOfInterest));
        }
        if (type2 != null) {
            types2 = tc.computeIfAbsent(type2, t -> lookupType(t, classOfInterest));
        }
        if (types1 == NOT_FOUND && types2 == NOT_FOUND) {
            return null;
        } else if (types1 == NOT_FOUND) {
            return types2;
        } else if (types2 == NOT_FOUND) {
            return types1;
        } else {
            Type[] ret = new Type[types1.length];
            for (int i = 0; i < types1.length; i++) {
                if (getRawType(types1[i]).isAssignableFrom(getRawType(types2[i]))) {
                    ret[i] = types2[i];
                } else {
                    ret[i] = types1[i];
                }
            }
            return ret;
        }
    }

    private static Type[] lookupType(Type type, Class<?> classOfInterest) {
        return lookupType(type, classOfInterest, null);
    }

    private static Type[] lookupType(
            Type type, Class<?> classOfInterest, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (type instanceof ParameterizedType) {
            return getParametrizedTypeArguments(
                    (ParameterizedType) type, classOfInterest, typeVarAssigns);
        } else if (type instanceof Class) {
            return getClassArguments((Class<?>) type, classOfInterest, typeVarAssigns);
        } else {
            return NOT_FOUND;
        }
    }

    private static Type[] getClassArguments(
            Class<?> clazz, Class<?> classOfInterest, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (!classOfInterest.isAssignableFrom(clazz)) {
            return NOT_FOUND;
        }
        if (typeVarAssigns == null) {
            typeVarAssigns = new HashMap<>();
        }
        if (classOfInterest.isInterface()) {
            for (Type interfaceType : clazz.getGenericInterfaces()) {
                Type[] ret = lookupType(interfaceType, classOfInterest, typeVarAssigns);
                if (ret != NOT_FOUND) {
                    return resolveTypes(ret, typeVarAssigns);
                }
            }
        }
        Type[] ret = lookupType(clazz.getGenericSuperclass(), classOfInterest, typeVarAssigns);
        if (ret == NOT_FOUND) {
            ret = new Type[classOfInterest.getTypeParameters().length];
            Arrays.fill(ret, Object.class);
        }
        return ret;
    }

    private static Type[] getParametrizedTypeArguments(
            ParameterizedType parameterizedType,
            Class<?> classOfInterest,
            Map<TypeVariable<?>, Type> typeVarAssigns) {
        Class clazz = (Class) parameterizedType.getRawType();
        if (clazz == classOfInterest) {
            // Type is List<String> and classOfInterest is List
            return resolveTypes(parameterizedType.getActualTypeArguments(), typeVarAssigns);
        }
        if (!classOfInterest.isAssignableFrom(clazz)) {
            return NOT_FOUND;
        }
        if (typeVarAssigns == null) {
            typeVarAssigns = new HashMap<>();
        }
        // get the subject parameterized type's arguments
        final Type[] typeArgs = parameterizedType.getActualTypeArguments();
        // and get the corresponding type variables from the raw class
        final TypeVariable<?>[] typeParams = clazz.getTypeParameters();

        // map the arguments to their respective type variables
        for (int i = 0; i < typeParams.length; i++) {
            final Type typeArg = typeArgs[i];
            typeVarAssigns.put(typeParams[i], typeVarAssigns.getOrDefault(typeArg, typeArg));
        }
        if (classOfInterest.isInterface()) {
            for (Type interfaceType : clazz.getGenericInterfaces()) {
                Type[] ret = lookupType(interfaceType, classOfInterest, typeVarAssigns);
                if (ret != NOT_FOUND) {
                    return resolveTypes(ret, typeVarAssigns);
                }
            }
        }
        return lookupType(clazz.getGenericSuperclass(), classOfInterest, typeVarAssigns);
    }

    /**
     * @param actualTypeArguments
     * @param typeVarAssigns
     * @return
     */
    private static Type[] resolveTypes(Type[] ret, Map<TypeVariable<?>, Type> typeVarAssigns) {
        for (int i = 0; i < ret.length; i++) {
            if (ret[i] instanceof TypeVariable) {
                ret[i] = typeVarAssigns.getOrDefault(ret[i], Object.class);
            } else if (ret[i] instanceof WildcardType) {
                WildcardType wildcard = (WildcardType) ret[i];
                Type[] bound = wildcard.getLowerBounds();
                if (bound.length > 0) {
                    ret[i] = bound[0];
                } else {
                    bound = wildcard.getUpperBounds();
                    if (bound.length > 0) {
                        ret[i] = bound[0];
                    } else {
                        ret[i] = Object.class;
                    }
                }
            }
        }
        return ret;
    }

    /** returns the raw type. Taken from google guice. */
    public static Class<?> getRawType(Type type) {
        if (type == null) {
            return null;

        } else if (type instanceof Class<?>) {
            // Type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class<?>) parameterizedType.getRawType();

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable || type instanceof WildcardType) {
            // We could use the variable's bounds, but that won't work if there
            // are multiple. Having a raw type that's more general than
            // necessary is okay.
            return Object.class;

        } else {
            String className = type.getClass().getName();
            throw new IllegalArgumentException(
                    "Expected a Class, "
                            + "ParameterizedType, or GenericArrayType, but <"
                            + type
                            + "> is of type "
                            + className);
        }
    }
}
