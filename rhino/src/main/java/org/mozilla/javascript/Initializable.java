package org.mozilla.javascript;

import java.io.Serializable;

/**
 * A single-function interface so that we can use lambda functions to lazily initialize native
 * classes.
 */
public interface Initializable extends Serializable {
    /** Initialize the class in question, returning the new constructor. */
    Object initialize(Context cx, Scriptable scope, boolean sealed);
}
