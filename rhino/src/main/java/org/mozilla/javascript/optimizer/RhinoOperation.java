package org.mozilla.javascript.optimizer;

import jdk.dynalink.Operation;

/**
 * A list of operation types used that are specific to Rhino, in addition to standard operations
 * like GET and SET...
 */
public enum RhinoOperation implements Operation {
    BIND,
    GETNOWARN,
    GETWITHTHIS,
    GETELEMENT,
    GETINDEX,
    SETSTRICT,
    SETCONST,
    SETELEMENT,
    SETINDEX,
}
