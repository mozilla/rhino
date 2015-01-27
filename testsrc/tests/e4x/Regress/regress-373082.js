/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-373082.js';

var BUGNUMBER = 373082;
var summary = 'Simpler sharing of XML and XMLList functions';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

var l;

expect = '<a/>';
l = <><a/></>;
actual = l.toXMLString();
TEST(1, expect, actual);

expect = '<b/>';
l.setName('b');
actual = l.toXMLString();
TEST(2, expect, actual);

expect = '<c/>';
XMLList.prototype.function::setName.call(l, 'c');
actual = l.toXMLString();
TEST(3, expect, actual);

expect = 't';
l = <><a>text</a></>;
actual = l.charAt(0);
TEST(4, expect, actual);

expect = 'TypeError: String.prototype.toString called on incompatible XML';

try
{
    delete XML.prototype.function::toString;
    delete Object.prototype.toString;
    actual = <a>TEXT</a>.toString();
}
catch(ex)
{
    actual = ex + '';
}
TEST(7, expect, actual);

expect = 'TypeError: String.prototype.toString called on incompatible XML';
try
{
    var x = <a><name/></a>;
    x.(name == "Foo");
    print(x.function::name());
}
catch(ex)
{
    actual = ex + '';
}
TEST(8, expect, actual);

try
{
    x = <a><name/></a>;
    x.(name == "Foo");
    print(x.name());
}
catch(ex)
{
    actual = ex + '';
}
TEST(9, expect, actual);

END();
