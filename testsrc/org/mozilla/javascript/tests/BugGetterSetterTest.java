/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

public class BugGetterSetterTest {
    private CompilerEnvirons environment = new CompilerEnvirons();

    @Before
    public void setUp() throws Exception {
        environment.setLanguageVersion(180);
        environment.setStrictMode(false);
    }

    @Test
    public void testNodeReplacementInWhileLoopWithBrackets() throws IOException {
        String script = "var o = {\n" +
                "  _x: 123, \n" +
                "  get x() {\n" +
                "    return this._x;\n" +
                "  }\n" +
                ", \n" +
                "  set x(value) {\n" +
                "    this._x = value;\n" +
                "  }\n" +
                "};\n";

        Parser parser = new Parser(environment);
        AstRoot astRoot = parser.parse(script, null, 1);
        assertEquals(script, astRoot.toSource());
    }
}
