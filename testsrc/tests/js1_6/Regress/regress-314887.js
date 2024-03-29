/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-314887.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 314887;
var summary = 'Do not crash when morons embed script tags in external script files';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

<script language="JavaScript" type="text/JavaScript">
<!--
//-->
 </script>
 
reportCompare(expect, actual, summary);
