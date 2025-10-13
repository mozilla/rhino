package org.mozilla.javascript;

/**
 * Represents code that is executable, and potentially resumable. Those concepts are separated out
 * as {@link JSCodeExec} and {@link JSCodeResume} to allow for the use of functional interfaces.
 */
public abstract class JSCode<T extends ScriptOrFn<T>> implements JSCodeExec<T>, JSCodeResume<T> {

    /**
     * Represents a mutable {@link JSCode} object in the process of being created, usually as a
     * child of a {@link JSDescriptor.Builder}. This is necessary because during compilationthere
     * may not be a concrete, class, method, or interpreter data structure yet,
     */
    public abstract static class Builder<U extends ScriptOrFn<U>> {

        /** Builds and returns the {@link JSCode} object. */
        public abstract JSCode<U> build();
    }

    /**
     * A builder for a null code entry. This may be used when either the constructor or call action
     * of a function is not supported and so will be null.
     */
    public static class NullBuilder<V extends ScriptOrFn<V>> extends Builder<V> {

        @Override
        public JSCode<V> build() {
            return null;
        }
    }
}
