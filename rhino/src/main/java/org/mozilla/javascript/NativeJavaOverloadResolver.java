/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mozilla.javascript.lc.ReflectUtils;
import org.mozilla.javascript.lc.member.ExecutableBox;
import org.mozilla.javascript.lc.type.ParameterizedTypeInfo;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;
import org.mozilla.javascript.lc.type.VariableTypeInfo;

/**
 * Overload resolution and caching logic for Java methods and constructors exposed to JavaScript.
 *
 * @see NativeJavaMethod (deprecated in favor of JSFunction + NativeJavaCode)
 */
public class NativeJavaOverloadResolver {

    private static final boolean DEBUG = false;

    private NativeJavaOverloadResolver() {}

    /**
     * Resolve the best matching method from {@code methods} for {@code args}, invoke it against the
     * appropriate Java object, and wrap the result for JavaScript. This is the shared invocation
     * path used by {@link NativeJavaCode} and the deprecated {@link NativeJavaMethod}.
     *
     * @param thisObj the JavaScript {@code this}; used both to locate the receiver Java object and,
     *     when it is a {@link NativeJavaObject} with a parameterized static type, to consolidate
     *     type variables
     * @param functionName used only for error messages
     */
    static Object invoke(
            Context cx,
            VarScope scope,
            Object thisObj,
            Object[] args,
            ExecutableBox[] methods,
            CopyOnWriteArrayList<ResolvedOverload> overloadCache,
            String functionName) {
        if (methods.length == 0) {
            throw new RuntimeException("No methods defined for call");
        }

        int index = findCachedFunction(cx, methods, args, overloadCache);
        if (index < 0) {
            Class<?> c = methods[0].asMethod().getDeclaringClass();
            String sig = c.getName() + '.' + functionName + '(' + scriptSignature(args) + ')';
            throw Context.reportRuntimeErrorById("msg.java.no_such_method", sig);
        }

        var meth = methods[index];

        Map<VariableTypeInfo, TypeInfo> mapping = Map.of();
        if (thisObj instanceof NativeJavaObject) {
            var staticType = ((NativeJavaObject) thisObj).staticType;
            if (staticType instanceof ParameterizedTypeInfo) {
                mapping =
                        ((ParameterizedTypeInfo) staticType)
                                .extractConsolidationMapping(TypeInfoFactory.get(scope));
            }
        }
        args = meth.wrapArgsInternal(args, mapping);

        Object javaObject;
        if (meth.isStatic()) {
            javaObject = null; // don't need an object
        } else {
            Scriptable o = (Scriptable) thisObj;
            Class<?> c = meth.getDeclaringClass();
            for (; ; ) {
                if (o == null) {
                    throw Context.reportRuntimeErrorById(
                            "msg.nonjava.method",
                            functionName,
                            ScriptRuntime.toString(thisObj),
                            c.getName());
                }
                if (o instanceof Wrapper) {
                    javaObject = ((Wrapper) o).unwrap();
                    if (c.isInstance(javaObject)) {
                        break;
                    }
                }
                o = o.getPrototype();
            }
        }

        var returnValue = meth.invoke(javaObject, args);
        var returnType = meth.getReturnType();

        if (returnType == TypeInfo.PRIMITIVE_VOID) {
            // skip result wrapping if we don't need result at all
            return Undefined.instance;
        }

        return cx.getWrapFactory().wrap(cx, scope, returnValue, returnType);
    }

