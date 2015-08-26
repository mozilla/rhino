/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.drivers.JsTestsBase;

public class JsTestsTest extends JsTestsBase {

    public void runJsTests() throws IOException {
        File[] tests = TestUtils.recursiveListFiles(new File("src/test/resources/jstests"),
                new FileFilter() {
                  public boolean accept(File f) {
                      return f.getName().endsWith(".jstest");
                  }
                });
        runJsTests(tests);
    }

    public void testJsTestsInterpreted() throws IOException {
        setOptimizationLevel(-1);
        runJsTests();
    }

    public void testJsTestsCompiled() throws IOException {
        setOptimizationLevel(0);
        runJsTests();
    }

    public void testJsTestsOptimized() throws IOException {
        setOptimizationLevel(9);
        runJsTests();
    }
}
