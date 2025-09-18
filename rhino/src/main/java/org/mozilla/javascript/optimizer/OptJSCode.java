package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.ScriptOrFn;

public abstract class OptJSCode<T extends ScriptOrFn<T>> extends JSCode<T> {

    protected OptJSCode() {}

    public static class BuilderEnv {
        boolean hasRegExpLiterals;
        boolean hasTemplateLiterals;
        Class<?> compiledClass;
    }

    public abstract static class Builder<T extends ScriptOrFn<T>> extends JSCode.Builder<T> {
        final BuilderEnv env;
        String methodName;
        String methodType;
        String resumeName;
        String resumeType;
        int index;
        OptJSCode<T> built;

        public Builder(BuilderEnv env) {
            this.env = env;
        }
    }
}
