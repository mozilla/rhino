package org.mozilla.javascript.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JavaAdapterInvokeTest {
    Context cx = null;
    Scriptable topScope = null;

    @Before
    public void enterContext() {
        cx = Context.enter();
        cx.setOptimizationLevel(-1);
        topScope = cx.initStandardObjects();
    }

    @After
    public void exitContext() {
        Context.exit();
    }

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

    @Test
    public void testInvoke() throws NoSuchMethodException {
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

        Number result = (Number) cx.evaluateString(topScope, testCode, "", 1, null);
        Assert.assertEquals(50, result.intValue());
    }

    @Test
    public void testInvokeWithPrototype() throws NoSuchMethodException {
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

        Number result = (Number) cx.evaluateString(topScope, testCode, "", 1, null);
        Assert.assertEquals(50, result.intValue());
    }

    @Test
    public void testInvokeWithPrototypeAndCtor() throws NoSuchMethodException {
        String testCode =
                "'use strict'\n"
                        + "function Obj() { this.myObj = {one: 1} }\n"
                        + "Obj.prototype.m1 = function(i) { return i + this.myObj.one }\n"
                        + "Obj.prototype.m2 = function() { return 7 }\n"
                        + "var impl = new Obj()\n"
                        + "adapter = new Packages."
                        + AdapterClass.class.getName()
                        + "(impl)\n"
                        + "adapter.doIt(42)";

        Number result = (Number) cx.evaluateString(topScope, testCode, "", 1, null);
        Assert.assertEquals(50, result.intValue());
    }

    @Test
    public void testInvokeJsOnly() throws NoSuchMethodException {
        String testCode =
                "'use strict'\n"
                        + "function Obj() { this.myObj = {one: 1} }\n"
                        + "Obj.prototype.m1 = function(i) { return i + this.myObj.one }\n"
                        + "Obj.prototype.m2 = function() { return 7 }\n"
                        + "function Adapter(adapter) { this.adapter = adapter }\n"
                        + "Adapter.prototype.doIt = function(i) { return this.adapter.m1(i) + this.adapter.m2() }\n"
                        + "var impl = new Obj()\n"
                        + "adapter = new Adapter(impl)\n"
                        + "adapter.doIt(42)";

        Number result = (Number) cx.evaluateString(topScope, testCode, "", 1, null);
        Assert.assertEquals(50, result.intValue());
    }
}
