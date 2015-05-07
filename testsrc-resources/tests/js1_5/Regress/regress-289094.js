/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-289094.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 289094;
var summary = 'argument shadowing function property special case for lambdas';
var actual = '';
var expect = 'function:function';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
function fn()
{
  var o = {
    d: function(x,y) {},
    init: function() { this.d.x = function(x) {}; this.d.y = function(y) {}; }
  };
  o.init();
  actual = typeof(o.d.x) + ':' + typeof(o.d.y);
}

fn();

reportCompare(expect, actual, summary);
