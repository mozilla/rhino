package org.mozilla.javascript;

/**
 * Loader for the V8 typed arrays.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public interface TypedArraysLoader {
    void load(ScriptableObject scope, boolean sealed);
}
