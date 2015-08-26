/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-327564.js';

var summary = "Hang due to cycle in XML object";
var BUGNUMBER = 327564;
var actual = 'No Cycle Detected';
var expect = 'Error: cyclic XML value';

printBugNumber(BUGNUMBER);
START(summary);

var p = <p/>;

p.c = 1;

var c = p.c[0];

p.insertChildBefore(null,c);

printStatus(p.toXMLString());

printStatus('p.c[1] === c');
TEST(1, true, p.c[1] === c);

p.c = 2

try
{
    c.appendChild(p)
    // iloop here
}
catch(ex)
{
    actual = ex + '';
    printStatus(actual);
}

TEST(2, expect, actual);

END();
