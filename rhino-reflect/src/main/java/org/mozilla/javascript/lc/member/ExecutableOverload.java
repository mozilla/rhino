package org.mozilla.javascript.lc.member;

/**
 * @author ZZZank
 */
public class ExecutableOverload {
    public final String name;
    public final ExecutableBox[] methods;

    public ExecutableOverload(String name, ExecutableBox[] methods) {
        this.name = name;
        this.methods = methods;
    }

    public static final class WithField extends ExecutableOverload {
        public final NativeJavaField field;

        public WithField(ExecutableOverload old, NativeJavaField field) {
            super(old.name, old.methods);
            this.field = field;
        }
    }
}
