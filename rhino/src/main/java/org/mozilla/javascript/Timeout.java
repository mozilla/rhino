package org.mozilla.javascript;

import java.util.Arrays;

public class Timeout {
    private static final int MIN_DELAY = 1;

    public static void init(VarScope scope, long maxDelay) {
        LambdaFunction setTimeout =
                new LambdaFunction(
                        scope,
                        "setTimeout",
                        1,
                        (cx1, scope1, thisObj1, args1) ->
                                setTimeout(cx1, scope1, thisObj1, args1, maxDelay));
        LambdaFunction clearTimeout =
                new LambdaFunction(
                        scope,
                        "clearTimeout",
                        1,
                        (Context cx, VarScope scope2, Scriptable thisObj, Object[] args) ->
                                clearTimeout(cx, args));

        LambdaFunction setImmediate =
                new LambdaFunction(
                        scope,
                        "setImmediate",
                        1,
                        (cx1, scope1, thisObj1, args1) ->
                                setImmediate(cx1, scope1, thisObj1, args1));

        LambdaFunction clearImmediate =
                new LambdaFunction(
                        scope,
                        "clearImmediate",
                        1,
                        (Context cx, VarScope scope2, Scriptable thisObj, Object[] args) ->
                                clearTimeout(cx, args));

        ScriptableObject.defineProperty(scope, "setTimeout", setTimeout, ScriptableObject.DONTENUM);

        ScriptableObject.defineProperty(
                scope, "clearTimeout", clearTimeout, ScriptableObject.DONTENUM);

        ScriptableObject.defineProperty(
                scope, "setImmediate", setImmediate, ScriptableObject.DONTENUM);

        ScriptableObject.defineProperty(
                scope, "clearImmediate", clearImmediate, ScriptableObject.DONTENUM);
    }

    private static Object clearTimeout(Context cx, Object[] args) {
        if (args.length == 0) {
            return Undefined.instance; // node just returns Undefined
        }
        int taskId = ScriptRuntime.toInt32(args[0]);
        cx.getTaskQueue().dequeueTimerTask(taskId);
        return Undefined.instance;
    }

    private static Object setImmediate(
            Context cx, VarScope scope, Scriptable thisObj, Object[] args) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.callback.not.function");
        }
        Function callback = (Function) args[0];
        var callbackArgs =
                args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new Object[] {};
        return cx.getTaskQueue()
                .enqueueTimerTask(() -> callback.call(cx, scope, thisObj, callbackArgs), -1);
    }

    private static Object setTimeout(
            Context cx, VarScope scope, Scriptable thisObj, Object[] args, long maxDelay) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.callback.not.function");
        }
        Function callback = (Function) args[0];

        int delay;
        if (args.length > 1) {
            delay = ScriptRuntime.toInt32(args[1]);
        } else {
            delay = 0;
        }
        if (delay > maxDelay) {
            throw ScriptRuntime.typeError(
                    "setTimeout delay must be less than or equal to the maximum delay of "
                            + maxDelay);
        }

        delay = Math.max(delay, MIN_DELAY);
        var callbackArgs =
                args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new Object[] {};
        return cx.getTaskQueue()
                .enqueueTimerTask(() -> callback.call(cx, scope, thisObj, callbackArgs), delay);
    }
}
