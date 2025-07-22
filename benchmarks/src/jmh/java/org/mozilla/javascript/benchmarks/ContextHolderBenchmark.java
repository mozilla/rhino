package org.mozilla.javascript.benchmarks;

import java.util.concurrent.TimeUnit;
import org.mozilla.javascript.Context;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Benchmark to test various strategies how to store the current context in a ThreadLocal.
 *
 * <pre>
 * Benchmark                              Mode  Cnt    Score    Error  Units
 * ContextHolderBenchmark.testDirect1     avgt    5   26,971 ±  0,391  ns/op
 * ContextHolderBenchmark.testDirect5     avgt    5   27,349 ±  1,534  ns/op
 * ContextHolderBenchmark.testDirect50    avgt    5  117,430 ±  6,230  ns/op
 * ContextHolderBenchmark.testIndirect1   avgt    5   16,955 ±  0,727  ns/op
 * ContextHolderBenchmark.testIndirect5   avgt    5   16,819 ±  2,035  ns/op
 * ContextHolderBenchmark.testIndirect50  avgt    5  143,583 ±  2,122  ns/op
 * ContextHolderBenchmark.testNoClear1    avgt    5   11,286 ±  1,369  ns/op
 * ContextHolderBenchmark.testNoClear5    avgt    5   12,202 ±  0,605  ns/op
 * ContextHolderBenchmark.testNoClear50   avgt    5   98,571 ±  9,092  ns/op
 * </pre>
 */
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ContextHolderBenchmark {
    static final Context someContext = Context.enter();

    public abstract static class AbstractContextHolder {

        abstract void setContext(Context context);

        abstract Context getContext();

        abstract void clearContext();
    }

    /** ContextHolder that accesses the threadLocal direct (get/set/remove) */
    @State(Scope.Thread)
    public static class ContextHolderDirect extends AbstractContextHolder {
        private static final ThreadLocal<Context> contextLocal = new ThreadLocal<>();

        void setContext(Context context) {
            contextLocal.set(context);
        }

        Context getContext() {
            return contextLocal.get();
        }

        void clearContext() {
            contextLocal.remove();
        }
    }

    /**
     * ContextHolder that accesses the threadLocal indirect. It holds an Object[] array as holder,
     * so that we need only read access. On the other side, we need a array-access on each
     * getContext invocation.
     */
    @State(Scope.Thread)
    public static class ContextHolderIndirect extends AbstractContextHolder {
        private static final ThreadLocal<Object[]> contextLocal =
                ThreadLocal.withInitial(() -> new Object[1]);

        void setContext(Context context) {
            contextLocal.get()[0] = context;
        }

        Context getContext() {
            return (Context) contextLocal.get()[0];
        }

        void clearContext() {
            contextLocal.get()[0] = null;
        }
    }

    /**
     * Direct access, but do not use context.remove. This give a massive performance benefit, when
     * the same thread sets the context again.
     */
    @State(Scope.Thread)
    public static class ContextHolderNoClear extends AbstractContextHolder {
        private static final ThreadLocal<Context> contextLocal = new ThreadLocal<>();

        void setContext(Context context) {
            contextLocal.set(context);
        }

        Context getContext() {
            return contextLocal.get();
        }

        void clearContext() {
            contextLocal.set(null);
        }
    }

    @Benchmark
    public Object testDirect1(ContextHolderDirect holder) {
        return performTest1(holder);
    }

    @Benchmark
    public Object testIndirect1(ContextHolderIndirect holder) {
        return performTest1(holder);
    }

    @Benchmark
    public Object testNoClear1(ContextHolderNoClear holder) {
        return performTest1(holder);
    }

    @Benchmark
    public Object testDirect5(ContextHolderDirect holder) {
        return performTest5(holder);
    }

    @Benchmark
    public Object testIndirect5(ContextHolderIndirect holder) {
        return performTest5(holder);
    }

    @Benchmark
    public Object testNoClear5(ContextHolderNoClear holder) {
        return performTest5(holder);
    }

    @Benchmark
    public Object testDirect50(ContextHolderDirect holder) {
        return performTest50(holder);
    }

    @Benchmark
    public Object testIndirect50(ContextHolderIndirect holder) {
        return performTest50(holder);
    }

    @Benchmark
    public Object testNoClear50(ContextHolderNoClear holder) {
        return performTest50(holder);
    }

    private Object performTest1(AbstractContextHolder holder) {
        holder.setContext(someContext);
        try {
            return new Context[] {
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
            };
        } finally {
            holder.clearContext();
        }
    }

    private Object performTest5(AbstractContextHolder holder) {
        holder.setContext(someContext);
        try {
            return new Context[] {
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
            };
        } finally {
            holder.clearContext();
        }
    }

    private Object performTest50(AbstractContextHolder holder) {
        holder.setContext(someContext);
        try {
            return new Context[] {
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
                holder.getContext(),
            };
        } finally {
            holder.clearContext();
        }
    }
}
