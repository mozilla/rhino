/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-238945.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 238945;
var summary = '7.9.1 Automatic Semicolon insertion - allow token following do{}while()';
var actual = 'error';
var expect = 'no error';

printBugNumber(BUGNUMBER);
printStatus (summary);

function f(){do;while(0)return}f()
 
  actual = 'no error'
  reportCompare(expect, actual, summary);
