package org.mozilla.javascript.optimizer;

import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.ScriptOrFn;

/** Subclass of {@link JSCode} for compiled Java methods. */
public abstract class OptJSCode<T extends ScriptOrFn<T>> extends JSCode<T> {

    protected OptJSCode() {}

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
        OptJSCode<T> built;

        public Builder(BuilderEnv env) {
            this.env = env;
        }

        /** Return the name of the generated {@link OptJSCode} subclass. */
        abstract String getClassName();

        /** Return the bytes of the generated {@link OptJSCode} subclass. */
        abstract byte[] getClassBytes();

        /**
         * Generate bytecode to instantiate the generated {@link OptJSCode} subclass. This is used
         * by the class compiler.
         */
        void buildByteCode(ClassFileWriter cfw) {
            var className = getClassName();
            var signature = className.replaceAll("\\.", "/");
            cfw.add(ByteCode.NEW, signature);
            cfw.add(ByteCode.DUP);
            cfw.addInvoke(ByteCode.INVOKESPECIAL, className, "<init>", "()V");
        }
    }
}
