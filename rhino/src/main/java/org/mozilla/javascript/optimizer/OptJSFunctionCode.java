package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Scriptable;

/**
 * Subclass of {@link OptJSCode} which narrows the type paramter to {@link JSFunction}. This allows
 * compiled code to be type safe and avoid casting when accessing the executable object.
 */
public abstract class OptJSFunctionCode extends OptJSCode<JSFunction> {

    protected OptJSFunctionCode() {}

    @Override
    public abstract Object execute(
            Context cx,
            JSFunction executableObject,
            Object newTarget,
            Scriptable scope,
            Object thisObj,
            Object[] args);

    @Override
    public abstract Object resume(
            Context cx,
            JSFunction executableObject,
            Object state,
            Scriptable scope,
            int operation,
            Object value);

    public static class Builder extends OptJSCode.Builder<JSFunction> {

        public Builder(OptJSCode.BuilderEnv env) {
            super(env);
        }

        @Override
        public JSCode<JSFunction> build() {
            if (built != null) {
                return built;
            }
            try {
                var subClassName = getClassName();
                var subClassBytes = getClassBytes();

                var loader = (GeneratedClassLoader) env.compiledClass.getClassLoader();
                Class<?> subClass = loader.defineClass(subClassName, subClassBytes);
                loader.linkClass(subClass);

                var instance = (OptJSFunctionCode) subClass.getConstructor().newInstance();
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
                    env.className,
                    methodName,
                    methodType,
                    resumeName,
                    resumeType,
                    true,
                    index); // TODO Auto-generated method stub
        }
    }
}
