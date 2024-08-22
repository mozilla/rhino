package org.mozilla.javascript.drivers;

/**
 * This exception is thrown by Promises and other kinds of tests that need to mess with how
 * JavaScript does exception handling and still have a way to fail all the way to the top.
 */
public class TestFailureException extends RuntimeException {

    public TestFailureException(String msg) {
        super(msg);
    }
}
