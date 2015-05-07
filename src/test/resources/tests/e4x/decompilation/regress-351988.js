/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-351988.js';

var summary = 'decompilation of XMLPI object initializer';
var BUGNUMBER = 351988;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

var f;
f = function() { var y = <?foo bar?>; }
actual = f + '';
expect = 'function () {\n    var y = <?foo bar?>;\n}';

compareSource(expect, actual, inSection(1) + summary);

END();
