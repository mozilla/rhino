/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;

/**
 * An example host object class.
 *
 * <p>Here's a shell session showing the Foo object in action:
 *
 * <pre>
 * js&gt; defineClass("Foo")
 * js&gt; foo = new Foo();      &lt;i&gt;A constructor call, see <a href="#Foo">Foo</a> below.&lt;/i&gt;
 * [object Foo]                 &lt;i&gt;The "Foo" here comes from <a href="#getClassName">getClassName</a>.&lt;/i&gt;
 * js&gt; foo.counter;          &lt;i&gt;The counter property is defined by the <code>defineProperty</code>&lt;/i&gt;
 * 0                            &lt;i&gt;call below and implemented by the <a href="#getCounter">getCounter</a>&lt;/i&gt;
 * js&gt; foo.counter;          &lt;i&gt;method below.&lt;/i&gt;
 * 1
 * js&gt; foo.counter;
 * 2
 * js&gt; foo.resetCounter();   &lt;i&gt;Results in a call to <a href="#resetCounter">resetCounter</a>.&lt;/i&gt;
 * js&gt; foo.counter;          &lt;i&gt;Now the counter has been reset.&lt;/i&gt;
 * 0
 * js&gt; foo.counter;
 * 1
 * js&gt; bar = new Foo(37);    &lt;i&gt;Create a new instance.&lt;/i&gt;
 * [object Foo]
 * js&gt; bar.counter;          &lt;i&gt;This instance's counter is distinct from&lt;/i&gt;
 * 37                           &lt;i&gt;the other instance's counter.&lt;/i&gt;
 * js&gt; foo.varargs(3, "hi"); &lt;i&gt;Calls <a href="#varargs">varargs</a>.&lt;/i&gt;
 * this = [object Foo]; args = [3, hi]
 * js&gt; foo[7] = 34;          &lt;i&gt;Since we extended ScriptableObject, we get&lt;/i&gt;
 * 34                           &lt;i&gt;all the behavior of a JavaScript object&lt;/i&gt;
 * js&gt; foo.a = 23;           &lt;i&gt;for free.&lt;/i&gt;
 * 23
 * js&gt; foo.a + foo[7];
 * 57
 * js&gt;
 * </pre>
 *
 * @see org.mozilla.javascript.Context
 * @see org.mozilla.javascript.Scriptable
 * @see org.mozilla.javascript.ScriptableObject
 * @author Norris Boyd
 */
public class Foo extends ScriptableObject {
    private static final long serialVersionUID = -3833489808933339159L;

    /**
     * The zero-parameter constructor.
     *
     * <p>When Context.defineClass is called with this class, it will construct Foo.prototype using
     * this constructor.
     */
    public Foo() {}

    /**
     * The Java method defining the JavaScript Foo constructor.
     *
     * <p>Takes an initial value for the counter property. Note that in the example Shell session
     * above, we didn't supply a argument to the Foo constructor. This means that the Undefined
     * value is used as the value of the argument, and when the argument is converted to an integer,
     * Undefined becomes 0.
     */
    public Foo(int counterStart) {
        counter = counterStart;
    }

    /** Returns the name of this JavaScript class, "Foo". */
    @Override
    public String getClassName() {
        return "Foo";
    }

    /**
     * The Java method defining the JavaScript resetCounter function.
     *
     * <p>Resets the counter to 0.
     */
    @JSFunction
    public void resetCounter() {
        counter = 0;
    }

    /**
     * The Java method implementing the getter for the counter property.
     *
     * <p>If "setCounter" had been defined in this class, the runtime would call the setter when the
     * property is assigned to.
     */
    @JSGetter
    public int getCounter() {
        return counter++;
    }

    /**
     * An example of a variable-arguments method.
     *
     * <p>All variable arguments methods must have the same number and types of parameters, and must
     * be static.
     *
     * <p>
     *
     * @param cx the Context of the current thread
     * @param thisObj the JavaScript 'this' value.
     * @param args the array of arguments for this call
     * @param funObj the function object of the invoked JavaScript function This value is useful to
     *     compute a scope using Context.getTopLevelScope().
     * @return computes the string values and types of 'this' and of each of the supplied arguments
     *     and returns them in a string.
     * @see org.mozilla.javascript.ScriptableObject#getTopLevelScope
     */
    @JSFunction
    public static Object varargs(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        StringBuilder buf = new StringBuilder();
        buf.append("this = ");
        buf.append(Context.toString(thisObj));
        buf.append("; args = [");
        for (int i = 0; i < args.length; i++) {
            buf.append(Context.toString(args[i]));
            if (i + 1 != args.length) buf.append(", ");
        }
        buf.append("]");
        return buf.toString();
    }

    /** A piece of private data for this class. */
    private int counter;
}
