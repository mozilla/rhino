/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Date: 23 October 2001
 *
 * SUMMARY: Testing regexps with the global flag set.
 * NOT every substring fitting the given pattern will be matched.
 * The parent string is CONSUMED as successive matches are found.
 *
 * From the ECMA-262 Final spec:
 *
 * 15.10.6.2 RegExp.prototype.exec(string)
 * Performs a regular expression match of string against the regular
 * expression and returns an Array object containing the results of
 * the match, or null if the string did not match.
 *
 * The string ToString(string) is searched for an occurrence of the
 * regular expression pattern as follows:
 *
 * 1.  Let S be the value of ToString(string).
 * 2.  Let length be the length of S.
 * 3.  Let lastIndex be the value of the lastIndex property.
 * 4.  Let i be the value of ToInteger(lastIndex).
 * 5.  If the global property is false, let i = 0.
 * 6.  If i < 0 or i > length then set lastIndex to 0 and return null.
 * 7.  Call [[Match]], giving it the arguments S and i.
 *     If [[Match]] returned failure, go to step 8;
 *     otherwise let r be its State result and go to step 10.
 * 8.  Let i = i+1.
 * 9.  Go to step 6.
 * 10. Let e be r's endIndex value.
 * 11. If the global property is true, set lastIndex to e.
 *
 *          etc.
 *
 *
 * So when the global flag is set, |lastIndex| is incremented every time
 * there is a match; not from i to i+1, but from i to "endIndex" e:
 *
 * e = (index of last input character matched so far by the pattern) + 1
 *
 * Thus in the example below, the first endIndex e occurs after the
 * first match 'a b'. The next match will begin AFTER this, and so
 * will NOT be 'b c', but rather 'c d'. Similarly, 'd e' won't be matched.
 */
//-----------------------------------------------------------------------------
var gTestfile = '15.10.6.2-1.js';
var i = 0;
var BUGNUMBER = '(none)';
var summary = 'Testing regexps with the global flag set';
var status = '';
var statusmessages = new Array();
var pattern = '';
var patterns = new Array();
var string = '';
var strings = new Array();
var actualmatch = '';
var actualmatches = new Array();
var expectedmatch = '';
var expectedmatches = new Array();


status = inSection(1);
string = 'a b c d e';
pattern = /\w\s\w/g;
actualmatch = string.match(pattern);
expectedmatch = ['a b','c d']; // see above explanation -
addThis();


status = inSection(2);
string = '12345678';
pattern = /\d\d\d/g;
actualmatch = string.match(pattern);
expectedmatch = ['123','456'];
addThis();



//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------



function addThis()
{
  statusmessages[i] = status;
  patterns[i] = pattern;
  strings[i] = string;
  actualmatches[i] = actualmatch;
  expectedmatches[i] = expectedmatch;
  i++;
}


function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
  testRegExp(statusmessages, patterns, strings, actualmatches, expectedmatches);
  exitFunc ('test');
}
