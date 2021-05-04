package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import org.mozilla.javascript.Kit;

/*
 * This class extends MutableCallSite to support a CallSite that goes through a list of candidate
 * method handles. It always delegates to the next handle in the chain when a FallbackException
 * is thrown. After a certain number of fallbacks, it pops a method from the chain and skips the failure.
 *
 * The idea of this class is to optimize call sites where prerequisites can be discarded. Generally
 * a method will make assumptions about whether it can operate at the particular site and throw
 * FallbackException if it can't.
 *
 * By eventually taking methods out of the chain when they have failed too many times, we can
 * relatively quickly discover the most efficient method for a particular call site, or fail
 * over to a generic method if, in the end, none will work.
 */
public class FallbackCallSite extends MutableCallSite {
    private static final int MAX_FAILURES = 10;

    private final MethodHandle fallbackHandle;
    private final MethodHandle[] handles;
    private int index = 0;
    private int failures = 0;

    /**
     * This is a convenience method to turn the method signature of a regular call to one for the
     * "fallbackHandle" argument to FallbackCallSite. By adding a FallbackException as the first
     * parameter and FallbackCallSite and integer as the last. This makes it easier to construct the
     * list of handles for the constructor.
     */
    public static MethodType makeFallbackCallType(MethodType mType) {
        return mType.insertParameterTypes(0, FallbackException.class)
                .appendParameterTypes(FallbackCallSite.class)
                .appendParameterTypes(Integer.TYPE);
    }

    /**
     * Create a new CallSite that will invoke each of the "targetHandles" in order. Any handle but
     * the last may throw FallbackException to cause a fallback invocation of the next handle in the
     * chain.
     *
     * <p>"fallbackHandle" must be a handle to a method with the same signature as all of the
     * "targetHandles," but which takes a FallbackException as the first parameter and
     * FallbackCallSite as the last. It must call "fallBack()" on this handle and use the returned
     * MethodHandle to invoke the fallback method.
     */
    public FallbackCallSite(MethodHandle fallbackHandle, MethodHandle... targetHandles) {
        super(targetHandles[0].type());
        this.fallbackHandle = fallbackHandle;
        this.handles = targetHandles;

        // Wrap all but the last handle with an exception handler for FallbackException
        // that will call the callback method.
        // The last handle in the chain will not catch this special exception.
        for (int i = 0; i < handles.length - 1; i++) {
            convertHandle(i);
        }
        setTarget(handles[0]);
    }

    private void convertHandle(int ix) {
        assert (ix < handles.length - 1);
        MethodHandle fallbackMethod =
                MethodHandles.insertArguments(
                        fallbackHandle, fallbackHandle.type().parameterCount() - 2, this, ix + 1);
        MethodHandle catchMethod =
                MethodHandles.catchException(handles[ix], FallbackException.class, fallbackMethod);
        handles[ix] = catchMethod;
    }

    /**
     * Callers must call this in the "fallback" method to determine which method to invoke next. It
     * returns a handle that can be used to invoke the next handle in the chain. Callers must call
     * that handle.
     *
     * <p>The "nonce" parameter is passed to the fallback method and must be passed on to this
     * method. It prevents us from prematurely falling back down the chain if there is an
     * inconsistency because this method is being used in a multi-threaded situation.
     *
     * @param nonce this must be the integer that was supplied to the fallback method.
     */
    public MethodHandle fallBack(int nonce) {
        failures++;
        if (index < handles.length - 1) {
            MethodHandle fallbackHandle = handles[nonce];
            if (failures >= MAX_FAILURES && index < nonce) {
                // Exceeded max failures, so change the target so that we now always
                // invoke the next target handle in the chain directly
                index = nonce;
                failures = 0;
                setTarget(fallbackHandle);
            }
            return fallbackHandle;
        }
        throw Kit.codeBug("Invalid fallback handle");
    }
}
