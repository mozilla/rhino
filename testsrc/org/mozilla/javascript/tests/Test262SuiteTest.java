/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.shell.Global;

import java.io.*;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mozilla.javascript.drivers.TestUtils.JS_FILE_FILTER;
import static org.mozilla.javascript.drivers.TestUtils.recursiveListFilesHelper;

@RunWith(Parameterized.class)
public class Test262SuiteTest {

    private final File jsFile;
    private final int optLevel;
    private final boolean isStrict;

    static final int[] OPT_LEVELS = {-1, 0, 9};

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    public Test262SuiteTest(File jsFile, int optLevel, boolean isStrict) {
        this.jsFile = jsFile;
        this.optLevel = optLevel;
        this.isStrict = isStrict;
    }

    private Object executeRhinoScript(File jsFile, int optLevel, boolean isStrict) {
        Context cx = Context.enter();

        try {
            List<String> harnessFiles = new ArrayList<String>();
            harnessFiles.add("sta.js");
            harnessFiles.add("assert.js");

            String jsFileStr = (String) SourceReader.readFileOrUrl(jsFile.getPath(), true, "UTF-8");
            Scanner scanner = new Scanner(new StringReader(jsFileStr));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("includes: ")) {
                    harnessFiles.addAll(Arrays.asList(line.substring(line.indexOf('[') + 1, line.lastIndexOf("]")).split(",")));
                }
            }
            scanner.close();

            cx.setOptimizationLevel(optLevel);
            cx.setLanguageVersion(Context.VERSION_ES6);

            Global global = new Global(cx);
            for (String harnessFile : harnessFiles) {
                cx.evaluateReader(global, new FileReader("test262/harness/" + harnessFile), "test262/harness/" + harnessFile, 1, null);
            }

            Scriptable scope = cx.newObject(global);
            scope.setPrototype(global);
            scope.setParentScope(null);

            return cx.evaluateString(scope, jsFileStr, jsFile.getPath().replaceAll("\\\\", "/"), 1, null);
        } catch (JavaScriptException ex) {
            fail(String.format("%s%n%s", ex.getMessage(), ex.getScriptStackTrace()));
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
    }

    public static List<File> getTestFiles() throws IOException {
        File testDir = new File("test262/test");

        List<File> testFiles = new LinkedList<File>();

        List<File> dirFiles = new LinkedList<File>();

        Scanner scanner = new Scanner(new File("testsrc/test262.properties"));

        String curLine = "", nxtLine = "";

        while (true) {
            curLine = nxtLine;
            nxtLine = scanner.hasNextLine() ? scanner.nextLine() : null;

            if (curLine == null) break;

            if (curLine.isEmpty() || curLine.startsWith("#")) continue;

            File file = new File(testDir, curLine);

            if (file.isFile()) {
                testFiles.add(file);
            } else if (file.isDirectory()) {
                recursiveListFilesHelper(file, JS_FILE_FILTER, dirFiles);

                while (true) {
                    curLine = nxtLine;
                    nxtLine = scanner.hasNextLine() ? scanner.nextLine() : null;

                    if (curLine == null) {
                        testFiles.addAll(dirFiles);
                        break;
                    };

                    Matcher m = EXCLUDE_PATTERN.matcher(curLine);
                    if (m.matches()) {
                        dirFiles.remove(new File(file, m.group(1)));
                    } else if (nxtLine == null || !nxtLine.matches(EXCLUDE_PATTERN.pattern())) {
                        testFiles.addAll(dirFiles);
                        dirFiles.clear();
                        nxtLine = curLine;
                        break;
                    }
                }
            }
        }
        scanner.close();

        //TODO Arrays.sort(cfgLines);
        return testFiles;
    }

    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("\\s{1,4}!\\s*(.+)");

    private static final Pattern HARNESS_INCLUDES_PATTERN = Pattern.compile("^.*(includes:\\s\\[(.*)\\]).*$");

    public static String loadFile(File f) throws IOException {
        int length = (int) f.length(); // don't worry about very long files
        char[] buf = new char[length];
        new FileReader(f).read(buf, 0, length);
        return new String(buf);
    }

    @Parameters(name = "{index}, js={0}, opt={1}, strict={2}")
    public static Collection<Object[]> test262SuiteValues() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        List<File> tests = getTestFiles();
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
