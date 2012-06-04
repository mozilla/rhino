/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;
import org.mozilla.javascript.*;
import java.util.Collections;
import java.util.Map;

public class Evaluator {

  public static Object eval(String source) {
    return eval(source, null);
  }

  public static Object eval(String source, String id, Scriptable object) {
    return eval(source, Collections.singletonMap(id, object));
  }

  public static Object eval(String source, Map<String, Scriptable> bindings) {
    Context cx = ContextFactory.getGlobal().enterContext();
    try {
      Scriptable scope = cx.initStandardObjects();
      if (bindings != null) {
          for (Map.Entry<String, Scriptable> entry : bindings.entrySet()) {
            final Scriptable object = entry.getValue();
            object.setParentScope(scope);
            scope.put(entry.getKey(), scope, object);
          }
      }
      return cx.evaluateString(scope, source, "source", 1, null);
    } finally {
      Context.exit();
    }
  }
}
