/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.shell.Global;

public class NativeJavaClassTest {
    private final String importClass =
            "importClass(Packages.org.mozilla.javascript.tests.NativeJavaClassTest)\n";
    private final Global global = new Global();
    private static int callCount = 0;

    public NativeJavaClassTest() {
        global.init(ContextFactory.getGlobal());
    }

    public static void setClass(Class<?> clazz) {
        callCount++;
    }

    @Before
    public void setUp() {
        callCount = 0;
    }

    @Test
    public void basicClass() {
        try (Context cx = ContextFactory.getGlobal().enterContext()) {
            Object result = runScript("java.lang.Object.class;");
            assertEquals(Context.javaToJS(Object.class, global), result);
        }
    }

    @Test
    public void withSetClass() {
        // setClass() still works
        runScript(importClass + "NativeJavaClassTest.setClass(java.lang.Object.class);");
        assertEquals(callCount, 1);

        // "class =" is still an alias to setClass()
        runScript(importClass + "NativeJavaClassTest.class = java.lang.Object.class;");
        assertEquals(callCount, 2);
    }

    @Test
    public void classWithStaticSetClass() {
        try (Context cx = ContextFactory.getGlobal().enterContext()) {
            Object result = runScript(importClass + "NativeJavaClassTest.class;");
            assertEquals(Context.javaToJS(NativeJavaClassTest.class, global), result);

            Object staticAccess = runScript(importClass + "NativeJavaClassTest.class;");
            Object instanceAccess =
                    runScript(importClass + "new NativeJavaClassTest().getClass();");
            assertEquals(staticAccess, instanceAccess);

            // "class =" does not update the class property
            result =
                    runScript(
                            importClass
                                    + "NativeJavaClassTest.class = java.lang.Object.class; NativeJavaClassTest.class");
            assertEquals(Context.javaToJS(NativeJavaClassTest.class, global), result);
        }
    }

    private Object runScript(final String scriptSourceText) {
        return ContextFactory.getGlobal()
                .call(context -> context.evaluateString(global, scriptSourceText, "", 1, null));
    }
}
