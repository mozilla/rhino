// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

// Test that ECMA-262 abstract method "LengthOfArrayLike" works on
// XMLObject, which has a length method instead of length property.
// The method is declared package-private, so we'll call it
// indirectly instead.

load('testsrc/assert.js');

var xml = <xml><a>1</a><b>2</b><a>3</a><c>4</c></xml>;
var {find, map, slice} = Array.prototype;

var expected = 4;
var actual = slice.call(xml.children(), 0).length;
assertEquals(expected, actual);

var expected = [<b>2</b>, <a>3</a>];
var actual = slice.call(xml.children(), 1, 3);
assertEquals(expected, actual);

var expected = xml.a[1];
var actual = find.call(xml.children(), x => x.toString() === "3");
assertEquals(expected, actual);

var expected = ["<a>1</a>", "<b>2</b>", "<a>3</a>", "<c>4</c>"];
var actual = map.call(xml.children(), x => x.toXMLString());
assertEquals(expected, actual);

"success"
