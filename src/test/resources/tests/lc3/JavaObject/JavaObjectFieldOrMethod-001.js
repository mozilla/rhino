/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'JavaObjectFieldOrMethod-001.js';

var SECTION = "JavaObject Field or method access";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

new TestCase(
  "dt.amIAFieldOrAMethod",
  String(dt.amIAFieldOrAMethod),
  "FIELD!" );

new TestCase(
  "dt.amIAFieldOrAMethod()",
  String(dt.amIAFieldOrAMethod()),
  "METHOD!" );

test();

