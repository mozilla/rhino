/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * RunScript4: Execute scripts in an environment that includes the example Counter class.
 *
 * @author Norris Boyd
 */
public class RunScript4 {

    /**
     * Main method that demonstrates creating and using custom JavaScript classes from Java objects.
     *
     * <p>This method shows advanced JavaScript-Java integration by defining a custom JavaScript
     * class (Counter) based on a Java class, creating an instance with initial parameters, and
     * making it available to JavaScript code as a global variable.
     *
     * <p>The method automatically creates a global JavaScript variable "myCounter" that is an
     * instance of the Counter class initialized with the value 7. This is equivalent to executing
     * the JavaScript code: {@code myCounter = new Counter(7);}
     *
     * <p>Example usage:
     *
     * <pre>
     * java YourClass "myCounter.count"
     * java YourClass "myCounter.count; myCounter.count; myCounter.resetCount(); myCounter.count"
     * java YourClass "var total = 0; for(var i=0; i&lt;5; i++) total += myCounter.count; total"
     * </pre>
     *
     * <p>Expected behavior:
     *
     * <ul>
     *   <li>myCounter.count returns the current count and increments it (starts at 7)
     *   <li>myCounter.resetCount() resets the counter back to 0
     *   <li>The Counter class methods and properties are fully accessible from JavaScript
     * </ul>
     *
     * <p>The method performs the following steps:
     *
     * <ol>
     *   <li>Creates and enters a JavaScript execution context
     *   <li>Initializes standard JavaScript objects (Object, Function, Math, etc.)
     *   <li>Defines the Counter class as a JavaScript constructor using defineClass()
     *   <li>Creates a new Counter instance with initial value 7 using cx.newObject()
     *   <li>Adds the Counter instance to global scope as "myCounter" variable
     *   <li>Concatenates all command line arguments into JavaScript code
     *   <li>Evaluates the JavaScript code with access to the myCounter object
     *   <li>Prints the evaluation result to standard error
     *   <li>Ensures proper cleanup of the JavaScript context
     * </ol>
     *
     * <p>This demonstrates how Java objects can be seamlessly integrated into JavaScript as
     * native-feeling JavaScript objects with full access to their methods and properties.
     *
     * @param args command line arguments that will be concatenated and evaluated as JavaScript
     *     code. The code can access and manipulate the global "myCounter" variable which is a
     *     Counter instance initialized with value 7.
     * @throws Exception if there are any errors during JavaScript execution, class definition, or
     *     object creation. This can include script compilation errors, runtime exceptions, or
     *     reflection errors during class definition.
     */
    public static void main(String args[]) throws Exception {
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();

            // Use the Counter class to define a Counter constructor
            // and prototype in JavaScript.
            ScriptableObject.defineClass(scope, Counter.class);

            // Create an instance of Counter and assign it to
            // the top-level variable "myCounter". This is
            // equivalent to the JavaScript code
            //    myCounter = new Counter(7);
            Object[] arg = {Integer.valueOf(7)};
            Scriptable myCounter = cx.newObject(scope, "Counter", arg);
            scope.put("myCounter", scope, myCounter);

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
    private RunScript4() {
        // Utility class - prevent instantiation
    }
}
