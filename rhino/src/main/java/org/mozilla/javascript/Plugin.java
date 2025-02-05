package org.mozilla.javascript;

/**
 * Plugins may be loaded with the serviceLocator.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public interface Plugin {

    /**
     * defines, if this plugin should be initialized in safe standard objects or only in standard
     * objects
     */
    default boolean isSafe() {
        return false;
    }

    /** The name of the plugin. */
    String getName();

    /** Initializes the safe standard objects. */
    default void init(Context cx, ScriptableObject scope, boolean sealed) {}

    /** Initialize the compiler environmnt. */
    default void initCompilerEnvirons(Context cx, CompilerEnvirons compilerEnvirons) {}
}
