/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-316885-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 316885;
var summary = 'Unrooted access in jsinterp.c';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

var str_with_num = "0.1";

var obj = {
  elem getter: function() {
    return str_with_num;
  },
  elem setter: function(value) {
    gc();
  }

};

expect = Number(str_with_num);
actual = obj.elem++;

gc();

 
reportCompare(expect, actual, summary);
