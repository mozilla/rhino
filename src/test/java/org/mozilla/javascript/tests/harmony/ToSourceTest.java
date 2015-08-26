/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.harmony;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;


public class ToSourceTest {

  private void assertSource(String source, String expectedOutput) {
    CompilerEnvirons env = new CompilerEnvirons();
    env.setLanguageVersion(Context.VERSION_ES6);
    Parser parser = new Parser(env);
    AstRoot root = parser.parse(source, null, 0);
    Assert.assertEquals(expectedOutput, root.toSource());
  }


  /**
   * Tests that var declaration AST nodes is properly decompiled.
   */
  @Test
  public void testArrowFunctionToSource() {
    assertSource("var a3 = a.map(s => s.length);", "var a3 = a.map(s => s.length);\n");
  }

}
