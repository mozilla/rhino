/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

/**
 * Carries the reflection info per JavaObject.
 *
 * <p>For example, if we take <code>ArrayList</code>, we have the following inheritance
 *
 * <ul>
 *   <li><code>ArrayList&lt;E&gt;</code> extends <code>AbstractList&lt;E&gt;</code> implements
 *       <code>List&lt;E&gt;</code>
 *   <li><code>AbstractList&lt;E&gt;</code> extends <code>AbstractCollection&lt;E&gt;</code>
 *       implements <code>List&lt;E&gt;</code>
 *   <li><code>AbstractCollection&lt;E&gt;</code> implements <code>Collection&lt;E&gt;</code>
 *   <li><code>Collection&lt;E&gt;</code> extends <code>Iterable&lt;E&gt;</code>
 *   <li><code>Iterable&lt;T&gt;</code>
 * </ul>
 *
 * we have to walk trough the inheritance tree and build a graph, how the genericTypes (here <code>E
 * </code> and <code>T</code>) are linked together.
 *
 * <p>For the generic type declaration <code>ArrayList&gt;String&lt;</code> we can query which
 * resolved type arguments each superClass or superInterface has. In this example <code>
 * resolve(Iterator.class, 0)</code> will return <code>String.class</code>.
 *
 * <p>When determining method parameters (for example the generic parameter of <code>List.add(M)
 * </code> is <code>M</code>) {@link #resolve(Type)} and {@link #reverseResolve(Type)} are used to
 * determine the correct type. (in this case String.class)
 *
 * <p>Some ideas are taken from
 * https://www.javacodegeeks.com/2013/12/advanced-java-generics-retreiving-generic-type-arguments.html
 *
 * @author Roland Praml, FOCONIS AG
 */
public class JavaTypeInfo {

    private final Map<Class<?>, Type[]> typeCache;
    private final Map<Type, Type> resolved;
    private final Map<Type, Type> reverseResolved;

    JavaTypeInfo(Type type) {
        typeCache = new HashMap<>();
        resolved = new HashMap<>();
        reverseResolved = new HashMap<>();
        reflect(type, null);
    }

    /**
     * Returns the resolved type argument for <code>classOfInterest</code>.
     *
     * @param classOfInterest a superClass or interface of the representing <code>type</code>.
     * @param index the generic index of <code>classOfIntererest</code>
     */
    public Class<?> resolve(Class<?> classOfInterest, int index) {
        Type[] entry = typeCache.get(classOfInterest);
        if (entry == null) {
            return null;
        } else {
            return getRawType(entry[index]);
        }
    }

    public Type reverseResolve(Type type) {
        return reverseResolved.getOrDefault(type, type);
    }

    public Class<?> resolve(Type type) {
        Type ret = resolved.get(type);
        if (ret instanceof Class) {
            return (Class<?>) ret;
        } else {
            return null;
        }
    }

    private void reflect(Type type, Type[] typeArgs) {
        if (type == null) {
            return;
        } else if (type instanceof Class) {
            Class<?> cls = (Class<?>) type;
            TypeVariable<?>[] params = cls.getTypeParameters();
            if (params.length != 0) {
                Type[] resolvedParams = new Type[params.length];
                for (int i = 0; i < params.length; i++) {
                    if (typeArgs == null) {
                        resolvedParams[i] = params[i];
                    } else {
                        // build a map how generic type parameters are linked over various
                        // subclasses.
                        resolvedParams[i] = resolved.getOrDefault(typeArgs[i], typeArgs[i]);
                        resolved.put(params[i], resolvedParams[i]);
                        if (resolvedParams[i] instanceof TypeVariable) {
                            reverseResolved.put(resolvedParams[i], params[i]);
                        }
                    }
                }
                typeCache.put(cls, resolvedParams);
            }
            for (Type iface : cls.getGenericInterfaces()) {
                reflect(iface, null);
            }
            reflect(cls.getGenericSuperclass(), null);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            reflect(pt.getRawType(), pt.getActualTypeArguments());
        }
    }

    public static JavaTypeInfo get(Scriptable scope, Type type) {
        if (type == null) {
            return null;
        }
        ClassCache cache = ClassCache.get(scope);
        return cache.getTypeCacheMap().computeIfAbsent(type, JavaTypeInfo::new);
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

        } else if (type instanceof WildcardType) {
            Type[] bound = ((WildcardType) type).getLowerBounds();
            if (bound.length == 1) {
                return getRawType(bound[0]);
            } else {
                bound = ((WildcardType) type).getUpperBounds();
                if (bound.length == 1) {
                    return getRawType(bound[0]);
                } else {
                    return Object.class;
                }
            }
        } else if (type instanceof TypeVariable) {
            Type[] bound = ((TypeVariable<?>) type).getBounds();
            if (bound.length == 1) {
                return getRawType(bound[0]);
            } else {
                return Object.class;
            }

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
