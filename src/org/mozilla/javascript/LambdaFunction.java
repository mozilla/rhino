/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements a single JavaScript function that has the prototype of the built-in
 * Function class, and which is implemented using a single function that can easily be implemented
 * using a lambda expression.
 */
public class LambdaFunction
    extends ScriptableObject
    implements Callable {

  private final Callable target;
  private final String name;

  /**
   * Create a new function. The new object will have the Function prototype and no parent. The
   * caller is responsible for binding this object to the appropriate scope.
   */
  public LambdaFunction(String name, int length, Callable target) {
    this.target = target;
    this.name = name;

    defineProperty("length", length, DONTENUM | PERMANENT);
    if (name != null) {
      defineProperty("name", name, DONTENUM | PERMANENT);
    }
  }

  @Override
  public String getClassName() {
    return (name == null ? "Function" : name);
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return target.call(cx, scope, thisObj, args);
  }

  @Override
  public String getTypeOf() {
    return "function";
  }
}
