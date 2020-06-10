/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements a JavaScript function that may be used as a constructor by delegating to an
 * interface that can be easily implemented as a lambda. The LambdaFunction class may be used to add
 * functions to the prototype that are also implemented as lambdas.
 */
public class LambdaConstructor
    extends ScriptableObject
    implements Function {

  private final Constructable target;
  private final String name;

  /**
   * Create a new function. The new object will have the Function prototype and no parent. The
   * caller is responsible for binding this object to the appropriate scope.
   */
  public LambdaConstructor(Scriptable scope, String name, int length, Constructable target) {
    this.target = target;
    this.name = name;

    defineProperty("length", length, DONTENUM | PERMANENT);
    if (name != null) {
      defineProperty("name", name, DONTENUM | PERMANENT);
    }

    setPrototype(ScriptableObject.getFunctionPrototype(scope));
    defineProperty("prototype", getPrototype(), DONTENUM | PERMANENT);

    setParentScope(scope);
  }

  @Override
  public String getClassName() {
    return (name == null ? "Function" : name);
  }

  @Override
  public String getTypeOf() {
    return "function";
  }

  @Override
  public boolean hasInstance(Scriptable instance)
  {
    return ScriptRuntime.jsDelegatesTo(instance, getPrototype());
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return target.construct(cx, scope, args);
  }

  @Override
  public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
    Scriptable obj = target.construct(cx, scope, args);
    obj.setPrototype(getPrototype());
    obj.setParentScope(scope);
    return obj;
  }

  public void definePrototypeMethod(String name, int length, Callable target) {
    LambdaFunction f = new LambdaFunction(name, length, target);
    ScriptableObject.putProperty(getPrototype(), name, f);
  }

  public void defineConstructorMethod(String name, int length, Callable target) {
    LambdaFunction f = new LambdaFunction(name, length, target);
    ScriptableObject.putProperty(this, name, f);
  }

  @Override
  public void optimizeStorage() {
    ((ScriptableObject)getPrototype()).optimizeStorage();
    super.optimizeStorage();
  }
}
