package org.mozilla.javascript;

import java.util.Iterator;
import java.util.ServiceLoader;

public class NullabilityDetectorProvider {
    private static NullabilityDetectorProvider provider;
    private final ServiceLoader<NullabilityDetector> loader;

    private NullabilityDetectorProvider() {
        loader = ServiceLoader.load(NullabilityDetector.class);
    }

    public static NullabilityDetectorProvider getInstance() {
        if (provider == null) {
            provider = new NullabilityDetectorProvider();
        }

        return provider;
    }

    public NullabilityDetector getImpl() {
        Iterator<NullabilityDetector> iterator = loader.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }
}
