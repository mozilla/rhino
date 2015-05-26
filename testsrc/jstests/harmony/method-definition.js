// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

assertEquals(123, {
  a() {
    return 123;
  }
}.a());

assertEquals("abcefg", {
  abc() {
    return "abc";
  },
  efg() {
    return this.abc() + "efg";
  }
}.efg());

assertEquals("\n" +
"function () {\n" +
"    +{f() {\n" +
"        print(1);\n" +
"    }};\n" +
"}\n", (function() { +{ f() { print(1); }}; }).toString());

// Allow reserved word
// assertEquals(123, {
//   if() {
//     return 123;
//   }
// }.a());

// Allow NumericLiteral
// assertEquals(123, {
//   123() {
//     return 123;
//   }
// }[123]());

// Allow StringLiteral
// assertEquals(123, {
//   'abc'() {
//     return 123;
//   }
// }.abc());

// Method is the kind of function, that is non-constructor.
// assertThrows('new (({ a() {} }).a)', TypeError);

// assertEquals(, Object.getOwnPropertyDescriptor({
//   a() {
//     return 123;
//   }
// }, 'a'));

"success";
