/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.shell.ShellContextFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mozilla.javascript.drivers.TestUtils.JS_FILE_FILTER;
import static org.mozilla.javascript.drivers.TestUtils.recursiveListFilesHelper;

@RunWith(Parameterized.class)
public class Test262SuiteTest {

    static final int[] OPT_LEVELS = {-1, 0, 9};

    static Map<Integer, Map<String, Script>> HARNESS_SCRIPT_CACHE = new HashMap<Integer, Map<String, Script>>();

    static ShellContextFactory CTX_FACTORY = new ShellContextFactory();

    @BeforeClass
    public static void setUpClass() throws Exception {
        HARNESS_SCRIPT_CACHE.put(-1, new HashMap<String, Script>());
        HARNESS_SCRIPT_CACHE.put(0, new HashMap<String, Script>());
        HARNESS_SCRIPT_CACHE.put(9, new HashMap<String, Script>());

        CTX_FACTORY.setLanguageVersion(Context.VERSION_ES6);
        TestUtils.setGlobalContextFactory(CTX_FACTORY);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TestUtils.setGlobalContextFactory(null);
    }

    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("\\s{1,4}!\\s*(.+)");

    private final String jsFilePath;
    private final String jsFileStr;
    private final int optLevel;
    private final boolean useStrict;
    private List<String> harnessFiles;

    public Test262SuiteTest(String jsFilePath, String jsFileStr, List<String> harnessFiles, int optLevel, boolean useStrict) {
        this.jsFilePath = jsFilePath;
        this.jsFileStr = jsFileStr;
        this.optLevel = optLevel;
        this.useStrict = useStrict;
        this.harnessFiles = harnessFiles;
    }

    private Object executeRhinoScript() {
        Context cx = Context.enter();

        try {
            cx.setOptimizationLevel(optLevel);

            Scriptable scope = cx.initStandardObjects();
            for (String harnessFile : harnessFiles) {
                if (!HARNESS_SCRIPT_CACHE.get(optLevel).containsKey(harnessFile)) {
                    HARNESS_SCRIPT_CACHE.get(optLevel).put(
                        harnessFile,
                        cx.compileReader(new FileReader("test262/harness/" + harnessFile), "test262/harness/" + harnessFile, 1, null)
                    );
                }
                HARNESS_SCRIPT_CACHE.get(optLevel).get(harnessFile).exec(cx, scope);
            }

            String str = jsFileStr;
            if (useStrict) { // taken from test262.py
                str = "\"use strict\";\nvar strict_mode = true;\n" + jsFileStr;
            }

            return cx.evaluateString(scope, str, jsFilePath.replaceAll("\\\\", "/"), 1, null);
        } catch (JavaScriptException ex) {
            fail(String.format("%s%n%s", ex.getMessage(), ex.getScriptStackTrace()));
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
    }

    private static final Yaml YAML = new Yaml();

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
                    }

                    if (curLine.isEmpty() || curLine.startsWith("#")) continue;

                    Matcher m = EXCLUDE_PATTERN.matcher(curLine);
                    if (m.matches()) {
                        Iterator<File> it = dirFiles.iterator();
                        while (it.hasNext()) {
                            String path = it.next().getPath().replaceAll("\\\\", "/");
                            if (path.contains(m.group(1))) it.remove();
                        }
                    } else {
                        testFiles.addAll(dirFiles);
                        dirFiles.clear();
                        file = new File(testDir, curLine);
                        if (file.isFile()) {
                            testFiles.add(file);
                            break;
                        } else if (file.isDirectory()) {
                            recursiveListFilesHelper(file, JS_FILE_FILTER, dirFiles);
                        }
                    }
                }
            }
        }
        scanner.close();

        return testFiles;
    }

    @Parameters(name = "js={0}, opt={3}, strict={4}")
    public static Collection<Object[]> test262SuiteValues() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        List<File> tests = getTestFiles();
        for (File jsTest : tests) {
            String jsFileStr = (String) SourceReader.readFileOrUrl(jsTest.getPath(), true, "UTF-8");
            List<String> harnessFiles = new ArrayList<String>();
            harnessFiles.add("sta.js");
            harnessFiles.add("assert.js");

            Map header = (Map) YAML.load(jsFileStr.substring(jsFileStr.indexOf("/*---") + 5, jsFileStr.lastIndexOf("---*/")));
            if (header.containsKey("includes")) {
                harnessFiles.addAll((Collection)header.get("includes"));
            }

            List<String> flags = header.containsKey("flags") ? (List<String>) header.get("flags") : Collections.EMPTY_LIST;
            for (int optLevel : OPT_LEVELS) {
                if (!flags.contains("onlyStrict")) {
                    result.add(new Object[]{jsTest.getPath(), jsFileStr, harnessFiles, optLevel, false});
                }
                if (!flags.contains("noStrict")) {
                    result.add(new Object[]{jsTest.getPath(), jsFileStr, harnessFiles, optLevel, true});
                }
            }
        }
        return result;
    }

    @Test
    public void test262() throws Exception {
        assertNotNull(executeRhinoScript());
    }
}
