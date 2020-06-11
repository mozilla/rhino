/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.engine;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.script.ScriptContext;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * <p>
 * This class defines the following built-in functions for the RhinoScriptEngine.
 * </p>
 * <ul>
 * <li>print(arg, arg, ...): Write each argument, concatenated to the ScriptEngine's
 * "standard output" as a string.</li>
 * </ul>
 */
public class Builtins {

  void register(Context cx, ScriptableObject scope, ScriptContext sc) {
    Writer stdout;
    if (sc.getWriter() == null) {
      stdout = new OutputStreamWriter(System.out);
    } else {
      stdout = sc.getWriter();
    }

    Callable printFunc = new LambdaFunction("print", 1,
        (Context lcx, Scriptable lscope, Scriptable lthis, Object[] args) ->
            print(lcx, lscope, stdout, args)
    );
    ScriptableObject.putProperty(scope, "print", printFunc);
  }

  private Object print(Context cx, Scriptable scope, Writer writer, Object[] args) {
    try {
      for (Object arg : args) {
        writer.write(ScriptRuntime.toString(arg));
      }
      writer.write('\n');
      return Undefined.instance;
    } catch (IOException ioe) {
      throw ScriptRuntime.throwCustomError(cx, scope, "Error", ioe.getMessage());
    }
  }
}
