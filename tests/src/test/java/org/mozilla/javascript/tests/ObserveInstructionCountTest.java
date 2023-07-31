/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.drivers.TestUtils;

/** @author Norris Boyd */
public class ObserveInstructionCountTest {
    // Custom Context to store execution time.
    static class MyContext extends Context {
        MyContext(ContextFactory factory) {
            super(factory);
        }

        int quota;
    }

    static class QuotaExceeded extends RuntimeException {
        private static final long serialVersionUID = -8018441873635071899L;
    }

    @Before
    public void setUp() {
        TestUtils.setGlobalContextFactory(new MyFactory());
    }

    @After
    public void tearDown() {
        TestUtils.setGlobalContextFactory(null);
    }

    static class MyFactory extends ContextFactory {

        @Override
        protected Context makeContext() {
            MyContext cx = new MyContext(this);
            // Make Rhino runtime call observeInstructionCount
            // each 500 bytecode instructions (if we're really enforcing
            // a quota of 2000, we could set this closer to 2000)
            cx.setInstructionObserverThreshold(500);
            return cx;
        }

        @Override
        protected void observeInstructionCount(Context cx, int instructionCount) {
            MyContext mcx = (MyContext) cx;
            mcx.quota -= instructionCount;
            if (mcx.quota <= 0) {
                throw new QuotaExceeded();
            }
        }

        @Override
        protected Object doTopCall(
                Callable callable,
                Context cx,
                Scriptable scope,
                Scriptable thisObj,
                Object[] args) {
            MyContext mcx = (MyContext) cx;
            mcx.quota = 2000;
            return super.doTopCall(callable, cx, scope, thisObj, args);
        }
    }

    private static void baseCase(String source) {
        Utils.runWithAllOptimizationLevels(
                new MyFactory(),
                cx -> {
                    assertTrue(cx instanceof MyContext);
                    try {
                        Scriptable globalScope = cx.initStandardObjects();
                        cx.evaluateString(globalScope, source, "test source", 1, null);
                        fail();
                    } catch (QuotaExceeded e) {
                        // expected
                    } catch (RuntimeException e) {
                        fail(e.toString());
                    }

                    return null;
                });
    }

    @Test
    public void whileTrueInGlobal() {
        String source = "var i=0; while (true) i++;";
        baseCase(source);
    }

    @Test
    public void twhileTrueNoCounterInGlobal() {
        String source = "while (true);";
        baseCase(source);
    }

    @Test
    public void whileTrueInFunction() {
        String source = "var i=0; function f() { while (true) i++; } f();";
        baseCase(source);
    }

    @Test
    public void forever() {
        String source = "for(;;);";
        baseCase(source);
    }

    @Test
    public void longRunningRegExp() {
        String source =
                "/(.*){1,32000}[bc]/.test(\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\");";
        baseCase(source);
    }
}
