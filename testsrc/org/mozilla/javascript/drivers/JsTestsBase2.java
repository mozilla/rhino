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

import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(BlockJUnit4ClassRunner.class)
public abstract class JsTestsBase2 {

  private Object executeRhinoScript(int optLevel) {
    String suiteName = this.getClass().getAnnotation(RhinoTest.class).value();

    FileReader script = null;
    Context cx = Context.enter();
    try {
      script = new FileReader(suiteName);
      cx.setOptimizationLevel(optLevel);

      Global global = new Global(cx);

      Scriptable scope = cx.newObject(global);
      scope.setPrototype(global);
      scope.setParentScope(null);

      return cx.evaluateReader(scope, script, suiteName, 1, null);
    } catch (JavaScriptException ex) {
      fail(ex.getScriptStackTrace());
      return null;
    }catch (Exception e) {
      fail(e.getMessage());
      return null;
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
