/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-364017.js';

var BUGNUMBER = 364017;
var summary = 'Do not assert map->vector && i < map->length';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
START(summary);

if (typeof dis != 'undefined')
{
    dis( function() {
        XML.prototype.function::toString = function() { return "foo"; };
    });
}

TEST(1, expect, actual);

END();
