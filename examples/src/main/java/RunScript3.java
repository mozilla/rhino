/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * RunScript3: Example of using JavaScript objects
 *
 * <p>Collects its arguments from the command line, executes the script, and then ...
 *
 * @author Norris Boyd
 */
public class RunScript3 {

    /**
     * Main method that evaluates JavaScript code and then accesses JavaScript variables and
     * functions from Java.
     *
     * <p>This method demonstrates advanced JavaScript-Java integration using the Mozilla Rhino
     * engine. After executing JavaScript code, it inspects the JavaScript scope to access variables
     * and call functions that were defined in the JavaScript code.
     *
     * <p>Example usage:
     *
     * <pre>
     * java YourClass "var x = 42; function f(arg) { return 'Hello ' + arg; }"
     * java YourClass "x = Math.PI;" "function f(n) { return n * 2; }"
     * java YourClass "var x = 'test';" "f = function(s) { return s.toUpperCase(); }"
     * </pre>
     *
     * <p>Expected output format:
     *
     * <pre>
     * x = [value of variable x]
     * f('my args') = [return value of function f]
     * </pre>
     *
     * <p>If the variable x is not defined, prints: "x is not defined."<br>
     * If function f is not defined or not a function, prints: "f is undefined or not a function."
     *
     * <p>The method performs the following steps:
     *
     * <ol>
     *   <li>Creates and enters a JavaScript execution context
     *   <li>Initializes standard JavaScript objects (Object, Function, Math, etc.)
     *   <li>Concatenates all command line arguments into a single JavaScript code string
     *   <li>Evaluates the JavaScript code (ignoring the return value)
     *   <li>Attempts to retrieve the variable "x" from the JavaScript scope
     *   <li>Prints the value of "x" or a message if undefined
     *   <li>Attempts to retrieve the function "f" from the JavaScript scope
     *   <li>If "f" is a valid function, calls it with the argument "my arg"
     *   <li>Prints the function result or an error message if function is invalid
     *   <li>Ensures proper cleanup of the JavaScript context
     * </ol>
     *
     * <p>This demonstrates how Java code can inspect and interact with JavaScript variables and
     * functions after script execution, enabling bidirectional communication between the JavaScript
     * and Java environments.
     *
     * @param args command line arguments that will be concatenated and evaluated as JavaScript
     *     code. The code should define a variable "x" and/or a function "f" to see the full
     *     demonstration of Java-JavaScript variable and function access.
     */
    public static void main(String args[]) {
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();

            // Collect the arguments into a single string.
            String s = "";
            for (int i = 0; i < args.length; i++) {
                s += args[i];
            }

            // Now evaluate the string we've collected. We'll ignore the result.
            cx.evaluateString(scope, s, "<cmd>", 1, null);

            // Print the value of variable "x"
            Object x = scope.get("x", scope);
            if (x == Scriptable.NOT_FOUND) {
                System.out.println("x is not defined.");
            } else {
                System.out.println("x = " + Context.toString(x));
            }

            // Call function "f('my arg')" and print its result.
            Object fObj = scope.get("f", scope);
            if (!(fObj instanceof Function)) {
                System.out.println("f is undefined or not a function.");
            } else {
                Object functionArgs[] = {"my arg"};
                Function f = (Function) fObj;
                Object result = f.call(cx, scope, scope, functionArgs);
                String report = "f('my args') = " + Context.toString(result);
                System.out.println(report);
            }
        } finally {
            Context.exit();
        }
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private RunScript3() {
        // Utility class - prevent instantiation
    }
}
