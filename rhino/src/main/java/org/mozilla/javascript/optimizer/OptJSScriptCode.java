package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.JSScript;
import org.mozilla.javascript.Scriptable;

public abstract class OptJSScriptCode extends OptJSCode<JSScript> {

    protected OptJSScriptCode() {}

    @Override
    public abstract Object execute(
            Context cx,
            JSScript executableObject,
            Object newTarget,
            Scriptable scope,
            Object thisObj,
            Object[] args);

    @Override
    public abstract Object resume(
            Context cx,
            JSScript executableObject,
            Object state,
            Scriptable scope,
            int operation,
            Object value);

    public static class Builder extends OptJSCode.Builder<JSScript> {
        public Builder(OptJSCode.BuilderEnv env) {
            super(env);
        }

        @Override
        public JSCode<JSScript> build() {
            if (built != null) {
                return built;
            }
            try {
                var subClassName = getClassName();
                var subClassBytes = getClassBytes();

                var loader = (GeneratedClassLoader) env.compiledClass.getClassLoader();
                Class<?> subClass = loader.defineClass(subClassName, subClassBytes);
                loader.linkClass(subClass);

                var instance = (OptJSScriptCode) subClass.getConstructor().newInstance();
                built = instance;
                return instance;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        String getClassName() {
            return env.className + "ojsc" + Integer.toString(index);
        }

        @Override
        byte[] getClassBytes() {
            return Codegen.generateOptJSCode(
                    env.className, methodName, methodType, resumeName, resumeType, false, index);
        }
    }
}
