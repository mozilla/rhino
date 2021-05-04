package org.mozilla.javascript.optimizer;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter.MHandle;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;

public class DynamicRuntime {
    public static final String ADD_OP = "add";

    public static final String BOOTSTRAP_SIGNATURE =
            "(Ljava/lang/invoke/MethodHandles$Lookup;"
                    + "Ljava/lang/String;"
                    + "Ljava/lang/invoke/MethodType;"
                    + ")Ljava/lang/invoke/CallSite;";
    public static final String METHOD_SIGNATURE_OOC =
            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;";

    public static final MHandle BOOTSTRAP_HANDLE =
            new MHandle(
                    ByteCode.MH_INVOKESTATIC,
                    "org.mozilla.javascript.optimizer.DynamicRuntime",
                    "bootstrap",
                    DynamicRuntime.BOOTSTRAP_SIGNATURE);

    /** Perform an add assuming both arguments are Integer objects and fail otherwise. */
    public static Object addIntegers(Object o1, Object o2, Context cx) {
        try {
            return (Integer) o1 + (Integer) o2;
        } catch (ClassCastException | NullPointerException e) {
            throw new FallbackException();
        }
    }

    /** Perform an add assuming both arguments are Double objects and fail otherwise. */
    public static Object addFloats(Object o1, Object o2, Context cx) {
        try {
            return (Double) o1 + (Double) o2;
        } catch (ClassCastException | NullPointerException e) {
            throw new FallbackException();
        }
    }

    /** Perform an add assuming both arguments are CharSequence objects and fail otherwise. */
    public static Object addCharSequences(Object o1, Object o2, Context cx) {
        try {
            return new ConsString((CharSequence) o1, (CharSequence) o2);
        } catch (ClassCastException | NullPointerException e) {
            throw new FallbackException();
        }
    }

    /**
     * This will be called by the call site when one method fails -- it is expected to invoke the
     * next one in the chain. A separate method is required for each call signature that we support.
     */
    public static Object fallbackOOC(
            FallbackException fe,
            Object arg1,
            Object arg2,
            Context cx,
            FallbackCallSite site,
            int nonce)
            throws Throwable {
        MethodHandle fallbackHandle = site.fallBack(nonce);
        return fallbackHandle.invokeExact(arg1, arg2, cx);
    }

    /**
     * This is the function that is the target of the actual "invokedynamic" instruction in the
     * bytecode. The "name" parameter selects the actual function to set up.
     */
    @SuppressWarnings("unused")
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType mType)
            throws NoSuchMethodException, IllegalAccessException {
        switch (name) {
            case ADD_OP:
                // Implement "add" by trying fast paths for Integer, Double, and CharSequences,
                // before finally falling back to regular old ScriptRuntime.
                return new FallbackCallSite(
                        lookup.findStatic(
                                DynamicRuntime.class,
                                "fallbackOOC",
                                FallbackCallSite.makeFallbackCallType(mType)),
                        lookup.findStatic(DynamicRuntime.class, "addIntegers", mType),
                        lookup.findStatic(DynamicRuntime.class, "addFloats", mType),
                        lookup.findStatic(DynamicRuntime.class, "addCharSequences", mType),
                        lookup.findStatic(ScriptRuntime.class, "add", mType));
            default:
                throw Kit.codeBug("Invalid bootstrap op " + name);
        }
    }
}
