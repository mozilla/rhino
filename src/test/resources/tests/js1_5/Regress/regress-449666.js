/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-449666.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 449666;
var summary = 'Do not assert: JSSTRING_IS_FLAT(str_)';
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

  var global;

  jit(true);

  if (typeof window == 'undefined') {
    global = this;
  }
  else {
    global = window;
  }

  if (!global['g']) {
    global['g'] = {};
  }

  if (!global['g']['l']) {
    global['g']['l'] = {};
    (function() {
      function k(a,b){
        var c=a.split(/\./);
        var d=global;
        for(var e=0;e<c.length-1;e++){
          if(!d[c[e]]){
            d[c[e]]={};
          }
          d=d[c[e]];
        }
        d[c[c.length-1]]=b;
        print("hi");
      }

      function T(a){return "hmm"}
      k("g.l.loaded",T);
    })();

  }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
