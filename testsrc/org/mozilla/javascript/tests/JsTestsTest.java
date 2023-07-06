/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.junit.Test;
import org.mozilla.javascript.drivers.JsTestsBase;
import org.mozilla.javascript.drivers.TestUtils;

public class JsTestsTest extends JsTestsBase {
    static final String baseDirectory = "testsrc" + File.separator + "jstests";

    static final String jstestsExtension = ".jstest";

    public void runJsTests() throws IOException {
        File[] tests =
                TestUtils.recursiveListFiles(
                        new File(baseDirectory),
                        new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                return f.getName().endsWith(jstestsExtension);
                            }
                        });
        runJsTests(tests);
    }

    @Test
    public void jsTestsInterpreted() throws IOException {
        setOptimizationLevel(-1);
        runJsTests();
    }

    @Test
    public void jsTestsCompiled() throws IOException {
        setOptimizationLevel(0);
        runJsTests();
    }

    @Test
    public void jsTestsOptimized() throws IOException {
        setOptimizationLevel(9);
        runJsTests();
    }
}
