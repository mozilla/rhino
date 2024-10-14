package org.mozilla.javascript;

/**
 * Plugins may be loaded with the serviceLocator.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public interface Plugin {

    default void initSafeStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {}
}
