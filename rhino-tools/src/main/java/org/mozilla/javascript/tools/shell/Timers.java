package org.mozilla.javascript.tools.shell;

import java.util.HashMap;
import java.util.PriorityQueue;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * This class supports the "setTimeout" and "clearTimeout" methods of semi-standard JavaScript. It
 * does it within a single thread by keeping track of a queue of timeout objects, and then it blocks
 * the thread. It's used solely within the Shell right now.
 */
public class Timers {
    private int lastId = 0;
    private final HashMap<Integer, Timeout> timers = new HashMap<>();
    private final PriorityQueue<Timeout> timerQueue = new PriorityQueue<>();

    /**
     * Initialize the "setTimeout" and "clearTimeout" functions on the specified scope.
     *
     * @param scope the scope where the functions should be defined
     */
    public void install(Scriptable scope) {
        LambdaFunction setTimeout =
                new LambdaFunction(
                        scope,
                        "setTimeout",
                        1,
                        (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                                setTimeout(args));
        ScriptableObject.defineProperty(scope, "setTimeout", setTimeout, ScriptableObject.DONTENUM);
        LambdaFunction clearTimeout =
                new LambdaFunction(
                        scope,
                        "clearTimeout",
                        1,
                        (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                                clearTimeout(args));
        ScriptableObject.defineProperty(
                scope, "clearTimeout", clearTimeout, ScriptableObject.DONTENUM);
    }

    /**
     * Execute all pending timers and microtasks, blocking the thread if we need to wait for any
     * timers to time out.
     *
     * @param cx The Context to use to execute microtasks and timer functions
     * @param scope the global scope
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    public void runAllTimers(Context cx, Scriptable scope) throws InterruptedException {
        boolean executed;
        do {
            cx.processMicrotasks();
            executed = executeNext(cx, scope);
        } while (executed);
        cx.processMicrotasks();
    }

    /**
     * Put up to one task on the context's "microtask queue." If the next task is not ready to run
     * for some time, then block the calling thread until the time is up.
     *
     * @param cx the context
     * @param scope the current scope
     * @return true if something was placed on the queue, and false if the queue is empty
     * @throws InterruptedException if the thread was interrupted
     */
    private boolean executeNext(Context cx, Scriptable scope) throws InterruptedException {
        Timeout t = timerQueue.peek();
        if (t == null) {
            return false;
        }
        long remaining = t.expiration - System.currentTimeMillis();
        if (remaining > 0) {
            Thread.sleep(remaining);
        }
        timerQueue.remove();
        timers.remove(t.id);
        cx.enqueueMicrotask(() -> t.func.call(cx, scope, scope, t.funcArgs));
        return true;
    }

    private Object setTimeout(Object[] args) {
        if (args.length == 0) {
            throw ScriptRuntime.typeError("Expected function parameter");
        }
        if (!(args[0] instanceof Function)) {
            throw ScriptRuntime.typeError("Expected first argument to be a function");
        }

        int id = ++lastId;
        Timeout t = new Timeout();
        t.id = id;
        t.func = (Function) args[0];
        int delay = 0;
        if (args.length > 1) {
            delay = ScriptRuntime.toInt32(args[1]);
        }
        t.expiration = System.currentTimeMillis() + delay;
        if (args.length > 2) {
            t.funcArgs = new Object[args.length - 2];
            System.arraycopy(args, 2, t.funcArgs, 0, t.funcArgs.length);
        }

        timers.put(id, t);
        timerQueue.add(t);
        return id;
    }

    private Object clearTimeout(Object[] args) {
        if (args.length == 0) {
            throw ScriptRuntime.typeError("Expected function parameter");
        }
        int id = ScriptRuntime.toInt32(args[0]);
        Timeout t = timers.remove(id);
        if (t != null) {
            timerQueue.remove(t);
        }
        return Undefined.instance;
    }

    /**
     * An object to go on the priority queue.
     *
     * <p>Note: this class has a natural ordering that is inconsistent with equals.
     */
    private static final class Timeout implements Comparable<Timeout> {
        int id;
        Function func;
        Object[] funcArgs = ScriptRuntime.emptyArgs;
        long expiration;

        @Override
        public int compareTo(Timeout o) {
            return Long.compare(expiration, o.expiration);
        }

        @Override
        public boolean equals(Object obj) {
            try {
                return expiration == ((Timeout) obj).expiration;
            } catch (ClassCastException cce) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            // This private class should never go in a HashMap.
            assert false;
            return (int) expiration;
        }
    }
}
