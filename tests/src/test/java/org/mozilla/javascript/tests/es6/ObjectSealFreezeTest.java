/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

public class ObjectSealFreezeTest {

    @Test
    public void sealWriteToExistingWritableProperty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  var r = {};"
                                            + "  Object.defineProperties(r, { a: { writable: true, value: 'abc' } });"
                                            + "  Object.seal(r);"
                                            + "  r.a = 'Rhino';"
                                            + "  return r.a;"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("Rhino", result);

                    return null;
                });
    }

    @Test
    public void sealWriteToExistingWritablePropertyStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  'use strict';"
                                            + "  var r = {};"
                                            + "  Object.defineProperties(r, { a: { writable: true, value: 'abc' } });"
                                            + "  Object.seal(r);"
                                            + "  r.a='Rhino';"
                                            + "  return r.a;"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("Rhino", result);

                    return null;
                });
    }

    @Test
    public void sealWriteToExistingSymbolProperty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  var sym = Symbol('X');"
                                            + "  var r = {};"
                                            + "  r[sym] = 'abc';"
                                            + "  Object.seal(r);"
                                            + "  r[sym] = 'Rhino';"
                                            + "  return r[sym];"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("Rhino", result);

                    return null;
                });
    }

    @Test
    public void sealWriteToExistingSymbolPropertyStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  'use strict';"
                                            + "  var sym = Symbol('X');"
                                            + "  var r = {};"
                                            + "  r[sym] = 'abc';"
                                            + "  Object.seal(r);"
                                            + "  r[sym] = 'Rhino';"
                                            + "  return r[sym];"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("Rhino", result);

                    return null;
                });
    }

    @Test
    public void freezeWriteToExistingWritableProperty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  var r = {};"
                                            + "  Object.defineProperties(r, { a: { writable: true, value: 'abc' } });"
                                            + "  Object.freeze(r);"
                                            + "  r.a = 'Rhino';"
                                            + "  return r.a;"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("abc", result);

                    return null;
                });
    }

    @Test
    public void freezeWriteToExistingWritablePropertyStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  'use strict';"
                                            + "  var r = {};"
                                            + "  Object.defineProperties(r, { a: { writable: true, value: 'abc' } });"
                                            + "  Object.freeze(r);"
                                            + "  r.a='Rhino';"
                                            + "  return r.a;"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals(
                            "Cannot add properties to this object because extensible is false.",
                            result);

                    return null;
                });
    }

    @Test
    public void freezeWriteToExistingSymbolProperty() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  var sym = Symbol('X');"
                                            + "  var r = {};"
                                            + "  r[sym] = 'abc';"
                                            + "  Object.freeze(r);"
                                            + "  r[sym] = 'Rhino';"
                                            + "  return r[sym];"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals("abc", result);

                    return null;
                });
    }

    @Test
    public void freezeWriteToExistingSymbolPropertyStrict() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  'use strict';"
                                            + "  var sym = Symbol('X');"
                                            + "  var r = {};"
                                            + "  r[sym] = 'abc';"
                                            + "  Object.freeze(r);"
                                            + "  r[sym] = 'Rhino';"
                                            + "  return r[sym];"
                                            + "};"
                                            + "try { "
                                            + "  foo();"
                                            + "} catch (e) { e.message }",
                                    "test",
                                    1,
                                    null);
                    assertEquals(
                            "Cannot add properties to this object because extensible is false.",
                            result);

                    return null;
                });
    }

    @Test
    public void objectConstructorForNonExtensibleFunctions() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "foo = function() {"
                                            + "  var res = '';\n"
                                            + "  var a = JSON.stringify;\n"
                                            + "  Object.preventExtensions(a);\n"
                                            + "  res += 'a.isExtensible = ' + Object.isExtensible(a);\n"
                                            + "  res += '\\n';\n"
                                            + "  var b = Object(a);\n"
                                            + "  res += typeof b;\n"
                                            + "  res += '\\n';\n"
                                            + "  res += a===b;\n"
                                            + "  res += '\\n';\n"
                                            + "  res += 'b.isExtensible = ' + Object.isExtensible(b);\n"
                                            + "  return res;\n"
                                            + "};"
                                            + "  foo();",
                                    "test",
                                    1,
                                    null);
                    assertEquals(
                            "a.isExtensible = false\nfunction\ntrue\nb.isExtensible = false" + "",
                            result);

                    return null;
                });
    }
}
