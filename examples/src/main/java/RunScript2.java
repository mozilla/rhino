/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * RunScript2: Like RunScript, but reflects the System.out into JavaScript.
 *
 * @author Norris Boyd
 */
public class RunScript2 {

    /**
     * Main method that evaluates JavaScript code with access to Java's System.out.
     *
     * <p>This method demonstrates JavaScript execution using the Mozilla Rhino engine with Java
     * object integration. It makes Java's System.out available as a global JavaScript variable
     * named "out", allowing JavaScript code to directly call Java print methods.
     *
     * <p>Example usage:
     *
     * <pre>
     * java YourClass "out.println('Hello from JavaScript')"
     * java YourClass "out.print('Result: ');" "out.println(3 + 4)"
     * java YourClass "for(var i=0; i&lt;3; i++) out.println('Line ' + i)"
     * </pre>
     *
     * <p>The method performs the following steps:
     *
     * <ol>
     *   <li>Creates and enters a JavaScript execution context
     *   <li>Initializes standard JavaScript objects (Object, Function, Math, etc.)
     *   <li>Converts Java's System.out to a JavaScript object using Context.javaToJS()
     *   <li>Adds the converted System.out as a global "out" variable in JavaScript scope
     *   <li>Concatenates all command line arguments into a single string
     *   <li>Evaluates the concatenated string as JavaScript code
     *   <li>Converts the result to a string and prints it to stderr
     *   <li>Ensures proper cleanup of the JavaScript context
     * </ol>
     *
     * <p>The global "out" variable in JavaScript provides access to all methods of Java's
     * PrintStream class, including println(), print(), printf(), etc.
     *
     * @param args command line arguments that will be concatenated and evaluated as JavaScript
     *     code. The JavaScript code can use the global "out" variable to access System.out methods
     *     for printing output.
     */
    public static void main(String args[]) {
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();

            // Add a global variable "out" that is a JavaScript reflection
            // of System.out
            Object jsOut = Context.javaToJS(System.out, scope);
            ScriptableObject.putProperty(scope, "out", jsOut);

            String s = "";
            for (int i = 0; i < args.length; i++) {
                s += args[i];
            }
            Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);
            System.err.println(Context.toString(result));
        } finally {
            Context.exit();
        }
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private RunScript2() {
        // Utility class - prevent instantiation
    }
}
