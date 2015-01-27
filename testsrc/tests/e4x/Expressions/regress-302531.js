/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-302531.js';

var summary = "E4X QuoteString should deal with empty string";
var BUGNUMBER = 302531;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

function f(e) {
  return <e {e}="" />;
}

XML.ignoreWhitespace = true;
XML.prettyPrinting = true;

expect = (
    <e foo="" />
    ).toXMLString().replace(/</g, '&lt;');

actual = f('foo').toXMLString().replace(/</g, '&lt;');

TEST(1, expect, actual);

END();
