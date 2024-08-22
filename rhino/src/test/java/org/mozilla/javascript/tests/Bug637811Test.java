/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/** @author André Bargull */
public class Bug637811Test {

    private Context cx;

    @Before
    public void setUp() throws Exception {
        cx =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        switch (featureIndex) {
                            case Context.FEATURE_STRICT_MODE:
                            case Context.FEATURE_WARNING_AS_ERROR:
                                return true;
                        }
                        return super.hasFeature(cx, featureIndex);
                    }
                }.enterContext();
    }

    @After
    public void tearDown() throws Exception {
        Context.exit();
    }

    @Test
    public void test() {
        String source = "";
        source += "var x = 0;";
        source += "bar: while (x < 0) { x = x + 1; }";
        cx.compileString(source, "", 1, null);
    }
}
