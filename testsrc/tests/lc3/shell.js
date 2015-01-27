/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestsuite = 'lc3';

// verify that DataTypeClass is on the CLASSPATH

DT = Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass;

if ( typeof DT == "undefined" ) {
    throw "Test Exception:  "+
        "com.netscape.javascript.qa.liveconnect.DataTypeClass "+
        "is not on the CLASSPATH";
}

/*
 * TestCase constructor
 *
 * tests in this directory oddly depend on a 3-argument
 * TestCase constructor, rather than the conventional 4
 */
function TestCase( d, e, a ) {
  this.path = (typeof gTestPath == 'undefined') ?
    (gTestsuite + '/' + gTestsubsuite + '/' + gTestfile) :
    gTestPath;
  this.file = gTestfile;
  this.name        = d; 
  this.description = d;
  this.expect      = e;
  this.actual      = a;
  this.passed      = getTestCaseResult(e, a);
  this.reason      = "";
  this.bugnumber   = typeof(BUGNUMER) != 'undefined' ? BUGNUMBER : '';
  this.type = (typeof window == 'undefined' ? 'shell' : 'browser');
  gTestcases[gTc++] = this;
}
