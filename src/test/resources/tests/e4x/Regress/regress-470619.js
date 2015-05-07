/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-470619.js';

var summary = 'Do not assert: regs.sp - 2 >= StackBase(fp)';
var BUGNUMBER = 470619;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

try
{
    y = <x/>;
    for (var z = 0; z < 2; ++z) { +y };
}
catch(ex)
{
    print(ex + '');
}
TEST(1, expect, actual);

END();
