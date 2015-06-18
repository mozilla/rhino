/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.io.*;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class Test262SuiteTest {

    private final File jsFile;
    private final int optLevel;
    private final boolean isStrict;

    static final int[] OPT_LEVELS = { -1, 0, 9 };

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("user.language", "US");
        System.setProperty("user.country", "en");
    }

    public Test262SuiteTest(File jsFile, int optLevel, boolean isStrict) {
        this.jsFile = jsFile;
        this.optLevel = optLevel;
        this.isStrict = isStrict;
    }

    private Object executeRhinoScript(File jsFile, int optLevel, boolean isStrict) {
        Reader jsFileReader = null;
        Context cx = Context.enter();

        try {
            jsFileReader = new FileReader(jsFile);

            cx.setOptimizationLevel(optLevel);
            cx.setLanguageVersion(Context.VERSION_ES6);

            Global global = new Global(cx);

            cx.evaluateReader(global, new FileReader("test262/harness/sta.js"), "test262/harness/sta.js", 1, null);
            cx.evaluateReader(global, new FileReader("test262/harness/assert.js"), "test262/harness/assert.js", 1, null);
            cx.evaluateReader(global, new FileReader("test262/harness/propertyHelper.js"), "test262/harness/propertyHelper.js", 1, null);

            Scriptable scope = cx.newObject(global);
            scope.setPrototype(global);
            scope.setParentScope(null);

            return cx.evaluateReader(scope, jsFileReader, jsFile.getPath().replaceAll("\\\\", "/"), 1, null);
        } catch (JavaScriptException ex) {
            fail(String.format("%s%n%s", ex.getMessage(), ex.getScriptStackTrace()));
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
            try {
                if (null != jsFileReader) jsFileReader.close();
            } catch (IOException e) {
            }
        }
    }

    public static File[] getTestFiles() throws IOException {
//        File testDir = getTestDir();
//        String[] tests = TestUtils.loadTestsFromResource("/" + getTestFilename(optLevel), null);
//        Arrays.sort(tests);
//        File[] files = new File[tests.length];
//        for (int i=0; i < files.length; i++) {
//            files[i] = new File(testDir, tests[i]);
//        }
//        return files;
        return new File[]{new File("test262/test/built-ins/Set/name.js")};
    }

    public static String loadFile(File f) throws IOException {
        int length = (int) f.length(); // don't worry about very long files
        char[] buf = new char[length];
        new FileReader(f).read(buf, 0, length);
        return new String(buf);
    }

    @Parameters(name = "{index}, js={0}, opt={1}, strict={2}")
    public static Collection<Object[]> test262SuiteValues() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        File[] tests = getTestFiles();
        for (File jsTest : tests) {
            for (int optLevel : OPT_LEVELS) {
                result.add(new Object[]{jsTest, optLevel, false});
                result.add(new Object[]{jsTest, optLevel, true});
            }
        }
        return result;
    }

    @Test
    public void test262() throws Exception {
        assertNotNull(executeRhinoScript(jsFile, optLevel, isStrict));
    }
}
