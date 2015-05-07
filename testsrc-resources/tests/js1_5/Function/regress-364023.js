/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-364023.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 364023;
var summary = 'Do not crash in JS_GetPrivate';
var actual = 'No Crash';
var expect = 'No Crash';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  function exploit() {
    var code = "";
    for(var i = 0; i < 0x10000; i++) {
      if(i == 125) {
        code += "void 0x10000050505050;\n";
      } else {
        code += "void " + (0x10000000000000 + i) + ";\n";
      }
    }
    code += "function foo() {}\n";
    eval(code);
  }
  exploit();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
