package org.mozilla.javascript;

public abstract class JSCode<T extends ScriptOrFn<T>> implements JSCodeExec<T>, JSCodeResume<T> {

    public abstract static class Builder<U extends ScriptOrFn<U>> {
        public abstract JSCode<U> build();
    }

    public static class NullBuilder<V extends ScriptOrFn<V>> extends Builder<V> {

        @Override
        public JSCode<V> build() {
            return null;
        }
    }
}
