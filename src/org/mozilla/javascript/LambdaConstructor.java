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

  /** If this flag is set, the constructor may be invoked as an ordinary function */
  public static final int CONSTRUCTOR_FUNCTION = 1 << 0;
  /** If this flag is set, the constructor may be invoked using "new" */
  public static final int CONSTRUCTOR_NEW = 1 << 1;
  /** By default, the constructor may be invoked either way */
  public static final int CONSTRUCTOR_DEFAULT = CONSTRUCTOR_FUNCTION | CONSTRUCTOR_NEW;

  private final Constructable targetConstructor;
  private final int flags;

  /**
   * Create a new function. The new object will have the Function prototype and no parent. The
   * caller is responsible for binding this object to the appropriate scope.
   */
  public LambdaConstructor(Scriptable scope, String name, int length, Constructable target) {
    super(scope, name, length, null);
    this.targetConstructor = target;
    this.flags = CONSTRUCTOR_DEFAULT;
  }

  /**
   * Create a new function and control whether it may be invoked using new, as a function,
   * or both.
   */
  public LambdaConstructor(Scriptable scope, String name, int length, int flags,
      Constructable target) {
    super(scope, name, length, null);
    this.targetConstructor = target;
    this.flags = flags;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if ((flags & CONSTRUCTOR_FUNCTION) == 0) {
      throw ScriptRuntime.typeError1("msg.constructor.no.function", getFunctionName());
    }
    return targetConstructor.construct(cx, scope, args);
  }

  @Override
  public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
    if ((flags & CONSTRUCTOR_NEW) == 0) {
      throw ScriptRuntime.typeError1("msg.no.new", getFunctionName());
    }
    Scriptable obj = targetConstructor.construct(cx, scope, args);
    obj.setPrototype(getClassPrototype());
    obj.setParentScope(scope);
    return obj;
  }

  public void definePrototypeMethod(Scriptable scope, String name, int length, Callable target) {
    LambdaFunction f = new LambdaFunction(scope, name, length, target);
    ScriptableObject.putProperty(getPrototypeScriptable(), name, f);
  }

  public void definePrototypeProperty(String name, Object value, int attributes) {
    ScriptableObject.defineProperty(getPrototypeScriptable(), name, value, attributes);
  }

  public void definePrototypeProperty(Symbol key, Object value, int attributes) {
    ScriptableObject.defineProperty(getPrototypeScriptable(), key, value, attributes);
  }

  public void defineConstructorMethod(Scriptable scope, String name, int length, Callable target) {
    LambdaFunction f = new LambdaFunction(scope, name, length, target);
    ScriptableObject.defineProperty(this, name, f, DONTENUM);
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
