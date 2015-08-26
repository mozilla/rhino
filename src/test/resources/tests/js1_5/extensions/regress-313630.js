/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-313630.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 313630;
var summary = 'Root access in js_fun_toString';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

var f = Function("return 1");
Function("return 2");
expect = f.toSource(0);

var likeFunction = {
  valueOf: function() {
    var tmp = f;
    f = null;
    return tmp;
  },
  __proto__: Function.prototype
};

var likeNumber = {
  valueOf: function() {
    gc();
    return 0;
  }
};

var actual = likeFunction.toSource(likeNumber);
printStatus(expect === actual);

reportCompare(expect, actual, summary);
