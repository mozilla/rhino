/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 *
 * Date:    28 Feb 2002
 * SUMMARY: Testing that Error.stack distinguishes between:
 *
 * A) top-level calls: myFunc();
 * B) no-name function calls: function() { myFunc();} ()
 *
 * The stack frame for A) should begin with '@'
 * The stack frame for B) should begin with '()'
 *
 * This behavior was coded by Brendan during his fix for bug 127136.
 * See http://bugzilla.mozilla.org/show_bug.cgi?id=127136#c13
 *
 * Note: our function getStackFrames(err) orders the array of stack frames
 * so that the 0th element will correspond to the highest frame, i.e. will
 * correspond to a line in top-level code. The 1st element will correspond
 * to the function that is called first, and so on...
 *
 * NOTE: At present Rhino does not have an Error.stack property. It is an
 * ECMA extension, see http://bugzilla.mozilla.org/show_bug.cgi?id=123177
 */
//-----------------------------------------------------------------------------
var gTestfile = 'errstack-001.js';
var UBound = 0;
var BUGNUMBER = '(none)';
var summary = 'Testing Error.stack';
var status = '';
var statusitems = [];
var actual = '';
var actualvalues = [];
var expect= '';
var expectedvalues = [];
var myErr = '';
var stackFrames = '';


function A(x,y)
{
  return B(x+1,y+1);
}

function B(x,z)
{
  return C(x+1,z+1);
}

function C(x,y)
{
  return D(x+1,y+1);
}

function D(x,z)
{
  try
  {
    throw new Error('meep!');
  }
  catch (e)
  {
    return e;
  }
}


myErr = A(44,13);
stackFrames = getStackFrames(myErr);
status = inSection(1);
actual = stackFrames[0].substring(0,1);
expect = '@';
addThis();

status = inSection(2);
actual = stackFrames[1].substring(0,9);
expect = 'A(44,13)@';
addThis();

status = inSection(3);
actual = stackFrames[2].substring(0,9);
expect = 'B(45,14)@';
addThis();

status = inSection(4);
actual = stackFrames[3].substring(0,9);
expect = 'C(46,15)@';
addThis();

status = inSection(5);
actual = stackFrames[4].substring(0,9);
expect = 'D(47,16)@';
addThis();



myErr = A('44:foo','13:bar');
stackFrames = getStackFrames(myErr);
status = inSection(6);
actual = stackFrames[0].substring(0,1);
expect = '@';
addThis();

status = inSection(7);
actual = stackFrames[1].substring(0,21);
expect = 'A("44:foo","13:bar")@';
addThis();

status = inSection(8);
actual = stackFrames[2].substring(0,23);
expect = 'B("44:foo1","13:bar1")@';
addThis();

status = inSection(9);
actual = stackFrames[3].substring(0,25);
expect = 'C("44:foo11","13:bar11")@';
addThis();

status = inSection(10);
actual = stackFrames[4].substring(0,27);
expect = 'D("44:foo111","13:bar111")@';;
addThis();



/*
 * Make the first frame occur in a function with an empty name -
 */
myErr = function() { return A(44,13); } ();
stackFrames = getStackFrames(myErr);
status = inSection(11);
actual = stackFrames[0].substring(0,1);
expect = '@';
addThis();

status = inSection(12);
actual = stackFrames[1].substring(0,3);
expect = '()@';
addThis();

status = inSection(13);
actual = stackFrames[2].substring(0,9);
expect = 'A(44,13)@';
addThis();

// etc. for the rest of the frames as above



/*
 * Make the first frame occur in a function with name 'anonymous' -
 */
var f = Function('return A(44,13);');
myErr = f();
stackFrames = getStackFrames(myErr);
status = inSection(14);
actual = stackFrames[0].substring(0,1);
expect = '@';
addThis();

status = inSection(15);
actual = stackFrames[1].substring(0,12);
expect = 'anonymous()@';
addThis();

status = inSection(16);
actual = stackFrames[2].substring(0,9);
expect = 'A(44,13)@';
addThis();

// etc. for the rest of the frames as above



/*
 * Make a user-defined error via the Error() function -
 */
var message = 'Hi there!'; var fileName = 'file name'; var lineNumber = 0;
myErr = Error(message, fileName, lineNumber);
stackFrames = getStackFrames(myErr);
status = inSection(17);
actual = stackFrames[0].substring(0,1);
expect = '@';
addThis();


/*
 * Now use the |new| keyword. Re-use the same params -
 */
myErr = new Error(message, fileName, lineNumber);
stackFrames = getStackFrames(myErr);
status = inSection(18);
actual = stackFrames[0].substring(0,1);
expect = '@';
addThis();




//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------



/*
 * Split the string |err.stack| along its '\n' delimiter.
 * As of 2002-02-28 |err.stack| ends with the delimiter, so
 * the resulting array has an empty string as its last element.
 *
 * Pop that useless element off before doing anything.
 * Then reverse the array, for convenience of indexing -
 */
function getStackFrames(err)
{
  var arr = err.stack.split('\n');
  arr.pop();
  return arr.reverse();
}


function addThis()
{
  statusitems[UBound] = status;
  actualvalues[UBound] = actual;
  expectedvalues[UBound] = expect;
  UBound++;
}


function test()
{
  enterFunc('test');
  printBugNumber(BUGNUMBER);
  printStatus(summary);

  for (var i=0; i<UBound; i++)
  {
    reportCompare(expectedvalues[i], actualvalues[i], statusitems[i]);
  }

  exitFunc ('test');
}
