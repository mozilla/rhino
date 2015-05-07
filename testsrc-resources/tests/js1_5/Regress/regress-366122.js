/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-366122.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 366122;
var summary = 'Compile large script';
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
    var code = "", obj = {};
    for(var i = 0; i < 0x10000; i++) {
      if(i == 10242) {
        code += "void 0x10000050505050;\n";
      } else {
        code += "void 'x" + i + "';\n";
      }
    }
    code += "export undefined;\n";
    code += "void 125;\n";
    eval(code);
  }
  try
  {
    exploit();
  }
  catch(ex)
  {
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
