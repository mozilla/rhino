package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.ScriptOrFn;

/** Subclass of {@link JSCode} for compiled Java methods. */
public abstract class MHJSCode<T extends ScriptOrFn<T>> extends JSCode<T> {

    /**
     * Builder environment used in the creation of {@link OptJSCode} objects. This hold information
     * on whether literals will need to be initialised, and a reference to the class containing the
     * method implementation once it has been built.
     */
    public static class BuilderEnv {
        boolean hasRegExpLiterals;
        boolean hasTemplateLiterals;
        Class<?> compiledClass;
        final String className;

        public BuilderEnv(String className) {
            this.className = className;
        }
    }

    /**
     * A builder for {@link OptJSCode}. Holds the builder environment, names and types of execute
     * and resume methods, an index within the set of descriptors owned by the class, and the built
     * version of the object (used to avoid generating duplicate classes at runtime).
     */
    public abstract static class Builder<T extends ScriptOrFn<T>> extends JSCode.Builder<T> {
        final BuilderEnv env;
        String methodName;
        String methodType;
        String resumeName;
        String resumeType;
        int index;
        MHJSCode<T> built;

        public Builder(BuilderEnv env) {
            this.env = env;
        }

        @Override
        public JSCode<T> build() {
            if (built != null) {
                return built;
            }
            try {
                MethodHandles.Lookup lookup =
                        (MethodHandles.Lookup)
                                env.compiledClass
                                        .getDeclaredMethod("getLookup")
                                        .invoke(env.compiledClass);
                MethodHandle exec =
                        lookup.findStatic(
                                env.compiledClass,
                                methodName,
                                MethodType.fromMethodDescriptorString(
                                        methodType, env.compiledClass.getClassLoader()));
                MethodHandle resume = null;
                if (resumeName != null) {
                    resume =
                            lookup.findStatic(
                                    env.compiledClass,
                                    resumeName,
                                    MethodType.fromMethodDescriptorString(
                                            resumeType, env.compiledClass.getClassLoader()));
                }
                return buildCode(exec, resume);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new Error("Gnerated class did not contain expected methods", e);
            }
        }

        protected abstract MHJSCode<T> buildCode(MethodHandle exec, MethodHandle resume);

        public abstract void buildByteCode(ClassFileWriter cfw, String mainClass);
    }
}
