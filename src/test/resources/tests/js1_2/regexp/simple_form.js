/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'simple_form.js';

/**
   Filename:     simple_form.js
   Description:  'Tests regular expressions using simple form: re(...)'

   Author:       Nick Lerissa
   Date:         March 19, 1998
*/

var SECTION = 'As described in Netscape doc "Whats new in JavaScript 1.2"';
var VERSION = 'no version';
startTest();
var TITLE   = 'RegExp: simple form';

writeHeaderToLog('Executing script: simple_form.js');
writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase ( SECTION,
	       "/[0-9]{3}/('23 2 34 678 9 09')",
	       String(["678"]), String(/[0-9]{3}/('23 2 34 678 9 09')));

new TestCase ( SECTION,
	       "/3.{4}8/('23 2 34 678 9 09')",
	       String(["34 678"]), String(/3.{4}8/('23 2 34 678 9 09')));

new TestCase ( SECTION,
	       "(/3.{4}8/('23 2 34 678 9 09').length",
	       1, (/3.{4}8/('23 2 34 678 9 09')).length);

var re = /[0-9]{3}/;
new TestCase ( SECTION,
	       "re('23 2 34 678 9 09')",
	       String(["678"]), String(re('23 2 34 678 9 09')));

re = /3.{4}8/;
new TestCase ( SECTION,
	       "re('23 2 34 678 9 09')",
	       String(["34 678"]), String(re('23 2 34 678 9 09')));

new TestCase ( SECTION,
	       "/3.{4}8/('23 2 34 678 9 09')",
	       String(["34 678"]), String(/3.{4}8/('23 2 34 678 9 09')));

re =/3.{4}8/;
new TestCase ( SECTION,
	       "(re('23 2 34 678 9 09').length",
	       1, (re('23 2 34 678 9 09')).length);

new TestCase ( SECTION,
	       "(/3.{4}8/('23 2 34 678 9 09').length",
	       1, (/3.{4}8/('23 2 34 678 9 09')).length);

test();
