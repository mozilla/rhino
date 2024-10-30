package org.mozilla.javascript.optimizer;

import jdk.dynalink.Namespace;

/**
 * A list of namespaces for operations that are specific to Rhino, on top of standard namespaces
 * like "Property".
 */
@SuppressWarnings("AndroidJdkLibsChecker")
public enum RhinoNamespace implements Namespace {
    NAME,
    MATH,
}
