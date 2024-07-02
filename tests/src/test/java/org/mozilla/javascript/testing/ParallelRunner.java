package org.mozilla.javascript.testing;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.RunnerScheduler;

/**
 * This class extends the standard Junit 4 "parameterized" test runner to run individual test cases
 * in parallel threads, using a fixed-size thread pool. It should be a drop-in replacement. However,
 * test runners must be thread-safe in order for this to work.
 */
public class ParallelRunner extends Parameterized {
    enum Event {
        START,
        FINISH,
        SUITESTART,
        SUITEFINISH,
        FAIL,
        ASSUMPTIONFAIL,
        IGNORE,
        DONE
    };

    public ParallelRunner(Class<?> testClass) throws Throwable {
        super(testClass);
    }

    @Override
    public void run(RunNotifier notifier) {
        // Use 1/2 the number of available CPUs -- this lets other work get done,
        // and at the moment we have two big test suites that we don't want to
        // compete.
        int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        LinkedBlockingQueue<TestEvent> events = new LinkedBlockingQueue<>();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);

        // Replace the scheduler in the test runner with one that allows us to
        // control execution. This feature is marked "experimental" in JUnit 4
        // but it's been around a while so we'll use it.
        setScheduler(
                new RunnerScheduler() {
                    @Override
                    public void schedule(Runnable child) {
                        // Send every request to the thread pool
                        threadPool.execute(child);
                    }

                    @Override
                    public void finished() {
                        // JUnit is finished starting tests, but they might not be
                        // done. Wait for them all to finish.
                        threadPool.shutdown();
                        try {
                            threadPool.awaitTermination(2, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                            // Ignore interruption while waiting
                        }
                        events.add(new TestEvent(Event.DONE));
                    }
                });

        // Run the tests of the superclass in a separate thread, so that the main
        // thread can report results as if the tests actually ran in
        // the regular thread.
        new Thread(
                        () -> {
                            // Pass a different notifier that will allow us to pass
                            // test results between threads safely.
                            RunNotifier localNotifier = new RunNotifier();
                            localNotifier.addListener(new Listener(events));
                            super.run(localNotifier);
                        })
                .start();

        // Poll the stream of events coming from our test notifier and report them
        // to the caller as if they happened all in one thread.
        for (; ; ) {
            TestEvent e;
            try {
                e = events.take();
            } catch (InterruptedException ie) {
                throw new AssertionError("Didn't expect an interrupt");
            }
            if (e.event == Event.DONE) {
                break;
            }
            switch (e.event) {
                case START:
                    notifier.fireTestStarted(e.description);
                    break;
                case FINISH:
                    notifier.fireTestFinished(e.description);
                    break;
                case SUITESTART:
                    notifier.fireTestSuiteStarted(e.description);
                    break;
                case SUITEFINISH:
                    notifier.fireTestSuiteFinished(e.description);
                    break;
                case FAIL:
                    notifier.fireTestFailure(e.failure);
                    break;
                case IGNORE:
                    notifier.fireTestIgnored(e.description);
                    break;
                case ASSUMPTIONFAIL:
                    notifier.fireTestAssumptionFailed(e.failure);
                    break;
                case DONE:
                    throw new AssertionError();
            }
        }
    }

    private static final class TestEvent {
        Event event;
        Description description;
        Failure failure;

        TestEvent(Event e) {
            this.event = e;
        }
    }

    /**
     * This class listens for test events from the notifier attached to a test case and puts them in
     * a concurrent queue for processing by the thread that spawned the tests.
     */
    private static final class Listener extends RunListener {
        private final BlockingQueue<TestEvent> eventQueue;

        Listener(BlockingQueue<TestEvent> q) {
            this.eventQueue = q;
        }

        @Override
        public void testStarted(Description d) {
            TestEvent e = new TestEvent(Event.START);
            e.description = d;
            eventQueue.add(e);
        }

        @Override
        public void testFinished(Description d) {
            TestEvent e = new TestEvent(Event.FINISH);
            e.description = d;
            eventQueue.add(e);
        }

        @Override
        public void testIgnored(Description d) {
            TestEvent e = new TestEvent(Event.IGNORE);
            e.description = d;
            eventQueue.add(e);
        }

        @Override
        public void testFailure(Failure f) {
            TestEvent e = new TestEvent(Event.FAIL);
            e.failure = f;
            eventQueue.add(e);
        }

        @Override
        public void testAssumptionFailure(Failure f) {
            TestEvent e = new TestEvent(Event.ASSUMPTIONFAIL);
            e.failure = f;
            eventQueue.add(e);
        }

        @Override
        public void testSuiteStarted(Description d) {
            TestEvent e = new TestEvent(Event.SUITESTART);
            e.description = d;
            eventQueue.add(e);
        }

        @Override
        public void testSuiteFinished(Description d) {
            TestEvent e = new TestEvent(Event.SUITEFINISH);
            e.description = d;
            eventQueue.add(e);
        }
    }
}
