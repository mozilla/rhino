/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * An example illustrating how to create a JavaScript object and retrieve properties and call
 * methods using the Mozilla Rhino JavaScript engine.
 *
 * <p>This test class demonstrates the integration between Java and JavaScript by:
 *
 * <ul>
 *   <li>Creating a JavaScript execution context
 *   <li>Defining a custom JavaScript class (Counter) in the scope
 *   <li>Creating an instance of the Counter object
 *   <li>Accessing JavaScript properties from Java
 *   <li>Calling JavaScript methods from Java
 * </ul>
 *
 * <p>Expected output should be:
 *
 * <pre>
 * count = 0
 * count = 1
 * resetCount
 * count = 0
 * </pre>
 *
 * @author Your Name
 * @version 1.0
 * @since 1.0
 */
public class CounterTest {

    /**
     * Main method that demonstrates JavaScript-Java integration using Rhino.
     *
     * <p>This method performs the following operations:
     *
     * <ol>
     *   <li>Enters a JavaScript context for execution
     *   <li>Initializes standard JavaScript objects in the scope
     *   <li>Defines the Counter class as a JavaScript constructor
     *   <li>Creates a new Counter instance using default constructor (count = 0)
     *   <li>Retrieves the count property twice (demonstrating post-increment behavior)
     *   <li>Calls the resetCount method to reset the counter
     *   <li>Retrieves the count property again to verify reset
     * </ol>
     *
     * <p>The Context.enter() and Context.exit() calls ensure proper resource management for the
     * JavaScript execution environment.
     *
     * @param args command line arguments (not used in this example)
     * @throws Exception if there are any errors during JavaScript execution, such as script
     *     compilation errors or runtime exceptions
     */
    public static void main(String[] args) throws Exception {
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();
            ScriptableObject.defineClass(scope, Counter.class);

            Scriptable testCounter = cx.newObject(scope, "Counter");

            Object count = ScriptableObject.getProperty(testCounter, "count");
            System.out.println("count = " + count);

            count = ScriptableObject.getProperty(testCounter, "count");
            System.out.println("count = " + count);

            ScriptableObject.callMethod(testCounter, "resetCount", new Object[0]);
            System.out.println("resetCount");

            count = ScriptableObject.getProperty(testCounter, "count");
            System.out.println("count = " + count);
        } finally {
            Context.exit();
        }
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private CounterTest() {
        // Utility class - prevent instantiation
    }
}
