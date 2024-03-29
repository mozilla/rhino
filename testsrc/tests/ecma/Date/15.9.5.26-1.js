/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.9.5.26-1.js';

/**    File Name:          15.9.5.26-1.js
       ECMA Section:       15.9.5.26 Date.prototype.setSeconds(sec [,ms])
       Description:

       If ms is not specified, this behaves as if ms were specified with the
       value getMilliseconds( ).

       1.  Let t be the result of LocalTime(this time value).
       2.  Call ToNumber(sec).
       3.  If ms is not specified, compute msFromTime(t); otherwise, call
       ToNumber(ms).
       4.  Compute MakeTime(HourFromTime(t), MinFromTime(t), Result(2),
       Result(3)).
       5.  Compute UTC(MakeDate(Day(t), Result(4))).
       6.  Set the [[Value]] property of the this value to TimeClip(Result(5)).
       7.  Return the value of the [[Value]] property of the this value.

       Author:             christine@netscape.com
       Date:               12 november 1997
*/
var SECTION = "15.9.5.26-1";
var VERSION = "ECMA_1";
startTest();

writeHeaderToLog( SECTION + " Date.prototype.setSeconds(sec [,ms] )");

addNewTestCase( 0, 0, 0,
		"TDATE = new Date(0);(TDATE).setSeconds(0,0);TDATE",
		UTCDateFromTime(SetSeconds(0,0,0)),
		LocalDateFromTime(SetSeconds(0,0,0)) );

addNewTestCase( 28800000,59,999,
		"TDATE = new Date(28800000);(TDATE).setSeconds(59,999);TDATE",
		UTCDateFromTime(SetSeconds(28800000,59,999)),
		LocalDateFromTime(SetSeconds(28800000,59,999)) );

addNewTestCase( 28800000,999,999,
		"TDATE = new Date(28800000);(TDATE).setSeconds(999,999);TDATE",
		UTCDateFromTime(SetSeconds(28800000,999,999)),
		LocalDateFromTime(SetSeconds(28800000,999,999)) );

addNewTestCase( 28800000,999, void 0,
		"TDATE = new Date(28800000);(TDATE).setSeconds(999);TDATE",
		UTCDateFromTime(SetSeconds(28800000,999,0)),
		LocalDateFromTime(SetSeconds(28800000,999,0)) );

addNewTestCase( 28800000,-28800, void 0,
		"TDATE = new Date(28800000);(TDATE).setSeconds(-28800);TDATE",
		UTCDateFromTime(SetSeconds(28800000,-28800)),
		LocalDateFromTime(SetSeconds(28800000,-28800)) );

addNewTestCase( 946684800000,1234567,void 0,
		"TDATE = new Date(946684800000);(TDATE).setSeconds(1234567);TDATE",
		UTCDateFromTime(SetSeconds(946684800000,1234567)),
		LocalDateFromTime(SetSeconds(946684800000,1234567)) );

addNewTestCase( -2208988800000,59,999,
		"TDATE = new Date(-2208988800000);(TDATE).setSeconds(59,999);TDATE",
		UTCDateFromTime(SetSeconds(-2208988800000,59,999)),
		LocalDateFromTime(SetSeconds(-2208988800000,59,999)) );

test();

function addNewTestCase( startTime, sec, ms, DateString,UTCDate, LocalDate) {
  DateCase = new Date( startTime );
  if ( ms != void 0 ) {
    DateCase.setSeconds( sec, ms );
  } else {
    DateCase.setSeconds( sec );
  }

  new TestCase( SECTION, DateString+".getTime()",             UTCDate.value,       DateCase.getTime() );
  new TestCase( SECTION, DateString+".valueOf()",             UTCDate.value,       DateCase.valueOf() );

  new TestCase( SECTION, DateString+".getUTCFullYear()",      UTCDate.year,    DateCase.getUTCFullYear() );
  new TestCase( SECTION, DateString+".getUTCMonth()",         UTCDate.month,  DateCase.getUTCMonth() );
  new TestCase( SECTION, DateString+".getUTCDate()",          UTCDate.date,   DateCase.getUTCDate() );

  new TestCase( SECTION, DateString+".getUTCHours()",         UTCDate.hours,  DateCase.getUTCHours() );
  new TestCase( SECTION, DateString+".getUTCMinutes()",       UTCDate.minutes,DateCase.getUTCMinutes() );
  new TestCase( SECTION, DateString+".getUTCSeconds()",       UTCDate.seconds,DateCase.getUTCSeconds() );
  new TestCase( SECTION, DateString+".getUTCMilliseconds()",  UTCDate.ms,     DateCase.getUTCMilliseconds() );

  new TestCase( SECTION, DateString+".getFullYear()",         LocalDate.year,       DateCase.getFullYear() );
  new TestCase( SECTION, DateString+".getMonth()",            LocalDate.month,      DateCase.getMonth() );
  new TestCase( SECTION, DateString+".getDate()",             LocalDate.date,       DateCase.getDate() );

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
function SetSeconds( t, s, m ) {
  var MS   = ( m == void 0 ) ? msFromTime(t) : Number( m );
  var TIME = LocalTime( t );
  var SEC  = Number(s);
  var RESULT4 = MakeTime( HourFromTime( TIME ),
			  MinFromTime( TIME ),
			  SEC,
			  MS );
  var UTC_TIME = UTC(MakeDate(Day(TIME), RESULT4));
  return ( TimeClip(UTC_TIME) );
}
