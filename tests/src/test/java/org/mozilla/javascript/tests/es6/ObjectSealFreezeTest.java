/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class ObjectSealFreezeTest {

    @Test
    public void sealWriteToExistingWritableProperty() {
        final String script =
                "foo = function() {"
                        + "  var r = {};"
                        + "  Object.defineProperties(r, { a: { writable: true, value: 'abc' } });"
                        + "  Object.seal(r);"
                        + "  r.a = 'Rhino';"
                        + "  return r.a;"
                        + "};"
                        + "try { "
                        + "  foo();"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("Rhino", script);
    }

    @Test
    public void sealWriteToExistingWritablePropertyStrict() {
        final String script =
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
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("Rhino", script);
    }

    @Test
    public void sealWriteToExistingSymbolProperty() {
        final String script =
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
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("Rhino", script);
    }

    @Test
    public void sealWriteToExistingSymbolPropertyStrict() {
        final String script =
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
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("Rhino", script);
    }

    @Test
    public void freezeWriteToExistingWritableProperty() {
        final String script =
                "foo = function() {"
                        + "  var r = {};"
                        + "  Object.defineProperties(r, { a: { writable: true, value: 'abc' } });"
                        + "  Object.freeze(r);"
                        + "  r.a = 'Rhino';"
                        + "  return r.a;"
                        + "};"
                        + "try { "
                        + "  foo();"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("abc", script);
    }

    @Test
    public void freezeWriteToExistingWritablePropertyStrict() {
        final String script =
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
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6(
                "Cannot add properties to this object because extensible is false.", script);
    }

    @Test
    public void freezeWriteToExistingSymbolProperty() {
        final String script =
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
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6("abc", script);
    }

    @Test
    public void freezeWriteToExistingSymbolPropertyStrict() {
        final String script =
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
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_ES6(
                "Cannot add properties to this object because extensible is false.", script);
    }

    @Test
    public void objectConstructorForNonExtensibleFunctions() {
        final String script =
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
                        + "  foo();";
        Utils.assertWithAllModes_ES6(
                "a.isExtensible = false\nfunction\ntrue\nb.isExtensible = false", script);
    }
}
