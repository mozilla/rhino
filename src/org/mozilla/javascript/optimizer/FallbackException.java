package org.mozilla.javascript.optimizer;

/**
 * Methods invoked using InvokeDynamic can throw this exception to indicate that they are not able
 * to operate on the current call site and should be potentially replaced by a more generic method.
 */
public class FallbackException extends RuntimeException {
    public FallbackException() {}
}
