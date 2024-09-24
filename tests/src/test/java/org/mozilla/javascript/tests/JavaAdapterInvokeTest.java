package org.mozilla.javascript.tests;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Scriptable;

public class JavaAdapterInvokeTest {

    public interface AdapterInterface {
        int m1(int i);

        int m2();
    }

    public static class AdapterClass {
        private AdapterInterface adapter;

        public AdapterClass(AdapterInterface adapter) {
            this.adapter = adapter;
        }

        public int doIt(int i) {
            return this.adapter.m1(i) + this.adapter.m2();
        }
    }

    /**
     * This test creates a new {@link AdapterClass} and passes a javascript object with two
     * functions (m1,m2) to the constructor.
     *
     * <p>It is expected, that rhino creates an {@link AdapterInterface}, that delegates the calls
     * to m1/m2 back to the javascript implementation.
     */
    @Test
    public void testInvoke() {
        String testCode =
                "'use strict'\n"
                        + "var impl = {"
                        + "  m1: function(i) { return i + 1 },\n"
                        + "  m2: function() { return 7 }\n"
                        + "}\n"
                        + "adapter = new Packages."
                        + AdapterClass.class.getName()
                        + "(impl)\n"
                        + "adapter.doIt(42)";
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    Number result = (Number) cx.evaluateString(scope, testCode, "", 1, null);
                    Assert.assertEquals(50, result.intValue());
                    return null;
                });
    }

    /** Similar to {@link #testInvoke()}, but we use a javascript object, created from prototype. */
    @Test
    public void testInvokeWithPrototype() {
        String testCode =
                "'use strict'\n"
                        + "function Obj() {}\n"
                        + "Obj.prototype.m1 = function(i) { return i + 1 }\n"
                        + "Obj.prototype.m2 = function() { return 7 }\n"
                        + "var impl = new Obj()\n"
                        + "adapter = new Packages."
                        + AdapterClass.class.getName()
                        + "(impl)\n"
                        + "adapter.doIt(42)";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    Number result = (Number) cx.evaluateString(scope, testCode, "", 1, null);
                    Assert.assertEquals(50, result.intValue());
                    return null;
                });
    }

    /**
     * Test uses some value in the method that are either constructed in ctor or in prototype. This
     * test fails before #1453
     */
    @Test
    public void testInvokeWithPrototypeAndObjs() {
        String testCode =
                "'use strict'\n"
                        + "function Obj() { this.myObj = {one: 1} }\n"
                        + "Obj.prototype.seven = 7\n"
                        + "Obj.prototype.m1 = function(i) { return i + this.myObj.one }\n"
                        + "Obj.prototype.m2 = function() { return this.seven }\n"
                        + "var impl = new Obj()\n"
                        + "adapter = new Packages."
                        + AdapterClass.class.getName()
                        + "(impl)\n"
                        + "adapter.doIt(42)";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    Number result = (Number) cx.evaluateString(scope, testCode, "", 1, null);
                    Assert.assertEquals(50, result.intValue());
                    return null;
                });
    }

    @Test
    public void testJavaLangThread() {
        String testCode =
                "'use strict'\n"
                        + "function MyRunnable() { this.myObj = {one: 1} }\n"
                        + "MyRunnable.prototype.run = function() { this.myObj.one++ }\n"
                        + "var runnable = new MyRunnable()\n"
                        + "var thread = new java.lang.Thread(runnable)\n"
                        + "thread.start()\n"
                        + "thread.join()\n"
                        + "runnable.myObj.one"; // we do not ue start here (as we do not catch the error

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    Number result = (Number)  cx.evaluateString(scope, testCode, "", 1, null);
                    Assert.assertEquals(2, result.intValue());
                    return null;
                });
    }

    /** Equivalent javascript code of {@link #testInvokeWithPrototypeAndObjs()} */
    @Test
    public void testInvokeJsOnly() {
        String testCode =
                "'use strict'\n"
                        + "function Obj() { this.myObj = {one: 1} }\n"
                        + "Obj.prototype.seven = 7\n"
                        + "Obj.prototype.m1 = function(i) { return i + this.myObj.one }\n"
                        + "Obj.prototype.m2 = function() { return this.seven }\n"
                        + "function Adapter(adapter) { this.adapter = adapter }\n"
                        + "Adapter.prototype.doIt = function(i) { return this.adapter.m1(i) + this.adapter.m2() }\n"
                        + "var impl = new Obj()\n"
                        + "adapter = new Adapter(impl)\n"
                        + "adapter.doIt(42)";

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    final Scriptable scope = cx.initStandardObjects();
                    Number result = (Number) cx.evaluateString(scope, testCode, "", 1, null);
                    Assert.assertEquals(50, result.intValue());
                    return null;
                });
    }
}
