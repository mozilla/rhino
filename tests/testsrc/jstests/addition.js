load("testsrc/assert.js");

let o = ({ toString: () => "foo", valueOf: () => 123 });

assertEquals("123bar", o + "bar");
assertEquals("bar123", "bar" + o);

assertEquals(1123, o + 1000);
assertEquals(1123, 1000 + o);

let stuff = "bar";
assertEquals("123bar", o + stuff);
assertEquals("bar123", stuff + o);

assertEquals("123bar", o + String("bar"));
assertEquals("bar123", String("bar") + o);

"success";
