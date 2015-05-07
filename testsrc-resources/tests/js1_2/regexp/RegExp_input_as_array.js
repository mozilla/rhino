/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'RegExp_input_as_array.js';

/**
   Filename:     RegExp_input_as_array.js
   Description:  'Tests RegExps $_ property  (same tests as RegExp_input.js but using $_)'

   Author:       Nick Lerissa
   Date:         March 13, 1998
*/

var SECTION = 'As described in Netscape doc "Whats new in JavaScript 1.2"';
var VERSION = 'no version';
startTest();
var TITLE   = 'RegExp: input';

writeHeaderToLog('Executing script: RegExp_input.js');
writeHeaderToLog( SECTION + " "+ TITLE);

RegExp['$_'] = "abcd12357efg";

// RegExp['$_'] = "abcd12357efg"; RegExp['$_']
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; RegExp['$_']",
	       "abcd12357efg", RegExp['$_']);

// RegExp['$_'] = "abcd12357efg"; /\d+/.exec('2345')
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; /\\d+/.exec('2345')",
	       String(["2345"]), String(/\d+/.exec('2345')));

// RegExp['$_'] = "abcd12357efg"; /\d+/.exec()
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; /\\d+/.exec()",
	       String(["12357"]), String(/\d+/.exec()));

// RegExp['$_'] = "abcd12357efg"; /[h-z]+/.exec()
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; /[h-z]+/.exec()",
	       null, /[h-z]+/.exec());

// RegExp['$_'] = "abcd12357efg"; /\d+/.test('2345')
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; /\\d+/.test('2345')",
	       true, /\d+/.test('2345'));

// RegExp['$_'] = "abcd12357efg"; /\d+/.test()
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; /\\d+/.test()",
	       true, /\d+/.test());

// RegExp['$_'] = "abcd12357efg"; /[h-z]+/.test()
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; /[h-z]+/.test()",
	       false, /[h-z]+/.test());

// RegExp['$_'] = "abcd12357efg"; (new RegExp('\d+')).test()
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; (new RegExp('\d+')).test()",
	       true, (new RegExp('\d+')).test());

// RegExp['$_'] = "abcd12357efg"; (new RegExp('[h-z]+')).test()
RegExp['$_'] = "abcd12357efg";
new TestCase ( SECTION, "RegExp['$_'] = 'abcd12357efg'; (new RegExp('[h-z]+')).test()",
	       false, (new RegExp('[h-z]+')).test());

test();
