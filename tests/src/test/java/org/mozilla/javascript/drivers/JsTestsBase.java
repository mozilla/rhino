/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.drivers;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.junit.BeforeClass;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public abstract class JsTestsBase {
    private int optimizationLevel;

    private static ContextFactory threadSafeFactory;

    @BeforeClass
    public static void init() {
        threadSafeFactory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        if (featureIndex == Context.FEATURE_THREAD_SAFE_OBJECTS) {
                            return true;
                        }
                        return super.hasFeature(cx, featureIndex);
                    }
                };
    }

    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    }

    public void runJsTest(Context cx, Scriptable shared, String name, String source) {
        // create a lightweight top-level scope
        Scriptable scope = cx.newObject(shared);
        scope.setPrototype(shared);
        Object result;
        try {
            result = cx.evaluateString(scope, source, "jstest input: " + name, 1, null);
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            System.out.println("FAILED");
            throw e;
        }
        assertTrue(result != null);
        assertTrue("success".equals(result));
    }

    public void runJsTests(File[] tests) throws IOException {
        try (Context cx = threadSafeFactory.enterContext()) {
            cx.setOptimizationLevel(this.optimizationLevel);
            Scriptable shared = cx.initStandardObjects();
            for (File f : tests) {
                int length = (int) f.length(); // don't worry about very long
                // files
                char[] buf = new char[length];
                new FileReader(f).read(buf, 0, length);
                String session = new String(buf);
                runJsTest(cx, shared, f.getName(), session);
            }
        }
    }
}
