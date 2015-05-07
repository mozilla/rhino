/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'fe-001.js';

if (1) function f() {return 1;}
if (0) function f() {return 0;}

function test()
{
  enterFunc ("test");

  printStatus ("Function Expression Statements basic test.");
   
  reportCompare (1, f(), "Both functions were defined.");
   
  exitFunc ("test");
}

test();
