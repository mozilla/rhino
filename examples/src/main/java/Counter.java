/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;

/**
 * A sample counter class that can be used from JavaScript through the Rhino engine. This class
 * extends ScriptableObject to provide JavaScript integration, allowing instances to be created and
 * manipulated from JavaScript code.
 *
 * <p>The counter maintains an internal count value that can be accessed and reset through
 * JavaScript methods. Each call to getCount() returns the current value and then increments it.
 */
public class Counter extends ScriptableObject {
    private static final long serialVersionUID = 438270592527335642L;

    /**
     * The zero-argument constructor used by Rhino runtime to create instances. Initializes the
     * counter with a default value of 0.
     */
    public Counter() {}

    /**
     * Constructor used by JavaScript to create Counter instances with an initial value. The
     * {@code @JSConstructor} annotation defines this as the JavaScript constructor.
     *
     * @param a the initial count value to set for this counter
     */
    @JSConstructor
    public Counter(int a) {
        count = a;
    }

    /**
     * Returns the class name for this ScriptableObject. This method is required by the
     * ScriptableObject interface and defines how this object appears in JavaScript.
     *
     * @return the fixed string 'Counter' which is the JavaScript class name
     */
    @Override
    public String getClassName() {
        return "Counter";
    }

    /**
     * Gets the current count value and increments it for the next call. The {@code @JSGetter}
     * annotation makes this method accessible as a JavaScript property getter. Each call returns
     * the current value and then increments the internal counter by 1.
     *
     * @return the current count value before incrementing
     */
    @JSGetter
    public int getCount() {
        return count++;
    }

    /**
     * Resets the counter back to zero. The {@code @JSFunction} annotation makes this method
     * callable from JavaScript as resetCount().
     */
    @JSFunction
    public void resetCount() {
        count = 0;
    }

    /**
     * Private field that stores the current counter value. This field is accessed through the
     * JavaScript property getter and can be reset using the resetCount method.
     */
    private int count;
}
