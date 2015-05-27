// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

var obj;

obj = {
  a() {
    return 123;
  }
};
assertEquals(123, obj.a());
// assertEquals("a", obj.a.name);

assertEquals("abcefg", {
  abc() {
    return "abc";
  },
  efg() {
    return this.abc() + "efg";
  }
}.efg());

obj = {
  get() {
    return 123;
  }
};
assertEquals(123, obj.get());
// assertEquals("get", obj.get.name);

obj = {
  set() {
    return 123;
  }
};
assertEquals(123, obj.set());
// assertEquals("set", obj.set.name);

assertEquals("\n" +
"function () {\n" +
"    +{f() {\n" +
"        print(1);\n" +
"    }};\n" +
"}\n", (function() { +{ f() { print(1); }}; }).toString());

// Allow reserved word
assertEquals(123, {
  if() {
    return 123;
  }
}.if());

// Allow NumericLiteral
assertEquals(123, {
  123() {
    return 123;
  }
}[123]());

// Allow StringLiteral
assertEquals(123, {
  'abc'() {
    return 123;
  }
}.abc());

// Method is the kind of function, that is non-constructor.
// assertThrows('new (({ a() {} }).a)', TypeError);

var desc = Object.getOwnPropertyDescriptor({
  a() {
    return 123;
  }
}, 'a');
assertEquals(true, desc.writable);
assertEquals(true, desc.enumerable);
assertEquals(true, desc.configurable);

"success";
