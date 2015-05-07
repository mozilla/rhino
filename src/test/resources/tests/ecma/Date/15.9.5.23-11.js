/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.9.5.23-11.js';

/**
   File Name:          15.9.5.23-1.js
   ECMA Section:       15.9.5.23 Date.prototype.setTime(time)
   Description:

   1.  If the this value is not a Date object, generate a runtime error.
   2.  Call ToNumber(time).
   3.  Call TimeClip(Result(1)).
   4.  Set the [[Value]] property of the this value to Result(2).
   5.  Return the value of the [[Value]] property of the this value.

   Author:             christine@netscape.com
   Date:               12 november 1997

*/
var SECTION = "15.9.5.23-1";
var VERSION = "ECMA_1";
startTest();
var TITLE = "Date.prototype.setTime()";

writeHeaderToLog( SECTION + " Date.prototype.setTime(time)");

var now = "now";
addTestCase( now, -86400000 );

test();


function addTestCase( startTime, setTime ) {
  if ( startTime == "now" ) {
    DateCase = new Date();
  } else {
    DateCase = new Date( startTime );
  }

  DateCase.setTime( setTime );
  var DateString = "var d = new Date("+startTime+"); d.setTime("+setTime+"); d" ;
  var UTCDate   = UTCDateFromTime ( Number(setTime) );
  var LocalDate = LocalDateFromTime( Number(setTime) );

  new TestCase( SECTION, DateString+".getTime()",             UTCDate.value,      DateCase.getTime() );
  new TestCase( SECTION, DateString+".valueOf()",             UTCDate.value,      DateCase.valueOf() );

  new TestCase( SECTION, DateString+".getUTCFullYear()",      UTCDate.year,       DateCase.getUTCFullYear() );
  new TestCase( SECTION, DateString+".getUTCMonth()",         UTCDate.month,      DateCase.getUTCMonth() );
  new TestCase( SECTION, DateString+".getUTCDate()",          UTCDate.date,       DateCase.getUTCDate() );
  new TestCase( SECTION, DateString+".getUTCDay()",           UTCDate.day,        DateCase.getUTCDay() );
  new TestCase( SECTION, DateString+".getUTCHours()",         UTCDate.hours,      DateCase.getUTCHours() );
  new TestCase( SECTION, DateString+".getUTCMinutes()",       UTCDate.minutes,    DateCase.getUTCMinutes() );
  new TestCase( SECTION, DateString+".getUTCSeconds()",       UTCDate.seconds,    DateCase.getUTCSeconds() );
  new TestCase( SECTION, DateString+".getUTCMilliseconds()",  UTCDate.ms,         DateCase.getUTCMilliseconds() );

  new TestCase( SECTION, DateString+".getFullYear()",         LocalDate.year,     DateCase.getFullYear() );
  new TestCase( SECTION, DateString+".getMonth()",            LocalDate.month,    DateCase.getMonth() );
  new TestCase( SECTION, DateString+".getDate()",             LocalDate.date,     DateCase.getDate() );
  new TestCase( SECTION, DateString+".getDay()",              LocalDate.day,      DateCase.getDay() );
  new TestCase( SECTION, DateString+".getHours()",            LocalDate.hours,    DateCase.getHours() );
  new TestCase( SECTION, DateString+".getMinutes()",          LocalDate.minutes,  DateCase.getMinutes() );
  new TestCase( SECTION, DateString+".getSeconds()",          LocalDate.seconds,  DateCase.getSeconds() );
  new TestCase( SECTION, DateString+".getMilliseconds()",     LocalDate.ms,       DateCase.getMilliseconds() );

  DateCase.toString = Object.prototype.toString;

  new TestCase( SECTION,
		DateString+".toString=Object.prototype.toString;"+DateString+".toString()",
		"[object Date]",
		DateCase.toString() );
}
function MyDate() {
  this.year = 0;
  this.month = 0;
  this.date = 0;
  this.hours = 0;
  this.minutes = 0;
  this.seconds = 0;
  this.ms = 0;
}
function LocalDateFromTime(t) {
  t = LocalTime(t);
  return ( MyDateFromTime(t) );
}
function UTCDateFromTime(t) {
  return ( MyDateFromTime(t) );
}
function MyDateFromTime( t ) {
  var d       = new MyDate();
  d.year      = YearFromTime(t);
  d.month     = MonthFromTime(t);
  d.date      = DateFromTime(t);
  d.hours     = HourFromTime(t);
  d.minutes   = MinFromTime(t);
  d.seconds   = SecFromTime(t);
  d.ms        = msFromTime(t);
  d.time      = MakeTime( d.hours, d.minutes, d.seconds, d.ms );
  d.value     = TimeClip( MakeDate( MakeDay( d.year, d.month, d.date ), d.time ) );
  d.day       = WeekDay( d.value );
  return (d);
}


