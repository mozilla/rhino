package org.mozilla.javascript;

import java.util.Arrays;

public class Interval {

    public static void init(VarScope scope, long maxDelay) {
        LambdaFunction setInterval =
                new LambdaFunction(
                        scope,
                        "setInterval",
                        1,
                        (cx, scope2, thisObj, args) ->
                                Interval.setInterval(cx, scope, thisObj, args, maxDelay));

        LambdaFunction clearInterval =
                new LambdaFunction(
                        scope,
                        "clearInterval",
                        1,
                        (Context cx, VarScope scope2, Scriptable thisObj, Object[] args) ->
                                clearInterval(cx, args));

        ScriptableObject.defineProperty(
                scope, "setInterval", setInterval, ScriptableObject.DONTENUM);
        ScriptableObject.defineProperty(
                scope, "clearInterval", clearInterval, ScriptableObject.DONTENUM);
    }

    static Object setInterval(
            Context cx, VarScope scope, Scriptable thisObj, Object[] args, long maxDelay) {
        if (args.length < 1 || !(args[0] instanceof Function)) {
            throw ScriptRuntime.typeError("The \"callback\" argument must be of type function");
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
                    "setInterval delay must be less than or equal to the maximum delay of "
                            + maxDelay);
        }
        var callbackArgs =
                args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new Object[] {};
        return cx.getTaskQueue()
                .enqueueRepeatingTimerTask(
                        () -> callback.call(cx, scope, thisObj, callbackArgs), delay);
    }

    private static Object clearInterval(Context cx, Object[] args) {
        if (args.length == 0) {
            return Undefined.instance; // node just returns Undefined
        }
        int taskId = ScriptRuntime.toInt32(args[0]);
        cx.getTaskQueue().dequeueTimerTask(taskId);
        return Undefined.instance;
    }
}
