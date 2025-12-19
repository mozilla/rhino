package org.mozilla.javascript.tools;

/**
 * This is a service interface to provide consoles. It allows there to be multiple console
 * implementations at runtime based on classpath.
 */
public interface ConsoleProvider {
    /** Create the console. Must not be called unless "isSupported" returned true. */
    Console newConsole();

    /** Return whether the console is possible to use at all */
    boolean isSupported();

    /** Return whether this console supports interactive command-line editing. */
    boolean supportsEditing();
}
