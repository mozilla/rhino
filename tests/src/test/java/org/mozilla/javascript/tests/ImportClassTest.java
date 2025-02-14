/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.ShellContextFactory;

/**
 * @author donnamalayeri
 */
public class ImportClassTest {

    protected final Global global = new Global();

    public ImportClassTest() {
        global.init(contextFactory);
    }

    @Before
    public void setUp() {
        TestUtils.setGlobalContextFactory(contextFactory);
    }

    @After
    public void tearDown() {
        TestUtils.setGlobalContextFactory(null);
    }

    private ContextFactory contextFactory =
            new ShellContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (featureIndex == Context.FEATURE_ENHANCED_JAVA_ACCESS) return true;
                    return super.hasFeature(cx, featureIndex);
                }
            };

    @Test
    public void importPackageAndClass() {
        Object result =
                runScript(
                        "importPackage(java.util);\n"
                                + "UUID.randomUUID();\n" // calls getPkgProperty("UUID", global,
                                // false)
                                + "importClass(java.util.UUID);\n"
                                + "UUID.randomUUID();\n"); // calls getPkgProperty("UUID",
        // NativeJavaPackage, true)
        assertTrue(Context.jsToJava(result, UUID.class) instanceof UUID);
    }

    @Test
    public void importInSameContext() {
        Object result = runScript("importClass(java.util.UUID);UUID.randomUUID();");
        assertTrue(Context.jsToJava(result, UUID.class) instanceof UUID);
        result = runScript("importClass(java.util.UUID);UUID.randomUUID();");
        assertTrue(Context.jsToJava(result, UUID.class) instanceof UUID);
    }

    @Test
    public void importMultipleTimes() {
        Utils.runWithAllModes(
                cx -> {
                    ScriptableObject sharedScope = cx.initStandardObjects();
                    // sharedScope.sealObject(); // code below will try to modify sealed object

                    Script script =
                            cx.compileString(
                                    "importClass(java.util.UUID);true", "TestScript", 1, null);

                    for (int i = 0; i < 3; i++) {
                        Scriptable scope = new ImporterTopLevel(cx);
                        scope.setPrototype(sharedScope);
                        scope.setParentScope(null);
                        script.exec(cx, scope);
                        assertEquals(UniqueTag.NOT_FOUND, sharedScope.get("UUID", sharedScope));
                        assertTrue(scope.get("UUID", scope) instanceof NativeJavaClass);
                    }
                    return null;
                });
    }

    private Object runScript(final String scriptSourceText) {

        return contextFactory.call(
                context -> {
                    Script script = context.compileString(scriptSourceText, "", 1, null);
                    Object exec = script.exec(context, global);
                    return exec;
                });
    }
}
