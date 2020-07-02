package org.mozilla.javascript.tests;

import org.junit.runners.Parameterized;
import org.junit.runners.model.RunnerScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Created by ian on 2020/7/3.
 *
 * @author ian
 * @since 2020/07/03 00:45
 */
public final class ParallelParameterized extends Parameterized {
    /**
     * Only called reflectively. Do not use programmatically.
     *
     * @param klass
     */
    public ParallelParameterized(Class<?> klass) throws Throwable {
        super(klass);
        setScheduler(parallel());
    }

    private static RunnerScheduler parallel() {
        return new RunnerScheduler() {
            private final ExecutorService fService = ForkJoinPool.commonPool();

            @Override
            public void schedule(Runnable childStatement) {
                fService.submit(childStatement);
            }

            @Override
            public void finished() {
                try {
                    fService.shutdown();
                    fService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        };
    }
}