    /**
     * Find the index of the correct function to call given the set of methods or constructors and
     * the arguments. If no function can be found to call, return -1.
     */
    public static int findFunction(Context cx, ExecutableBox[] methodsOrCtors, Object[] args) {
        if (methodsOrCtors.length == 0) {
            return -1;
        }
        if (methodsOrCtors.length == 1) {
            if (failFastConversionWeights(args, methodsOrCtors[0]) == null) {
                return -1;
            }
            if (DEBUG) printDebug("Found ", methodsOrCtors[0], args);
            return 0;
        }

        int firstBestFit = -1;
        int[] firstBestFitWeights = null;

        int[] extraBestFits = null;
        int[][] extraBestFitWeights = null;
        int extraBestFitsCount = 0;

        search:
        for (int i = 0; i < methodsOrCtors.length; i++) {
            ExecutableBox member = methodsOrCtors[i];

            final var weights = failFastConversionWeights(args, member);
            if (weights == null) {
                continue search;
            }

            if (firstBestFit < 0) {
                if (DEBUG) printDebug("Found first applicable ", member, args);
                firstBestFit = i;
                firstBestFitWeights = weights;
                continue search;
            }

            // Compare with all currently fit methods.
            // The loop starts from -1 denoting firstBestFit and proceed
            // until extraBestFitsCount to avoid extraBestFits allocation
            // in the most common case of no ambiguity
            int betterCount = 0; // number of times member was preferred over best fits
            int worseCount = 0; // number of times best fits were preferred over member
            for (int j = -1; j != extraBestFitsCount; ++j) {
                int bestFitIndex = j < 0 ? firstBestFit : extraBestFits[j];
                ExecutableBox bestFit = methodsOrCtors[bestFitIndex];
                int[] bestFitWeights = j < 0 ? firstBestFitWeights : extraBestFitWeights[j];
                if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)
                        && bestFit.isPublic() != member.isPublic()) {
                    // When FEATURE_ENHANCED_JAVA_ACCESS gives us access
                    // to non-public members, continue to prefer public methods in overloading
                    if (!bestFit.isPublic()) ++betterCount;
                    else ++worseCount;
                } else {
                    int preference =
                            preferSignature(args, member, weights, bestFit, bestFitWeights);
                    if (preference == PREFERENCE_AMBIGUOUS) {
                        break;
                    } else if (preference == PREFERENCE_FIRST_ARG) {
                        ++betterCount;
                    } else if (preference == PREFERENCE_SECOND_ARG) {
                        ++worseCount;
                    } else {
                        if (preference != PREFERENCE_EQUAL) Kit.codeBug();
                        // This should not happen in theory but on some JVMs, Class.getMethods will
                        // return all static methods of the class hierarchy, even if a derived
                        // class's parameters match exactly. We want to call the derived class's
                        // method.
                        if (bestFit.isStatic()
                                && bestFit.getDeclaringClass()
                                        .isAssignableFrom(member.getDeclaringClass())) {
                            if (DEBUG) printDebug("Substituting (overridden static)", member, args);
                            if (j == -1) {
                                firstBestFit = i;
                                firstBestFitWeights = weights;
                            } else {
                                extraBestFits[j] = i;
                                extraBestFitWeights[j] = weights;
                            }
                        } else {
                            if (DEBUG) printDebug("Ignoring same signature member ", member, args);
                        }
                        continue search;
                    }
                }
            }
            if (betterCount == 1 + extraBestFitsCount) {
                // member was preferred over all best fits
                if (DEBUG) printDebug("New first applicable ", member, args);
                firstBestFit = i;
                firstBestFitWeights = weights;
                extraBestFitsCount = 0;
            } else if (worseCount == 1 + extraBestFitsCount) {
                // all best fits were preferred over member, ignore it
                if (DEBUG) printDebug("Rejecting (all current bests better) ", member, args);
            } else {
                // some ambiguity was present, add member to best fit set
                if (DEBUG) printDebug("Added to best fit set ", member, args);
                if (extraBestFits == null) {
                    // Allocate maximum possible array
                    extraBestFits = new int[methodsOrCtors.length - 1];
                    extraBestFitWeights = new int[methodsOrCtors.length - 1][];
                }
                extraBestFits[extraBestFitsCount] = i;
                extraBestFitWeights[extraBestFitsCount] = weights;
                ++extraBestFitsCount;
            }
        }

        if (firstBestFit < 0) {
            // Nothing was found
            return -1;
        } else if (extraBestFitsCount == 0) {
            // single best fit
            return firstBestFit;
        }

        // report remaining ambiguity
        StringBuilder buf = new StringBuilder();
        for (int j = -1; j != extraBestFitsCount; ++j) {
            int bestFitIndex;
            if (j == -1) {
                bestFitIndex = firstBestFit;
            } else {
                bestFitIndex = extraBestFits[j];
            }
            buf.append("\n    ");
            buf.append(methodsOrCtors[bestFitIndex].toJavaDeclaration());
        }

        ExecutableBox firstFitMember = methodsOrCtors[firstBestFit];
        String memberName = firstFitMember.getName();
        String memberClass = firstFitMember.getDeclaringClass().getName();

        if (methodsOrCtors[0].isConstructor()) {
            throw Context.reportRuntimeErrorById(
                    "msg.constructor.ambiguous", memberName, "", buf.toString());
        }
        throw Context.reportRuntimeErrorById(
                "msg.method.ambiguous", memberClass, memberName, "", buf.toString());
    }

    /**
     * Find and cache the index of the correct function to call. Uses per-call-site caching via
     * ResolvedOverload to avoid repeated overload resolution for the same arg types.
     */
    public static int findCachedFunction(
            Context cx,
            ExecutableBox[] methods,
            Object[] args,
            CopyOnWriteArrayList<ResolvedOverload> overloadCache) {
        if (methods.length > 1) {
            for (ResolvedOverload ovl : overloadCache) {
                if (ovl.matches(args)) {
                    return ovl.index;
                }
            }
            int index = findFunction(cx, methods, args);
            // As a sanity measure, don't let the lookup cache grow longer
            // than twice the number of overloaded methods
            if (overloadCache.size() < methods.length * 2) {
                ResolvedOverload ovl = new ResolvedOverload(args, index);
                overloadCache.addIfAbsent(ovl);
            }
            return index;
        }
        return findFunction(cx, methods, args);
    }

    public static String scriptSignature(Object[] values) {
        StringBuilder sig = new StringBuilder();
        for (int i = 0; i != values.length; ++i) {
            Object value = values[i];

            String s;
            if (value == null) {
                s = "null";
            } else if (value instanceof Boolean) {
                s = "boolean";
            } else if (value instanceof String) {
                s = "string";
            } else if (value instanceof Number) {
                s = "number";
            } else if (value instanceof Scriptable) {
                if (value instanceof Undefined) {
                    s = "undefined";
                } else if (value instanceof Wrapper) {
                    Object wrapped = ((Wrapper) value).unwrap();
                    s = wrapped.getClass().getName();
                } else if (value instanceof Function) {
                    s = "function";
                } else {
                    s = "object";
                }
            } else {
                s = JavaMembers.javaSignature(value.getClass());
            }

            if (i != 0) {
                sig.append(',');
            }
            sig.append(s);
        }
        return sig.toString();
    }

    private static final int PREFERENCE_EQUAL = 0;
    private static final int PREFERENCE_FIRST_ARG = 1;
    private static final int PREFERENCE_SECOND_ARG = 2;
    private static final int PREFERENCE_AMBIGUOUS = 3;

    private static int preferSignature(
            Object[] args,
            ExecutableBox member1,
            int[] computedWeights1,
            ExecutableBox member2,
            int[] computedWeights2) {
        final var types1 = member1.getArgTypes();
        final var types2 = member2.getArgTypes();

        int totalPreference = 0;
        for (int j = 0; j < args.length; j++) {
            final var type1 =
                    member1.isVarArgs() && j >= types1.size()
                            ? types1.get(types1.size() - 1)
                            : types1.get(j);
            final var type2 =
                    member2.isVarArgs() && j >= types2.size()
                            ? types2.get(types2.size() - 1)
                            : types2.get(j);
            if (type1.asClass() == type2.asClass()) {
                continue;
            }
            final var arg = args[j];

            // Determine which of type1, type2 is easier to convert from arg.
            final var rank1 =
                    j < computedWeights1.length
                            ? computedWeights1[j]
                            : NativeJavaObject.getConversionWeight(arg, type1);
            final var rank2 =
                    j < computedWeights2.length
                            ? computedWeights2[j]
                            : NativeJavaObject.getConversionWeight(arg, type2);

            int preference;
            if (rank1 < rank2) {
                preference = PREFERENCE_FIRST_ARG;
            } else if (rank1 > rank2) {
                preference = PREFERENCE_SECOND_ARG;
            } else {
                // Equal ranks
                if (rank1 == NativeJavaObject.CONVERSION_NONTRIVIAL) {
                    if (type1.isAssignableFrom(type2)) {
                        preference = PREFERENCE_SECOND_ARG;
                    } else if (type2.isAssignableFrom(type1)) {
                        preference = PREFERENCE_FIRST_ARG;
                    } else {
                        preference = PREFERENCE_AMBIGUOUS;
                    }
                } else {
                    preference = PREFERENCE_AMBIGUOUS;
                }
            }

            totalPreference |= preference;

            if (totalPreference == PREFERENCE_AMBIGUOUS) {
                break;
            }
        }
        return totalPreference;
    }

    static int[] failFastConversionWeights(Object[] args, ExecutableBox member) {
        final var argTypes = member.getArgTypes();
        var typeLen = argTypes.size();
        if (member.isVarArgs()) {
            typeLen--;
            if (typeLen > args.length) {
                return null;
            }
        } else {
            if (typeLen != args.length) {
                return null;
            }
        }
        final var weights = new int[typeLen];
        for (int i = 0; i < typeLen; i++) {
            final var weight = NativeJavaObject.getConversionWeight(args[i], argTypes.get(i));
            if (weight >= NativeJavaObject.CONVERSION_NONE) {
                if (DEBUG) {
                    printDebug("Rejecting (args can't convert) ", member, args);
                }
                return null;
            }
            weights[i] = weight;
        }
        return weights;
    }

    private static void printDebug(String msg, ExecutableBox member, Object[] args) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append(" ----- ");
            sb.append(msg);
            sb.append(member.getDeclaringClass().getName());
            sb.append('.');
            if (member.isMethod()) {
                sb.append(member.getName());
            }
            sb.append(ReflectUtils.liveConnectSignature(member.getArgTypes()));
            sb.append(" for arguments (");
            sb.append(scriptSignature(args));
            sb.append(')');
            System.out.println(sb);
        }
    }
}
