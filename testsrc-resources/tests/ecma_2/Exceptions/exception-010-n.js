/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'exception-010-n.js';

var SECTION = "exception-010";
var VERSION = "ECMA_2";
startTest();
var TITLE   = "Don't Crash throwing null";

writeHeaderToLog( SECTION + " "+ TITLE);
print("Null throw test.");
print("BUGNUMBER: 21799");

DESCRIPTION = "throw null";
EXPECTED = "error";

new TestCase( SECTION,  "throw null",     "error",    eval("throw null" ));

test();

print("FAILED!: Should have exited with uncaught exception.");


