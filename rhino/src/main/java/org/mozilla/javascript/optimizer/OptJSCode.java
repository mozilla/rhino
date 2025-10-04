package org.mozilla.javascript.optimizer;

import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.ScriptOrFn;

public abstract class OptJSCode<T extends ScriptOrFn<T>> extends JSCode<T> {

    protected OptJSCode() {}

    public static class BuilderEnv {
        boolean hasRegExpLiterals;
        boolean hasTemplateLiterals;
        Class<?> compiledClass;
        final String className;

        public BuilderEnv(String className) {
            this.className = className;
        }
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

        abstract String getClassName();

        abstract byte[] getClassBytes();

        void buildByteCode(ClassFileWriter cfw) {
            var className = getClassName();
            var signature = className.replaceAll("\\.", "/");
            cfw.add(ByteCode.NEW, signature);
            cfw.add(ByteCode.DUP);
            cfw.addInvoke(ByteCode.INVOKESPECIAL, className, "<init>", "()V");
        }
    }
}
