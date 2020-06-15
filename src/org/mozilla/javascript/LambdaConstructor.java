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
    extends LambdaFunction {

  private final Constructable targetConstructor;

  /**
   * Create a new function. The new object will have the Function prototype and no parent. The
   * caller is responsible for binding this object to the appropriate scope.
   */
  public LambdaConstructor(Scriptable scope, String name, int length, Constructable target) {
    super(scope, name, length, null);
    this.targetConstructor = target;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return targetConstructor.construct(cx, scope, args);
  }

  @Override
  public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
    Scriptable obj = targetConstructor.construct(cx, scope, args);
    obj.setPrototype(getClassPrototype());
    obj.setParentScope(scope);
    return obj;
  }

  public void definePrototypeMethod(Scriptable scope, String name, int length, Callable target) {
    LambdaFunction f = new LambdaFunction(scope, name, length, target);
    ScriptableObject.putProperty(getPrototypeScriptable(), name, f);
  }

  public void defineConstructorMethod(Scriptable scope, String name, int length, Callable target) {
    LambdaFunction f = new LambdaFunction(scope, name, length, target);
    ScriptableObject.putProperty(this, name, f);
  }

  public static <T> T convertThisObject(Scriptable thisObj, Class<T> targetClass) {
    if (!targetClass.isInstance(thisObj)) {
      throw ScriptRuntime.typeError0("msg.this.not.instance");
    }
    return (T)thisObj;
  }

  private Scriptable getPrototypeScriptable() {
    Object prop = getPrototypeProperty();
    if (!(prop instanceof Scriptable)) {
      throw ScriptRuntime.typeError("Not properly a lambda constructor");
    }
    return (Scriptable)prop;
  }

  @Override
  public void optimizeStorage() {
    if (getPrototype() != null) {
      ((ScriptableObject) getPrototype()).optimizeStorage();
    }
    super.optimizeStorage();
  }
}
