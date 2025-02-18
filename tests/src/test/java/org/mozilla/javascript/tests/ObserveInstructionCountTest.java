/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.testutils.Utils;

/**
 * @author Norris Boyd
 */
public class ObserveInstructionCountTest {

    static class MyContext extends Context {
        MyContext(ContextFactory factory) {
            super(factory);
        }

        private int quota;
        private boolean observed;

        @Override
        protected void observeInstructionCount(int instructionCount) {
            super.observeInstructionCount(instructionCount);
            observed = true;
        }

        public boolean wasObserved() {
            return observed;
        }
    }

    static class QuotaExceededException extends RuntimeException {}

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
                throw new QuotaExceededException();
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
        Utils.runWithAllModes(
                new MyFactory(),
                cx -> {
                    assertTrue(cx instanceof MyContext);
                    try {
                        Scriptable globalScope = cx.initStandardObjects();
                        cx.evaluateString(globalScope, source, "test source", 1, null);
                        fail();
                    } catch (QuotaExceededException e) {
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
    public void whileTrueNoCounterInGlobal() {
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

    /** see https://github.com/mozilla/rhino/issues/1497 */
    @Test
    public void regExpObserved() {
        Utils.runWithAllModes(
                new ContextFactory() {
                    @Override
                    protected Context makeContext() {
                        return new MyContext(this);
                    }
                },
                cx -> {
                    assertTrue(cx instanceof MyContext);
                    cx.setInstructionObserverThreshold(0);
                    try {
                        Scriptable globalScope = cx.initStandardObjects();
                        cx.evaluateString(
                                globalScope, "/(.*)ab/.test(\"1234\");", "test source", 1, null);
                        assertFalse(((MyContext) cx).wasObserved());
                    } catch (RuntimeException e) {
                        fail(e.toString());
                    }

                    return null;
                });
    }
}
