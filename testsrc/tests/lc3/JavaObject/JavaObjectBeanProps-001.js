/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'JavaObjectBeanProps-001.js';

var SECTION = "JavaObject Field or method access";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = [
  "boolean",
  "byte",
  "integer",
  "double",
  "float",
  "short",
  "char"
  ];

var v = [
  true,
  1,
  2,
  3.0,
  4.0,
  5,
  'a'.charCodeAt(0)
  ];

for (var i=0; i < a.length; i++) {
  var name = a[i];
  var getterName = "get" + a[i].charAt(0).toUpperCase() +
    a[i].substring(1);
  var setterName = "set" + a[i].charAt(0).toUpperCase() +
    a[i].substring(1);
  new TestCase(
    "dt['" + name + "'] == dt." + getterName + "()",
    dt[name],
    dt[getterName]() );

  dt[name] = v[i];
  new TestCase(
    "dt['" + name + "'] = "+ v[i] +"; dt." + getterName + "() == " + v[i],
    dt[getterName](),
    v[i]);
}

test();

