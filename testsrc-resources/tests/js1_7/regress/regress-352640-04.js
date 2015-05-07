/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-352640-04.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 352640;
var summary = 'Do not assert: scopeStmt or crash @ js_LexicalLookup';
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
 
  try
  {
    new Function("switch(w) { case 2: with({}) let y; case 3: }");
  }
  catch(ex)
  {
    print(ex + '');
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
