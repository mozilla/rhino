package org.mozilla.javascript;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class MicroTaskQueue {
    private static final Logger LOG = Logger.getLogger(Context.class.getName());

    private final Context context;
    private final ArrayDeque<Runnable> microtasks = new ArrayDeque<>();
    // TODO - potentailly wrap those in a class?
    private final DelayQueue<TimerTask> timerTasks = new DelayQueue<>();
    private final AtomicInteger nextTimerTaskId = new AtomicInteger();
    private final ArrayList<DeferredTask> deferredTasks = new ArrayList<>();
    private final UnhandledRejectionTracker unhandledPromises = new UnhandledRejectionTracker();
    private final BlockingQueue<DeferredTask> readyDeferredTasks = new LinkedBlockingQueue<>();

    private int defaultWaitTimeForEmptyQueueMs = 50;
    private int maxWaitTimeForNextTimerMs = 100;
    private int maxLoopsOfNoWork = 10000;
    private int microtaskSuspendCount;
    private Clock clock = Clock.systemUTC();

    MicroTaskQueue(Context context) {
        this.context = context;
    }

    public void setDefaultWaitTimeForEmptyQueueMs(int time) {
        defaultWaitTimeForEmptyQueueMs = time;
    }

    public int getMaxWaitTimeForNextTimerMs() {
        return maxWaitTimeForNextTimerMs;
    }

    public void setMaxWaitTimeForNextTimerMs(int time) {
        maxWaitTimeForNextTimerMs = time;
    }

    public int getMaxLoopsOfNoWork() {
        return maxLoopsOfNoWork;
    }

    public void setMaxLoopsOfNoWork(int loops) {
        maxLoopsOfNoWork = loops;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public Clock getClock() {
        return clock;
    }

    /**
     * Control whether to track unhandled promise rejections. If "track" is set to true, then the
     * tracker returned by "getUnhandledPromiseTracker" must be periodically used to process the
     * queue of unhandled promise rejections, or a memory leak may result.
     *
     * @param track if true, then track unhandled promise rejections
     */
    public void setTrackUnhandledPromiseRejections(boolean track) {
        unhandledPromises.enable(track);
    }

    /**
     * Return the object used to track unhandled promise rejections.
     *
     * @return the tracker object
     */
    public UnhandledRejectionTracker getUnhandledPromiseTracker() {
        return unhandledPromises;
    }

    /**
     * Returns the current time in millisecond since the unix epoch, as measured by the {@link
     * #clock}. Same idea as {@link System#currentTimeMillis()}, but we can change this in tests.
     */
    public long now() {
        return clock.millis();
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    public NativePromise promise(VarScope scope, CompletableFuture<?> future) {
        AtomicReference<LambdaFunction> resolve = new AtomicReference<>();
        AtomicReference<LambdaFunction> reject = new AtomicReference<>();
        //  TODO: Verify this will work in glide, there is a history of problems:  When using
        // Polyfills this is picked up from polyfillScope which is delegated from ESLatestParent
        var promiseObject =
                context.newObject(
                        scope,
                        "Promise",
                        new Callable[] {
                            (c, s, thisObj, arr) -> {
                                resolve.set((LambdaFunction) arr[0]);
                                reject.set((LambdaFunction) arr[1]);
                                return null;
                            }
                        });

        var promise = (NativePromise) promiseObject;
        DeferredTask deferredTask =
                new DeferredTask(clock, future, promise, resolve.get(), reject.get(), scope);
        deferredTasks.add(deferredTask);

        future.thenRun(
                        () -> {
                            readyDeferredTasks.add(deferredTask);
                        })
                .exceptionally(
                        (ex) -> {
                            readyDeferredTasks.add(deferredTask);

                            // Needed because 'exceptionally' takes a Callable, not a Runnable
                            return null;
                        });

        return promise;
    }

    /**
     * Returns the current time in nanoseconds since the unix epoch, as measured by the {@link
     * #clock}. Same idea as {@link System#nanoTime()}, but we can change this in tests.
     */
    public long nowNanoseconds() {
        Instant instant = clock.instant();
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }

    /**
     * Add a task that will be executed at the end of the current operation. The various "evaluate"
     * functions will all call this before exiting to ensure that all microtasks run to completion.
     * Otherwise, callers should call "processMirotasks" to run them all. This feature is primarily
     * used to implement Promises. The microtask queue is not thread-safe.
     */
    public void enqueueMicrotask(Runnable task) {

        microtasks.add(context.captureTaskState(task));
    }

    /**
     * Temporarily suspend microtask processing. Tasks will be resumed again when
     * resumeMicrotaskProcessing() is called. Suspensions are cumulative -- each call to this method
     * increases the suspend count, and microtasks will resume when the count again reaches zero.
     * This can be used in complex frameworks that wish to execute multiple individual scripts
     * before promises are resolved to emulate the behavior of some web browsers and other
     * frameworks. The suspend count is not thread safe.
     *
     * @see #resumeMicrotaskProcessing()
     */
    public void suspendMicrotaskProcessing() {
        microtaskSuspendCount++;
    }

    /**
     * Resume microtask processing after a suspend. Each call to suspendMicrotaskProcessing() must
     * be matched with a call to this function in order for processing to resume.
     *
     * @see #suspendMicrotaskProcessing()
     */
    public void resumeMicrotaskProcessing() {
        assert microtaskSuspendCount > 0;
        microtaskSuspendCount--;
        if (microtaskSuspendCount == 0) {
            processMicrotasksTracksWork();
        }
    }

    /**
     * Add a task to Timer queue to be executed after current operation, before the microtasks
     * queue.
     *
     * @return timer taskId
     */
    public int enqueueTimerTask(Runnable task, long delay) {
        int taskId = this.nextTimerTaskId.incrementAndGet();
        var t = TimerTask.of(clock, taskId, context.captureTaskState(task), delay);
        this.timerTasks.add(t);
        return taskId;
    }

    /**
     * Add a repeating task to Timer queue, that will be re-run forever, at the specific time
     * interval. As other TimeTasks in the queue, they are executed after current operation, before
     * the microtasks queue.
     *
     * @param interval - specific time interval in miliseconds between task repetitions
     * @return timer taskId
     */
    public int enqueueRepeatingTimerTask(Runnable task, long interval) {
        int taskId = this.nextTimerTaskId.incrementAndGet();
        var t = TimerTask.repeating(clock, taskId, context.captureTaskState(task), interval);
        this.timerTasks.add(t);
        return taskId;
    }

    /**
     * Removes a task from the Timer queue if matching one is found.
     *
     * @param taskId - id of the task to be removed, as returned by {@link
     *     Context#enqueueTimerTask(Runnable, long)} }
     * @return - true if task was removed
     */
    public boolean dequeueTimerTask(int taskId) {
        return this.timerTasks.remove(TimerTask.empty(clock, taskId));
    }

    private void runTimerTask(TimerTask task) {
        if (task.isRepeating()) {
            // if it's a repeating task, schedule next run before executing current
            var next = task.nextRepeat();
            this.timerTasks.add(next);
        }
        task.getTask().run();
    }

    private boolean processReadyTimerTasks() throws InterruptedException {
        var didWork = false;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            TimerTask head = timerTasks.poll();
            if (head == null) {
                break;
            }
            didWork = true;
            runTimerTask(head);
            processMicrotasksTracksWork();
        }
        return didWork;
    }

    /**
     * Run all the microtasks for the current context to completion. This is called by the various
     * "evaluate" functions. Frameworks that call Function objects directly should call this
     * function to ensure that everything completes if they want all Promises to eventually resolve.
     * This function is idempotent, but the microtask queue is not thread-safe.
     */
    public boolean processMicrotasksTracksWork() {
        boolean didWork = false;
        Runnable head;
        do {
            head = microtasks.poll();
            if (head != null) {
                didWork = true;
                head.run();
            }
        } while (head != null);
        return didWork;
    }

    public Object waitForOnePromise(NativePromise promise) {
        return waitForOnePromise(promise, Optional.empty());
    }

    public Object waitForOnePromise(NativePromise promise, Optional<Long> timeoutMs) {
        try {
            if (!promise.isPending()) return promise.getResult();

            var startingInstant = Instant.now(clock);
            var loopsWithoutWork = 0;

            while (promise.isPending()) {
                if (microtasks.isEmpty() && deferredTasks.isEmpty() && timerTasks.isEmpty()) {
                    // nothing in the queues to wait for, abort.
                    throw context.reportRuntimeError("Invalid operation - No tasks to wait on");
                }
                var didWork = singleEventLoop();

                if (!didWork) {
                    loopsWithoutWork++;
                } else {
                    loopsWithoutWork = 0;
                }
                if (timeoutMs.isPresent()
                        && Instant.now(clock)
                                .isAfter(startingInstant.plusMillis(timeoutMs.get()))) {
                    throwErrorForTimeout(timeoutMs.get());
                }

                if (loopsWithoutWork > maxLoopsOfNoWork) {
                    throwErrorForNoWorkDone(startingInstant, loopsWithoutWork);
                }
            }
            var result = promise.getResult();
            return result instanceof ConsString ? result.toString() : result;
        } catch (InterruptedException e) {
            shutdownTasks();
            throw context.reportRuntimeError("Interrupted while waiting for pending tasks");
        }
    }

    public boolean singleEventLoop() throws InterruptedException {
        var didWork = false;
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        didWork |= processReadyTimerTasks();
        didWork |= processMicrotasksTracksWork();
        didWork |= processDeferredTasks();

        return didWork;
    }

    public void processPendingTasks() {
        try {
            var loopsWithoutWork = 0;
            var startingInstant = Instant.now(clock);
            while (!microtasks.isEmpty() || !deferredTasks.isEmpty() || !timerTasks.isEmpty()) {
                var didWork = singleEventLoop();
                if (!didWork) {
                    loopsWithoutWork++;
                } else {
                    loopsWithoutWork = 0;
                }

                if (loopsWithoutWork > maxLoopsOfNoWork) {
                    throwErrorForNoWorkDone(startingInstant, loopsWithoutWork);
                }
            }
        } catch (InterruptedException e) {
            shutdownTasks();
            throw context.reportRuntimeError("Interrupted while waiting for pending tasks");
        }
    }

    private void clearQueues() {
        microtasks.clear();
        deferredTasks.clear();
        readyDeferredTasks.clear();
        timerTasks.clear();
    }

    private void throwErrorForTimeout(long timeoutMs) {
        var nextTimerDelay =
                timerTasks.peek() == null ? 0 : timerTasks.peek().getDelay(TimeUnit.MILLISECONDS);
        var formattedErrorMessage =
                String.format(
                        "Rhino aborted processing pending tasks because the timeout of %d ms was reached."
                                + "We had %d microtasks, %d deferred tasks, and %d timer tasks%n"
                                + (timerTasks.isEmpty()
                                        ? ""
                                        : "The next timer task is scheduled in %d ms"),
                        timeoutMs,
                        microtasks.size(),
                        deferredTasks.size(),
                        timerTasks.size(),
                        nextTimerDelay);
        shutdownTasks();
        throw context.reportRuntimeError(formattedErrorMessage);
    }

    private void throwErrorForNoWorkDone(Instant startingInstant, int loopsWithoutWork) {
        var elapsed = Instant.now(clock).toEpochMilli() - startingInstant.toEpochMilli();
        var nextTimerDelay =
                timerTasks.peek() == null ? 0 : timerTasks.peek().getDelay(TimeUnit.MILLISECONDS);
        var formattedErrorMessage =
                String.format(
                        "Rhino aborted after trying to process pending tasks for %d loops. Elapsed time: %d ms%n"
                                + "We had %d microtasks, %d deferred tasks, and %d timer tasks%n"
                                + (timerTasks.isEmpty()
                                        ? ""
                                        : "The next timer task is scheduled in %d ms"),
                        loopsWithoutWork,
                        elapsed,
                        microtasks.size(),
                        deferredTasks.size(),
                        timerTasks.size(),
                        nextTimerDelay);
        shutdownTasks();
        throw context.reportRuntimeError(formattedErrorMessage);
    }

    private boolean processDeferredTasks() throws InterruptedException {
        var didWork = false;
        // if we have deferred tasks ready
        // process all of them
        if (!readyDeferredTasks.isEmpty()) {
            return processAllDeferredTasksOnQueue();
        } else {
            // If we don't have any ready, we are going to wait to the next
            // timer. But we don't want to wait forever, so we put a max
            // time we wait for.
            // We also don't want to make this a crazy tight loop, so we have a min timer
            // If we got a task, we process it and go ahead and process any other ready ones
            // Then we will end up looping back for the timer.
            // see:
            // https://nodejs.org/en/learn/asynchronous-work/event-loop-timers-and-nexttick#timers
            var nextDelay = timerTasks.peek();
            var timeForNextTimer =
                    nextDelay != null
                            ? nextDelay.getDelay(TimeUnit.MILLISECONDS)
                            : defaultWaitTimeForEmptyQueueMs;
            var maxTimeout = Math.min(timeForNextTimer, maxWaitTimeForNextTimerMs);
            var readyTask = readyDeferredTasks.poll(maxTimeout, TimeUnit.MILLISECONDS);
            if (readyTask != null) {
                didWork = true;
                deferredTasks.remove(readyTask);
                enqueueMicrotaskFrom(readyTask);
                processMicrotasksTracksWork();
            }
            didWork |= processAllDeferredTasksOnQueue();
        }
        return didWork;
    }

    private boolean processAllDeferredTasksOnQueue() throws InterruptedException {
        var didWork = false;
        while (!readyDeferredTasks.isEmpty()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            DeferredTask readyTask = readyDeferredTasks.poll();
            if (readyTask != null) {
                didWork = true;
                deferredTasks.remove(readyTask);
                enqueueMicrotaskFrom(readyTask);
                processMicrotasksTracksWork();
            }
        }
        return didWork;
    }

    private void enqueueMicrotaskFrom(DeferredTask deferredTask) throws InterruptedException {
        try {
            Object result = deferredTask.future.get();

            enqueueMicrotask(
                    () -> {
                        deferredTask.resolve.call(
                                context,
                                deferredTask.scope,
                                deferredTask.promise,
                                new Object[] {result});
                    });
        } catch (Exception executionException) {
            enqueueMicrotask(
                    () -> {
                        EcmaError ecmaError = asEcmaError(executionException);
                        Scriptable nativeError =
                                ScriptRuntime.wrapException(ecmaError, deferredTask.scope, context);
                        deferredTask.reject.call(
                                context,
                                deferredTask.scope,
                                deferredTask.promise,
                                new Object[] {nativeError});
                    });
        }
    }

    private static EcmaError asEcmaError(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof EcmaError) {
            return (EcmaError) cause;
        } else {
            // From https://fetch.spec.whatwg.org/#fetch-method it seems that, at least for fetch,
            // a "TypeError" is a good option, but is this general?
            // Given our API, we only have a Java exception here, we can't reject with a given JS
            // object
            return ScriptRuntime.constructError("TypeError", getMessage(e, cause));
        }
    }

    // I miss Kotlin's ?. operator :(
    private static String getMessage(Exception e, Throwable cause) {
        String message = cause != null ? cause.getMessage() : e.getMessage();
        return message == null ? e.toString() : message;
    }

    private void shutdownTasks() {
        cancelPendingDeferredTasks();
        clearQueues();
    }

    private void cancelPendingDeferredTasks() {
        // Best effort!
        for (DeferredTask deferredTask : deferredTasks) {
            try {
                deferredTask.future.cancel(true);
            } catch (CancellationException e) {
                LOG.warning("Ignoring exception while cancelling a deferred task: " + e);
            }
        }
    }

    private static final class TimerTask implements Delayed {
        private final Clock clock;
        private final int id;
        private final Runnable task;
        private final long startTime;
        private final long originalDelay;
        private final boolean repeating;

        private TimerTask(Clock clock, int id, Runnable task, long delayMilis, boolean repeating) {
            this.clock = clock;
            this.id = id;
            this.task = task;
            this.originalDelay = delayMilis;
            this.startTime = clock.millis() + delayMilis;
            this.repeating = repeating;
        }

        public Runnable getTask() {
            return task;
        }

        public TimerTask nextRepeat() {
            return new TimerTask(clock, id, task, originalDelay, true);
        }

        /** Returns a new, non-repeating TimeoutTask */
        public static TimerTask of(Clock clock, int id, Runnable task, long delayMilis) {
            return new TimerTask(clock, id, task, delayMilis, false);
        }

        /** Returns a new, repeating TimeoutTask */
        public static TimerTask repeating(Clock clock, int id, Runnable task, long delayMilis) {
            return new TimerTask(clock, id, task, delayMilis, true);
        }

        /**
         * Returns a new, empty TimerTask with same taskId that will pass .equals on real tasks with
         * the same taskId. Use this to for comparisons to find TimerTask in collections like
         * DelayQueue
         */
        public static TimerTask empty(Clock clock, int taskId) {
            return of(clock, taskId, () -> {}, 0);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return startTime - clock.millis();
        }

        @Override
        public int compareTo(Delayed other) {
            long diff =
                    Long.compare(
                            this.getDelay(TimeUnit.MILLISECONDS),
                            other.getDelay(TimeUnit.MILLISECONDS));
            if (diff != 0) return (int) diff;
            // Tie-break by monotonically-increasing task id so tasks scheduled in
            // the same millisecond drain in FIFO order. Without this, ordering
            // among same-deadline tasks (e.g. consecutive setImmediate calls) is
            // not guaranteed by DelayQueue.
            return Integer.compare(this.id, ((TimerTask) other).id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimerTask that = (TimerTask) o;
            // we're only comparing tasks using their `id` so it's convenient to find (and remove
            // them) by id
            // and leverage helper methods like TimeoutTask.empty (check comment there)
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        public boolean isRepeating() {
            return repeating;
        }
    }

    private static class DeferredTask {
        private final Future<?> future;
        private final NativePromise promise;
        private final LambdaFunction resolve;
        private final LambdaFunction reject;
        private final VarScope scope;
        private final long started;

        public DeferredTask(
                Clock clock,
                Future<?> future,
                NativePromise promise,
                LambdaFunction resolve,
                LambdaFunction reject,
                VarScope scope) {
            this.future = future;
            this.promise = promise;
            this.resolve = resolve;
            this.reject = reject;
            this.scope = scope;
            this.started = clock.millis();
        }
    }
}
