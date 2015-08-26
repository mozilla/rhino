/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-319872.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 319872;
var summary = 'Do not Crash in jsxml.c';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
START(summary);
printStatus ("Expect either no error, out of memory or catchable script stack " + 
             "space quota is exhausted error");
expectExitCode(0);
expectExitCode(3);
expectExitCode(5);

try
{
  var i,m,str;
  str="<a xmlns:v=\"";
  m="";

  for (i=0;i<(1024*1024)/2;i++)
    m += "\n";

  for(i=0;i<500;i++)
    str += m ;

  str += "\">f00k</a>";

  var xx = new XML(str);

  printStatus(xx.toXMLString());
}
catch(ex)
{
  expect = 'InternalError: script stack space quota is exhausted';
  actual = ex + '';
  print(actual);
}
TEST(1, expect, actual);
