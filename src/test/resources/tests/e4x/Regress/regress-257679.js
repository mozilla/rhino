/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-257679.js';

START("Standalone <![CDATA[ .... ]]> should be allowed");
printBugNumber(257679);

var x = <![CDATA[ < some & > arbitrary text ]]>;

var expected = new XML("<![CDATA[ < some & > arbitrary text ]]>");

TEST(1, expected, x);

END();
