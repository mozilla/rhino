/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'control_characters.js';

/**
   Filename:     control_characters.js
   Description:  'Tests regular expressions containing .'

   Author:       Nick Lerissa
   Date:         April 8, 1998
*/

var SECTION = 'As described in Netscape doc "Whats new in JavaScript 1.2"';
var VERSION = 'no version';
var TITLE = 'RegExp: .';
var BUGNUMBER="123802";

startTest();
writeHeaderToLog('Executing script: control_characters.js');
writeHeaderToLog( SECTION + " "+ TITLE);


// '�O� �:i��'.match(new RegExp('.+'))
new TestCase ( SECTION, "'�O� �:i��'.match(new RegExp('.+'))",
	       String(['�O� �:i��']), String('�O� �:i��'.match(new RegExp('.+'))));

// string1.match(new RegExp(string1))
var string1 = '�O� �:i��';
new TestCase ( SECTION, "string1 = " + string1 + " string1.match(string1)",
	       String([string1]), String(string1.match(string1)));

string1 = "";
for (var i = 0; i < 32; i++)
  string1 += String.fromCharCode(i);
new TestCase ( SECTION, "string1 = " + string1 + " string1.match(string1)",
	       String([string1]), String(string1.match(string1)));

test();
