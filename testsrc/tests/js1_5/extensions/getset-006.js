/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Date: 14 April 2001
 *
 * SUMMARY: Testing  obj.__lookupGetter__(), obj.__lookupSetter__()
 * See http://bugzilla.mozilla.org/show_bug.cgi?id=71992
 *
 * Brendan: "I see no need to provide more than the minimum:
 * o.__lookupGetter__('p') returns the getter function for o.p,
 * or undefined if o.p has no getter.  Users can wrap and layer."
 */
//-----------------------------------------------------------------------------
var gTestfile = 'getset-006.js';
var UBound = 0;
var BUGNUMBER = 71992;
var summary = 'Testing  obj.__lookupGetter__(), obj.__lookupSetter__()';
var statprefix = 'Status: ';
var status = '';
var statusitems = [ ];
var actual = '';
var actualvalues = [ ];
var expect= '';
var expectedvalues = [ ];
var cnName = 'name';
var cnColor = 'color';
var cnNonExistingProp = 'ASDF_#_$%';
var cnDEFAULT = 'default name';
var cnFRED = 'Fred';
var cnRED = 'red';
var obj = {};
var obj2 = {};
var s;


// The only setter and getter functions we'll use in the three sections below -
var cnNameSetter = function(newValue) {this._name=newValue; this.nameSETS++;};
var cnNameGetter = function() {this.nameGETS++; return this._name;};



// SECTION1: define getter/setter directly on an object (not its prototype)
obj = new Object();
obj.nameSETS = 0;
obj.nameGETS = 0;
obj.__defineSetter__(cnName, cnNameSetter);
obj.__defineGetter__(cnName, cnNameGetter);
obj.name = cnFRED;
obj.color = cnRED;

status ='In SECTION1 of test; looking up extant getter/setter';
actual = [obj.__lookupSetter__(cnName), obj.__lookupGetter__(cnName)];
expect = [cnNameSetter, cnNameGetter];
addThis();

status = 'In SECTION1 of test; looking up nonexistent getter/setter';
actual = [obj.__lookupSetter__(cnColor), obj.__lookupGetter__(cnColor)];
expect = [undefined, undefined];
addThis();

status = 'In SECTION1 of test; looking up getter/setter on nonexistent property';
actual = [obj.__lookupSetter__(cnNonExistingProp), obj.__lookupGetter__(cnNonExistingProp)];
expect = [undefined, undefined];
addThis();



// SECTION2: define getter/setter in Object.prototype
Object.prototype.nameSETS = 0;
Object.prototype.nameGETS = 0;
Object.prototype.__defineSetter__(cnName, cnNameSetter);
Object.prototype.__defineGetter__(cnName, cnNameGetter);

obj = new Object();
obj.name = cnFRED;
obj.color = cnRED;

status = 'In SECTION2 of test looking up extant getter/setter';
actual = [obj.__lookupSetter__(cnName), obj.__lookupGetter__(cnName)];
expect = [cnNameSetter, cnNameGetter];
addThis();

status = 'In SECTION2 of test; looking up nonexistent getter/setter';
actual = [obj.__lookupSetter__(cnColor), obj.__lookupGetter__(cnColor)];
expect = [undefined, undefined];
addThis();

status = 'In SECTION2 of test; looking up getter/setter on nonexistent property';
actual = [obj.__lookupSetter__(cnNonExistingProp), obj.__lookupGetter__(cnNonExistingProp)];
expect = [undefined, undefined];
addThis();



// SECTION 3: define getter/setter in prototype of user-defined constructor
function TestObject()
{
}
TestObject.prototype.nameSETS = 0;
TestObject.prototype.nameGETS = 0;
TestObject.prototype.__defineSetter__(cnName, cnNameSetter);
TestObject.prototype.__defineGetter__(cnName, cnNameGetter);
TestObject.prototype.name = cnDEFAULT;

obj = new TestObject();
obj.name = cnFRED;
obj.color = cnRED;

status = 'In SECTION3 of test looking up extant getter/setter';
actual = [obj.__lookupSetter__(cnName), obj.__lookupGetter__(cnName)];
expect = [cnNameSetter, cnNameGetter];
addThis();

status = 'In SECTION3 of test; looking up non-existent getter/setter';
actual = [obj.__lookupSetter__(cnColor), obj.__lookupGetter__(cnColor)];
expect = [undefined, undefined];
addThis();

status = 'In SECTION3 of test; looking up getter/setter on nonexistent property';
actual = [obj.__lookupSetter__(cnNonExistingProp), obj.__lookupGetter__(cnNonExistingProp)];
expect = [undefined, undefined];
addThis();



//---------------------------------------------------------------------------------
test();
//---------------------------------------------------------------------------------


function addThis()
{
  statusitems[UBound] = status;
  actualvalues[UBound] = actual.toString();
  expectedvalues[UBound] = expect.toString();
  UBound++;
}


function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  for (var i = 0; i < UBound; i++)
  {
    reportCompare(expectedvalues[i], actualvalues[i], getStatus(i));
  }

  exitFunc ('test');
}


function getStatus(i)
{
  return statprefix + statusitems[i];
}
