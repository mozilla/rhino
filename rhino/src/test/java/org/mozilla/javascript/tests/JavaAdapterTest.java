package org.mozilla.javascript.tests;

import org.junit.After;
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

    interface C {
        int methodInC(String str);
    }

    interface B extends C {}

    public abstract class A implements B {}

    @Test
    public void overrideMethodInMultiLayerInterface() throws NoSuchMethodException {
        String testCode =
                "JavaAdapter(Packages." + A.class.getName() + ",{methodInC:function(){}},null)";

        NativeJavaObject adapterObject =
                (NativeJavaObject) cx.evaluateString(topScope, testCode, "", 1, null);

        // if the method 'methodInC' is overrided from 'interface C',
        // its signature will be 'public int methodInC(java.lang.String)'  (expected result),
        // otherwise if the method 'methodInC' is newly created by JavaAdapter,
        // its signature will be 'public java.lang.Object methodInC()'
        adapterObject.unwrap().getClass().getDeclaredMethod("methodInC", String.class);
    }
}
