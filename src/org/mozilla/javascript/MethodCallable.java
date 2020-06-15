/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This interface is used to make it easier to write lambdas that operate on methods on
 * Java classes.
 */
public interface MethodCallable<T> {
  Object invoke(Context cx, Scriptable scope, T thisObj, Object[] args);
}
