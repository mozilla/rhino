/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-476414-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 476414;
var summary = 'Do not crash @ GetGCThingFlags';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

function whatToTestSpidermonkeyTrunk(code)
{
  return {
  allowExec: true
      };
}
whatToTest = whatToTestSpidermonkeyTrunk;
function tryItOut(code)
{
  var wtt = whatToTest(code.replace(/\n/g, " ").replace(/\r/g, " "));
  var f = new Function(code);
  if (wtt.allowExec && f) {
    rv = tryRunning(f, code);
  }
}
function tryRunning(f, code)
{
  try { 
    var rv = f();
  } catch(runError) {}
}
var realFunction = Function;
var realUneval = uneval;
var realToString = toString;
var realToSource = toSource;
function tryEnsureSanity()
{
  delete Function;
  delete uneval;
  delete toSource;
  delete toString;
  Function = realFunction;
  uneval = realUneval;
  toSource = realToSource;
  toString = realToString;
}
for (let iters = 0; iters < 2000; ++iters) { 
  count=27745; tryItOut("with({x: (c) = (x2 = [])})false;");
  tryEnsureSanity();
  count=35594; tryItOut("switch(null) { case this.__defineSetter__(\"window\", function () { yield  \"\"  } ): break; }");
  tryEnsureSanity();
  count=45020; tryItOut("with({}) { (this.__defineGetter__(\"x\", function (y)this)); } ");
  tryEnsureSanity();
  count=45197; tryItOut("M:with((p={}, (p.z = <x/> ===  '' )()))/*TUUL*/for each (let y in [true, {}, {}, (void 0), true, true, true, (void 0), true, (void 0)]) { return; }");
  tryEnsureSanity();
  gc();
  tryEnsureSanity();
  count=45254; tryItOut("for each (NaN in this);");
  tryEnsureSanity();
}
 
reportCompare(expect, actual, summary);
