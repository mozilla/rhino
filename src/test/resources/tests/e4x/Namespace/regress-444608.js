/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-444608.js';

var summary = '13.2 Namespaces - call constructors directly';
var BUGNUMBER = 444608;
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
START(summary);

var x = <xml/>;
Namespace = function() { return 10; };
x.addNamespace("x");

TEST(1, expect, actual);

END();
