/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.drivers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.io.*;

import static org.junit.Assert.*;

/**
 * This class is used for creating test scripts that are loaded from JS scripts. Each test must
 * be represented by a class that extends this class, and has a "RhinoTest" annotation at the
 * class level that returns the file name of the script to execute.
 */

@RunWith(BlockJUnit4ClassRunner.class)
public abstract class ScriptTestsBase {

    private Object executeRhinoScript(int optLevel) {
        RhinoTest anno = this.getClass().getAnnotation(RhinoTest.class);
        assertNotNull(anno);

        int jsVersion = Context.VERSION_1_8;
        LanguageVersion jsVersionAnnotation = this.getClass().getAnnotation(LanguageVersion.class);
        if (jsVersionAnnotation != null) {
            jsVersion = jsVersionAnnotation.value();
            Context.checkLanguageVersion(jsVersion);
        }

        Reader script = null;
        String suiteName = null;

        Context cx = Context.enter();
        try {
            if (!"".equals(anno.value())) {
                script = new InputStreamReader(new FileInputStream(anno.value()), "UTF-8");
                suiteName = anno.value();
            } else if (!"".equals(anno.inline())) {
                script = new StringReader("load('testsrc/assert.js');\n" + anno.inline() + "\n" + "'success';");
                suiteName = "inline.js";
            }

            cx.setOptimizationLevel(optLevel);
            cx.setLanguageVersion(jsVersion);

            Global global = new Global(cx);

            Scriptable scope = cx.newObject(global);
            scope.setPrototype(global);
            scope.setParentScope(null);

            return cx.evaluateReader(scope, script, suiteName, 1, null);
        } catch (JavaScriptException ex) {
            fail(String.format("%s%n%s", ex.getMessage(), ex.getScriptStackTrace()));
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
            try {
                if (null != script) script.close();
            } catch (IOException e) {
            }
        }
    }

    @Test
    public void rhinoTestNoOpt() {
        assertEquals("success", executeRhinoScript(-1));
    }

    @Test
    public void rhinoTestOpt0() {
        assertEquals("success", executeRhinoScript(0));
    }

    @Test
    public void rhinoTestOpt9() {
        assertEquals("success", executeRhinoScript(9));
    }
}
