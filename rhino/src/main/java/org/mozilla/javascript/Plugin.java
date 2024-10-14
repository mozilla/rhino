package org.mozilla.javascript;

/**
 * Plugins may be loaded with the serviceLocator.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public interface Plugin {

    /** Initializes the safe standard objects. */
    default void initSafeStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {}

    /** Initializes the (unsafe) standard objects. */
    default void initStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {}

    /** Initialize the compiler environmnt. */
    default void initCompilerEnvirons(Context cx, CompilerEnvirons compilerEnvirons) {}
}
