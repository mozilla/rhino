package org.mozilla.javascript.tests;

import java.util.function.Predicate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

public class JavaAdapterTest {
    Context cx = null;
    Scriptable topScope = null;

    @Before
    public void enterContext() {
        cx = Context.enter();
        topScope = cx.initStandardObjects();
    }

    @After
    public void exitContext() {
        Context.exit();
    }

    private Object eval(String script) {
        return cx.evaluateString(topScope, script, "JavaAdapterTest", 0, null);
    }

    interface C {
        int methodInC(String str);
    }

    interface B extends C {}

    public abstract class A implements B {}

    @Test
    public void overrideMethodInMultiLayerInterface() {
        // if the method 'methodInC' is overrided from 'interface C',
        // its signature will be 'public int methodInC(java.lang.String)'  (expected result),
        // otherwise if the method 'methodInC' is newly created by JavaAdapter,
        // its signature will be 'public java.lang.Object methodInC()'
        String testCode =
                "JavaAdapter(Packages."
                        + A.class.getName()
                        + ",{methodInC:function(str){ return Number(str); }}"
                        + ",null)";

        var adapterObject = (NativeJavaObject) eval(testCode);
        var adapted = (C) adapterObject.unwrap();
        Assert.assertEquals(123, adapted.methodInC("123"));
    }

    @Test
    public void ignoreOriginalArgs() {
        String testCode =
                "JavaAdapter(Packages."
                        + A.class.getName()
                        + ",{methodInC:function(){ return 42; }}" // args are ignored
                        + ",null)";

        var adapterObject = (NativeJavaObject) eval(testCode);
        var adapted = (C) adapterObject.unwrap();
        Assert.assertEquals(42, adapted.methodInC("whatever string"));
    }

    public abstract static class StaticA implements B {}

    @Test
    public void implementStaticClass() {
        String testCode =
                "JavaAdapter(Packages."
                        + StaticA.class.getName()
                        + ",{methodInC:function(){ return 42; }}"
                        + ")"; // no args required

        var adapterObject = (NativeJavaObject) eval(testCode);
        var adapted = (C) adapterObject.unwrap();
        Assert.assertEquals(42, adapted.methodInC("whatever string"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void implementInterface() {
        String testCode =
                "JavaAdapter(Packages."
                        + Predicate.class.getName()
                        + ",{test:function(obj){ return 'rhino' == obj; }}"
                        + ")"; // no args required since superclass is Object

        var adapterObject = (NativeJavaObject) eval(testCode);
        var adapted = (Predicate<Object>) adapterObject.unwrap();
        Assert.assertTrue(adapted.test("rhino"));
        Assert.assertFalse(adapted.test("rhizo"));
        Assert.assertFalse(adapted.test(null));
        Assert.assertFalse(adapted.test(42));
    }

    /**
     * @see org.mozilla.javascript.JavaAdapter.JavaAdapterSignature
     */
    @Test
    public void adapterCache() {
        String testCode =
                String.format(
                        "JavaAdapter(Packages.%s,%s)",
                        Predicate.class.getName(),
                        "{ test: function(obj) { return 'rhino' == obj; } }");

        var adapterObject1 = (NativeJavaObject) eval(testCode);
        var adapterObject2 = (NativeJavaObject) eval(testCode);
        Assert.assertSame(adapterObject1.unwrap().getClass(), adapterObject2.unwrap().getClass());
    }
}
