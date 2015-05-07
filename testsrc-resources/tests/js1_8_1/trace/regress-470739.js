/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-470739.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 470739;
var summary = 'TM: never abort on ==';
var actual = '';
var expect = '';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  function loop()
  {
    var i;
    var start = new Date();

    for(i=0;i<500000;++i) { var r = (void 0) == null; }

    var stop = new Date();
    return stop - start;
  }

  jit(false);
  var timenonjit = loop();
  jit(true);
  var timejit = loop();
  jit(false);

  print('time: nonjit = ' + timenonjit + ', jit = ' + timejit);

  expect = true;
  actual = timejit < timenonjit/2;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
