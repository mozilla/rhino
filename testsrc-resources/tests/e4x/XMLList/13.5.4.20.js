/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '13.5.4.20.js';

START("13.5.4.20 - XMLList toString()");

TEST(1, true, XMLList.prototype.hasOwnProperty("toString"));
   
x = <><alpha>one</alpha></>;

TEST(2, "one", x.toString());

x = <><alpha>one</alpha><bravo>two</bravo></>;

TEST(3, "<alpha>one</alpha>\n<bravo>two</bravo>", x.toString());

END();