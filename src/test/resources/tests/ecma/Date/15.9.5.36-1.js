/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.9.5.36-1.js';

/**
   File Name:          15.9.5.36-1.js
   ECMA Section:       15.9.5.36 Date.prototype.setFullYear(year [, mon [, date ]] )
   Description:

   If mon is not specified, this behaves as if mon were specified with the
   value getMonth( ). If date is not specified, this behaves as if date were
   specified with the value getDate( ).

   1.   Let t be the result of LocalTime(this time value); but if this time
   value is NaN, let t be +0.
   2.   Call ToNumber(year).
   3.   If mon is not specified, compute MonthFromTime(t); otherwise, call
   ToNumber(mon).
   4.   If date is not specified, compute DateFromTime(t); otherwise, call
   ToNumber(date).
   5.   Compute MakeDay(Result(2), Result(3), Result(4)).
   6.   Compute UTC(MakeDate(Result(5), TimeWithinDay(t))).
   7.   Set the [[Value]] property of the this value to TimeClip(Result(6)).
   8.   Return the value of the [[Value]] property of the this value.

   Author:             christine@netscape.com
   Date:               12 november 1997

   Added test cases for Year 2000 Compatilibity Testing.

*/
var SECTION = "15.9.5.36-1";
var VERSION = "ECMA_1";
startTest();

writeHeaderToLog( SECTION + " Date.prototype.setFullYear(year [, mon [, date ]] )");


// 1969

addNewTestCase( "TDATE = new Date(0);(TDATE).setFullYear(1969);TDATE",
		UTCDateFromTime(SetFullYear(0,1969)),
		LocalDateFromTime(SetFullYear(0,1969)) );

addNewTestCase( "TDATE = new Date(0);(TDATE).setFullYear(1969,11);TDATE",
		UTCDateFromTime(SetFullYear(0,1969,11)),
		LocalDateFromTime(SetFullYear(0,1969,11)) );

addNewTestCase( "TDATE = new Date(0);(TDATE).setFullYear(1969,11,31);TDATE",
		UTCDateFromTime(SetFullYear(0,1969,11,31)),
		LocalDateFromTime(SetFullYear(0,1969,11,31)) );

test();

function addNewTestCase( DateString, UTCDate, LocalDate) {
  DateCase = eval( DateString );

  new TestCase( SECTION, DateString+".getTime()",             UTCDate.value,       DateCase.getTime() );
  new TestCase( SECTION, DateString+".valueOf()",             UTCDate.value,       DateCase.valueOf() );

  new TestCase( SECTION, DateString+".getUTCFullYear()",      UTCDate.year,    DateCase.getUTCFullYear() );
  new TestCase( SECTION, DateString+".getUTCMonth()",         UTCDate.month,  DateCase.getUTCMonth() );
  new TestCase( SECTION, DateString+".getUTCDate()",          UTCDate.date,   DateCase.getUTCDate() );
  new TestCase( SECTION, DateString+".getUTCDay()",           UTCDate.day,    DateCase.getUTCDay() );
  new TestCase( SECTION, DateString+".getUTCHours()",         UTCDate.hours,  DateCase.getUTCHours() );
  new TestCase( SECTION, DateString+".getUTCMinutes()",       UTCDate.minutes,DateCase.getUTCMinutes() );
  new TestCase( SECTION, DateString+".getUTCSeconds()",       UTCDate.seconds,DateCase.getUTCSeconds() );
  new TestCase( SECTION, DateString+".getUTCMilliseconds()",  UTCDate.ms,     DateCase.getUTCMilliseconds() );

  new TestCase( SECTION, DateString+".getFullYear()",         LocalDate.year,       DateCase.getFullYear() );
  new TestCase( SECTION, DateString+".getMonth()",            LocalDate.month,      DateCase.getMonth() );
  new TestCase( SECTION, DateString+".getDate()",             LocalDate.date,       DateCase.getDate() );
  new TestCase( SECTION, DateString+".getDay()",              LocalDate.day,        DateCase.getDay() );
  new TestCase( SECTION, DateString+".getHours()",            LocalDate.hours,      DateCase.getHours() );
  new TestCase( SECTION, DateString+".getMinutes()",          LocalDate.minutes,    DateCase.getMinutes() );
  new TestCase( SECTION, DateString+".getSeconds()",          LocalDate.seconds,    DateCase.getSeconds() );
  new TestCase( SECTION, DateString+".getMilliseconds()",     LocalDate.ms,         DateCase.getMilliseconds() );

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
  var d = new MyDate();
  d.year = YearFromTime(t);
  d.month = MonthFromTime(t);
  d.date = DateFromTime(t);
  d.hours = HourFromTime(t);
  d.minutes = MinFromTime(t);
  d.seconds = SecFromTime(t);
  d.ms = msFromTime(t);

  d.time = MakeTime( d.hours, d.minutes, d.seconds, d.ms );
  d.value = TimeClip( MakeDate( MakeDay( d.year, d.month, d.date ), d.time ) );
  d.day = WeekDay( d.value );

  return (d);
}
function SetFullYear( t, year, mon, date ) {
  var T = ( isNaN(t) ) ? 0 : LocalTime(t) ;
  var YEAR = Number( year );
  var MONTH = ( mon == void 0 ) ? MonthFromTime(T) : Number( mon );
  var DATE = ( date == void 0 ) ? DateFromTime(T)  : Number( date );

  var DAY = MakeDay( YEAR, MONTH, DATE );
  var UTC_DATE = UTC(MakeDate( DAY, TimeWithinDay(T)));

  return ( TimeClip(UTC_DATE) );
}
