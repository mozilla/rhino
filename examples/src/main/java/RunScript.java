/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * RunScript: simplest example of controlling execution of Rhino.
 *
 * <p>Collects its arguments from the command line, executes the script, and prints the result.
 *
 * @author Norris Boyd
 */
public class RunScript {

    /**
     * Main method that evaluates JavaScript code passed as command line arguments.
     *
     * <p>This method demonstrates basic JavaScript execution using the Mozilla Rhino engine. It
     * concatenates all command line arguments into a single JavaScript expression, evaluates it,
     * and prints the result to standard error.
     *
     * <p>Example usage:
     *
     * <pre>
     * java YourClass "3 + 4"
     * java YourClass "Math.sqrt(16)"
     * java YourClass "var x = 5;" "x * 2"
     * </pre>
     *
     * <p>The method performs the following steps:
     *
     * <ol>
     *   <li>Creates and enters a JavaScript execution context
     *   <li>Initializes standard JavaScript objects (Object, Function, Math, etc.)
     *   <li>Concatenates all command line arguments into a single string
     *   <li>Evaluates the concatenated string as JavaScript code
     *   <li>Converts the result to a string and prints it to stderr
     *   <li>Ensures proper cleanup of the JavaScript context
     * </ol>
     *
     * @param args command line arguments that will be concatenated and evaluated as JavaScript
     *     code. Each argument is treated as part of a single JavaScript expression or statement
     *     sequence.
     */
    public static void main(String args[]) {
        // Creates and enters a Context. The Context stores information
        // about the execution environment of a script.
        Context cx = Context.enter();
        try {
            // Initialize the standard objects (Object, Function, etc.)
            // This must be done before scripts can be executed. Returns
            // a scope object that we use in later calls.
            Scriptable scope = cx.initStandardObjects();

            // Collect the arguments into a single string.
            String s = "";
            for (int i = 0; i < args.length; i++) {
                s += args[i];
            }

            // Now evaluate the string we've colected.
            Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);

            // Convert the result to a string and print it.
            System.err.println(Context.toString(result));

        } finally {
            // Exit from the context.
            Context.exit();
        }
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private RunScript() {
        // Utility class - prevent instantiation
    }
}
