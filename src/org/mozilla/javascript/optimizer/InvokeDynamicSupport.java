package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class InvokeDynamicSupport {

    static class CachingCallSite extends MutableCallSite {
        final Lookup lookup;

        CachingCallSite(Lookup lookup, String name, MethodType type) {
            super(type);
            this.lookup = lookup;
        }
    }

    public static CallSite bootstrapProp0Call(MethodHandles.Lookup lookup,
                                              String name, MethodType type) {
        CachingCallSite callSite = new CachingCallSite(lookup, name, type);
        MethodHandle check = INITCALL.bindTo(callSite);
        check = check.asType(type);

        callSite.setTarget(check);
        return callSite;

    }

    public static boolean checkClass(Class<?> clazz, Object receiver) {
        return receiver instanceof NativeJavaObject &&
                ((NativeJavaObject)receiver).unwrap().getClass() == clazz;
    }

    public static Object unwrapObject(Object obj) {
        return ((Wrapper)obj).unwrap();
    }

    public static Object callProp0(CachingCallSite callSite, Object value,
                               String property, Context cx, Scriptable scope)
            throws Throwable {
        if (value.getClass() != NativeJavaObject.class) {
            callSite.setTarget(FALLBACK);
            return FALLBACK.invoke(value, property, cx, scope);
        }

        Object javaObject = ((NativeJavaObject)value).unwrap();
        Class<?> javaClass = javaObject.getClass();
        MethodHandle target, localTarget;
        target = callSite.lookup.unreflect(javaClass.getMethod(property));
        target = localTarget = target.asType(MethodType.genericMethodType(1));
        target = MethodHandles.filterArguments(target, 0, UNWRAP);
        target = MethodHandles.dropArguments(target, 1,
                String.class, Context.class, Scriptable.class);

        MethodHandle test = CHECK_CLASS.bindTo(javaClass);
        test = test.asType(MethodType.methodType(boolean.class, Object.class));

        MethodHandle guard = MethodHandles.guardWithTest(test, target, callSite.getTarget());
        callSite.setTarget(guard);

        return localTarget.invoke(javaObject);

    }

    private static final MethodHandle CHECK_CLASS;
    private static final MethodHandle INITCALL;
    private static final MethodHandle FALLBACK;
    private static final MethodHandle UNWRAP;
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            CHECK_CLASS = lookup.findStatic(InvokeDynamicSupport.class, "checkClass",
                    MethodType.methodType(boolean.class, Class.class, Object.class));
            INITCALL = lookup.findStatic(InvokeDynamicSupport.class, "callProp0",
                    MethodType.methodType(Object.class, CachingCallSite.class,
                            Object.class, String.class, Context.class,
                            Scriptable.class));
            FALLBACK = lookup.findStatic(OptRuntime.class, "callProp0",
                    MethodType.methodType(Object.class, Object.class,
                            String.class, Context.class, Scriptable.class));
            UNWRAP = lookup.findStatic(InvokeDynamicSupport.class, "unwrapObject",
                    MethodType.methodType(Object.class, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
