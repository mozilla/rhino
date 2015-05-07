/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'replace-001.js';

/**
 *  File Name:          String/replace-001.js
 *  ECMA Section:       15.6.4.10
 *  Description:        Based on ECMA 2 Draft 7 February 1999
 *
 *  Author:             christine@netscape.com
 *  Date:               19 February 1999
 */

var SECTION = "String/replace-001.js";
var VERSION = "ECMA_2";
var TITLE   = "String.prototype.replace( regexp, replaceValue )";

startTest();

/*
 * If regexp is not an object of type RegExp, it is replaced with the
 * result of the expression new RegExp(regexp).  Let string denote the
 * result of converting the this value to a string.  String is searched
 * for the first occurrence of the regular expression pattern regexp if
 * regexp.global is false, or all occurrences if regexp.global is true.
 *
 * The match is performed as in String.prototype.match, including the
 * update of regexp.lastIndex.  Let m be the number of matched
 * parenthesized subexpressions as specified in section 15.7.5.3.
 *
 * If replaceValue is a function, then for each matched substring, call
 * the function with the following m + 3 arguments. Argument 1 is the
 * substring that matched. The next m arguments are all of the matched
 * subexpressions. Argument m + 2 is the length of the left context, and
 * argument m + 3 is string.
 *
 * The result is a string value derived from the original input by
 * replacing each matched substring with the corresponding return value
 * of the function call, converted to a string if need be.
 *
 * Otherwise, let newstring denote the result of converting replaceValue
 * to a string. The result is a string value derived from the original
 * input string by replacing each matched substring with a string derived
 * from newstring by replacing characters in newstring by replacement text
 * as specified in the following table:
 *
 * $& The matched substring.
 * $� The portion of string that precedes the matched substring.
 * $� The portion of string that follows the matched substring.
 * $+ The substring matched by the last parenthesized subexpressions in
 *      the regular expression.
 * $n The corresponding matched parenthesized subexpression n, where n
 * is a single digit 0-9. If there are fewer than n subexpressions, �$n
 * is left unchanged.
 *
 * Note that the replace function is intentionally generic; it does not
 * require that its this value be a string object. Therefore, it can be
 * transferred to other kinds of objects for use as a method.
 */


AddTestCase( "NO TESTS EXIST", "PASSED", "Test not implemented");

test();
