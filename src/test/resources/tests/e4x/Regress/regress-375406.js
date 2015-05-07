/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-375406.js';

var summary = 'Do not crash @ PutProperty setting <a/>.attribute("")[0]';
var BUGNUMBER = 375406;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

<a/>.attribute('')[0] = 1;

TEST(1, expect, actual);

END();
