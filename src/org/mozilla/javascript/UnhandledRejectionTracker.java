package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is responsible for handling tracking of unhandled Promise rejections. These come up
 * when a Promise is either rejected or an exception is thrown and there is no "catch" handler set
 * up. There is one of these tracker objects for each Context class.
 *
 * <p>Different frameworks will choose different ways to handle unhandled rejections. As a result,
 * Rhino does nothing by default.
 *
 * <p>However, if "trackUnhandledPromiseRejections" is called on the Context object, then Rhino will
 * track them in this object. It is the responsibility of the product embedding Rhino to
 * periodically check for unhandled rejections in this class and either remove them or terminate the
 * script and allow the context and its tracker to be garbage- collected.
 *
 * <p>Note that if "trackUnhandledPromiseRejections" is set, and rejections are not handled, then
 * Promise objects will accumulate in this object and cause a memory leak.
 */
public class UnhandledRejectionTracker {
    private boolean enabled = false;
    private static final IdentityHashMap<NativePromise, NativePromise> unhandled =
            new IdentityHashMap<>(0);

    /**
     * Iterate through all the rejected promises that have not yet been handled. As each promise is
     * handled by this method, it is removed from this tracker and will not appear again. This is
     * useful for delivering unhandled promise notifications to users one time via a callback.
     */
    public void process(Consumer<Object> handler) {
        Iterator<NativePromise> it = unhandled.values().iterator();
        while (it.hasNext()) {
            try {
                handler.accept(it.next().getResult());
            } finally {
                // Always remove even if handler throws
                it.remove();
            }
        }
    }

    /**
     * Return a collection of all of the results of any unhandled rejected promises. This does not
     * remove unhandled promises from the collection, but reports the current state. It is useful
     * for command-line tools.
     *
     * @return a read-only collection of promise results. To clear them, call "process".
     */
    public List<Object> enumerate() {
        ArrayList<Object> ret = new ArrayList<>();
        for (NativePromise result : unhandled.values()) {
            ret.add(result.getResult());
        }
        return ret;
    }

    void enable(boolean enabled) {
        this.enabled = enabled;
    }

    void promiseRejected(NativePromise p) {
        if (enabled) {
            unhandled.put(p, p);
        }
    }

    void promiseHandled(NativePromise p) {
        if (enabled) {
            unhandled.remove(p);
        }
    }
}
