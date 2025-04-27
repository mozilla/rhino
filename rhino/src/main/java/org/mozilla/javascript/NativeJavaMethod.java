/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class reflects Java methods into the JavaScript environment and handles overloading of
 * methods.
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaPackage
 * @see NativeJavaClass
 */
public class NativeJavaMethod extends BaseFunction {

    private static final long serialVersionUID = -3440381785576412928L;

    NativeJavaMethod(MemberBox[] methods) {
        this.functionName = methods[0].getName();
        this.methods = methods;
    }

    NativeJavaMethod(MemberBox[] methods, String name) {
        this.functionName = name;
        this.methods = methods;
    }

    NativeJavaMethod(MemberBox method, String name) {
        this.functionName = name;
        this.methods = new MemberBox[] {method};
    }

    public NativeJavaMethod(Method method, String name) {
        this(new MemberBox(method), name);
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    static String scriptSignature(Object[] values) {
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

    @Override
    String decompile(int indent, EnumSet<DecompilerFlag> flags) {
        StringBuilder sb = new StringBuilder();
        boolean justbody = flags.contains(DecompilerFlag.ONLY_BODY);
        if (!justbody) {
            sb.append("function ");
            sb.append(getFunctionName());
            sb.append("() {");
        }
        sb.append("/*\n");
        sb.append(toString());
        sb.append(justbody ? "*/\n" : "*/}\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, N = methods.length; i != N; ++i) {
            // Check member type, we also use this for overloaded constructors
            if (methods[i].isMethod()) {
                Method method = methods[i].method();
                sb.append(JavaMembers.javaSignature(method.getReturnType()));
                sb.append(' ');
                sb.append(method.getName());
            } else {
                sb.append(methods[i].getName());
            }
            sb.append(JavaMembers.liveConnectSignature(methods[i].argTypes));
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        // Find a method that matches the types given.
        if (methods.length == 0) {
            throw new RuntimeException("No methods defined for call");
        }

        int index = findCachedFunction(cx, args);
        if (index < 0) {
            Class<?> c = methods[0].method().getDeclaringClass();
            String sig = c.getName() + '.' + getFunctionName() + '(' + scriptSignature(args) + ')';
            throw Context.reportRuntimeErrorById("msg.java.no_such_method", sig);
        }

        MemberBox meth = methods[index];
        Class<?>[] argTypes = meth.argTypes;

        if (meth.vararg) {
            // marshall the explicit parameters
            Object[] newArgs = new Object[argTypes.length];
            for (int i = 0; i < argTypes.length - 1; i++) {
                newArgs[i] = Context.jsToJava(args[i], argTypes[i]);
            }

            Object varArgs;

            // Handle special situation where a single variable parameter
            // is given and it is a Java or ECMA array or is null.
            if (args.length == argTypes.length
                    && (args[args.length - 1] == null
                            || args[args.length - 1] instanceof NativeArray
                            || args[args.length - 1] instanceof NativeJavaArray)) {
                // convert the ECMA array into a native array
                varArgs = Context.jsToJava(args[args.length - 1], argTypes[argTypes.length - 1]);
            } else {
                // marshall the variable parameters
                Class<?> componentType = argTypes[argTypes.length - 1].getComponentType();
                varArgs = Array.newInstance(componentType, args.length - argTypes.length + 1);
                for (int i = 0; i < Array.getLength(varArgs); i++) {
                    Object value = Context.jsToJava(args[argTypes.length - 1 + i], componentType);
                    Array.set(varArgs, i, value);
                }
            }

            // add varargs
            newArgs[argTypes.length - 1] = varArgs;
            // replace the original args with the new one
            args = newArgs;
        } else {
            // First, we marshall the args.
            Object[] origArgs = args;
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Object coerced = Context.jsToJava(arg, argTypes[i]);
                if (coerced != arg) {
                    if (origArgs == args) {
                        args = args.clone();
                    }
                    args[i] = coerced;
                }
            }
        }
        Object javaObject;
        if (meth.isStatic()) {
            javaObject = null; // don't need an object
        } else {
            Scriptable o = thisObj;
            Class<?> c = meth.getDeclaringClass();
            for (; ; ) {
                if (o == null) {
                    throw Context.reportRuntimeErrorById(
                            "msg.nonjava.method",
                            getFunctionName(),
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
        if (debug) {
            printDebug("Calling ", meth, args);
        }

        Object retval = meth.invoke(javaObject, args);
        Class<?> staticType = meth.method().getReturnType();

        if (debug) {
            Class<?> actualType = (retval == null) ? null : retval.getClass();
            System.err.println(
                    " ----- Returned "
                            + retval
                            + " actual = "
                            + actualType
                            + " expect = "
                            + staticType);
        }

        Object wrapped =
                cx.getWrapFactory()
                        .wrap(
                                cx, scope,
                                retval, staticType);
        if (debug) {
            Class<?> actualType = (wrapped == null) ? null : wrapped.getClass();
            System.err.println(" ----- Wrapped as " + wrapped + " class = " + actualType);
        }

        if (wrapped == null && staticType == Void.TYPE) {
            wrapped = Undefined.instance;
        }
        return wrapped;
    }

    int findCachedFunction(Context cx, Object[] args) {
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

    /**
     * Find the index of the correct function to call given the set of methods or constructors and
     * the arguments. If no function can be found to call, return -1.
     */
    static int findFunction(Context cx, MemberBox[] methodsOrCtors, Object[] args) {
        if (methodsOrCtors.length == 0) {
            return -1;
        }
        if (methodsOrCtors.length == 1) {
            if (failFastConversionWeights(args, methodsOrCtors[0]) == null) {
                return -1;
            }
            if (debug) printDebug("Found ", methodsOrCtors[0], args);
            return 0;
        }

        int firstBestFit = -1;
        int[] firstBestFitWeights = null;

        int[] extraBestFits = null;
        int[][] extraBestFitWeights = null;
        int extraBestFitsCount = 0;

        search:
        for (int i = 0; i < methodsOrCtors.length; i++) {
            MemberBox member = methodsOrCtors[i];

            final var weights = failFastConversionWeights(args, member);
            if (weights == null) {
                continue search;
            }

            if (firstBestFit < 0) {
                if (debug) printDebug("Found first applicable ", member, args);
                firstBestFit = i;
                firstBestFitWeights = weights;
                continue search;
            }

            // Compare with all currently fit methods.
            // The loop starts from -1 denoting firstBestFit and proceed
            // until extraBestFitsCount to avoid extraBestFits allocation
            // in the most common case of no ambiguity
            int betterCount = 0; // number of times member was preferred over
            // best fits
            int worseCount = 0; // number of times best fits were preferred
            // over member
            for (int j = -1; j != extraBestFitsCount; ++j) {
                int bestFitIndex = j < 0 ? firstBestFit : extraBestFits[j];
                MemberBox bestFit = methodsOrCtors[bestFitIndex];
                int[] bestFitWeights = j < 0 ? firstBestFitWeights : extraBestFitWeights[j];
                if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)
                        && bestFit.isPublic() != member.isPublic()) {
                    // When FEATURE_ENHANCED_JAVA_ACCESS gives us access
                    // to non-public members, continue to prefer public
                    // methods in overloading
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
                        // This should not happen in theory
                        // but on some JVMs, Class.getMethods will return all
                        // static methods of the class hierarchy, even if
                        // a derived class's parameters match exactly.
                        // We want to call the derived class's method.
                        if (bestFit.isStatic()
                                && bestFit.getDeclaringClass()
                                        .isAssignableFrom(member.getDeclaringClass())) {
                            // On some JVMs, Class.getMethods will return all
                            // static methods of the class hierarchy, even if
                            // a derived class's parameters match exactly.
                            // We want to call the derived class's method.
                            if (debug) printDebug("Substituting (overridden static)", member, args);
                            if (j == -1) {
                                firstBestFit = i;
                                firstBestFitWeights = weights;
                            } else {
                                extraBestFits[j] = i;
                                extraBestFitWeights[j] = weights;
                            }
                        } else {
                            if (debug) printDebug("Ignoring same signature member ", member, args);
                        }
                        continue search;
                    }
                }
            }
            if (betterCount == 1 + extraBestFitsCount) {
                // member was preferred over all best fits
                if (debug) printDebug("New first applicable ", member, args);
                firstBestFit = i;
                firstBestFitWeights = weights;
                extraBestFitsCount = 0;
            } else if (worseCount == 1 + extraBestFitsCount) {
                // all best fits were preferred over member, ignore it
                if (debug) printDebug("Rejecting (all current bests better) ", member, args);
            } else {
                // some ambiguity was present, add member to best fit set
                if (debug) printDebug("Added to best fit set ", member, args);
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

        MemberBox firstFitMember = methodsOrCtors[firstBestFit];
        String memberName = firstFitMember.getName();
        String memberClass = firstFitMember.getDeclaringClass().getName();

        if (methodsOrCtors[0].isCtor()) {
            throw Context.reportRuntimeErrorById(
                    "msg.constructor.ambiguous", memberName, scriptSignature(args), buf.toString());
        }
        throw Context.reportRuntimeErrorById(
                "msg.method.ambiguous",
                memberClass,
                memberName,
                scriptSignature(args),
                buf.toString());
    }

    /** Types are equal */
    private static final int PREFERENCE_EQUAL = 0;

    private static final int PREFERENCE_FIRST_ARG = 1;
    private static final int PREFERENCE_SECOND_ARG = 2;

    /** No clear "easy" conversion */
    private static final int PREFERENCE_AMBIGUOUS = 3;

    /**
     * Determine which of two signatures is the closer fit. Returns one of {@link
     * #PREFERENCE_EQUAL}, {@link #PREFERENCE_FIRST_ARG}, {@link #PREFERENCE_SECOND_ARG}, or {@link
     * #PREFERENCE_AMBIGUOUS}.
     */
    private static int preferSignature(
            Object[] args,
            MemberBox member1,
            int[] computedWeights1,
            MemberBox member2,
            int[] computedWeights2) {
        final var types1 = member1.argTypes;
        final var types2 = member2.argTypes;

        int totalPreference = 0;
        for (int j = 0; j < args.length; j++) {
            final var type1 =
                    member1.vararg && j >= types1.length ? types1[types1.length - 1] : types1[j];
            final var type2 =
                    member2.vararg && j >= types2.length ? types2[types2.length - 1] : types2[j];
            if (type1 == type2) {
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

    /**
     * 1. {@code args} is too short for {@code member} calling -> return {@code null}
     *
     * <p>2. at least one arg cannot be converted -> return {@code null}
     *
     * <p>3. otherwise -> return an int array holding all computed conversion weights, whose length
     * will be {@code args.length} for non-vararg member or {@code args.length-1} for vararg member
     *
     * @see NativeJavaObject#getConversionWeight(Object, Class)
     * @see NativeJavaObject#canConvert(Object, Class)
     */
    static int[] failFastConversionWeights(Object[] args, MemberBox member) {
        final var argTypes = member.argTypes;
        var typeLen = argTypes.length;
        if (member.vararg) {
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
            final var weight = NativeJavaObject.getConversionWeight(args[i], argTypes[i]);
            if (weight >= NativeJavaObject.CONVERSION_NONE) {
                if (debug) {
                    printDebug("Rejecting (args can't convert) ", member, args);
                }
                return null;
            }
            weights[i] = weight;
        }
        return weights;
    }

    private static final boolean debug = false;

    private static void printDebug(String msg, MemberBox member, Object[] args) {
        if (debug) {
            StringBuilder sb = new StringBuilder();
            sb.append(" ----- ");
            sb.append(msg);
            sb.append(member.getDeclaringClass().getName());
            sb.append('.');
            if (member.isMethod()) {
                sb.append(member.getName());
            }
            sb.append(JavaMembers.liveConnectSignature(member.argTypes));
            sb.append(" for arguments (");
            sb.append(scriptSignature(args));
            sb.append(')');
            System.out.println(sb);
        }
    }

    MemberBox[] methods;
    private String functionName;
    private final transient CopyOnWriteArrayList<ResolvedOverload> overloadCache =
            new CopyOnWriteArrayList<>();
}

class ResolvedOverload {
    final Class<?>[] types;
    final int index;

    ResolvedOverload(Object[] args, int index) {
        this.index = index;
        types = new Class<?>[args.length];
        for (int i = 0, l = args.length; i < l; i++) {
            Object arg = args[i];
            if (arg instanceof Wrapper) arg = ((Wrapper) arg).unwrap();
            types[i] = arg == null ? null : arg.getClass();
        }
    }

    boolean matches(Object[] args) {
        if (args.length != types.length) {
            return false;
        }
        for (int i = 0, l = args.length; i < l; i++) {
            Object arg = args[i];
            if (arg instanceof Wrapper) arg = ((Wrapper) arg).unwrap();
            if (arg == null) {
                if (types[i] != null) return false;
            } else if (arg.getClass() != types[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ResolvedOverload)) {
            return false;
        }
        ResolvedOverload ovl = (ResolvedOverload) other;
        return Arrays.equals(types, ovl.types) && index == ovl.index;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(types);
    }
}
