load("testsrc/assert.js");
assertEquals(1,Array.prototype.at.length)
assertEquals("b", ["b","c","d"].at(0))
assertEquals("d", ["b","c","d"].at(-1))
assertEquals(3, [1,2,3].at(2))
assertEquals({a:"b"}, [{a:"b"}, {c: "d"}].at(0))
assertEquals(undefined, ["b","c","d"].at(6))

const intArray = new Array(0, 10, -10, 20, -30, 40, -50);
assertEquals(0, intArray.at(0))
assertEquals(-50, intArray.at(-1))
assertEquals(-10, intArray.at(2))
assertEquals(undefined, intArray.at(Infinity))
assertEquals(undefined, intArray.at(11))
assertEquals(0,intArray.at("a"));
assertEquals(0,intArray.at({}));
assertEquals(0,intArray.at(NaN));
assertEquals(0,intArray.at(undefined));

var arrayLike = {
  0: 0,
  length: 0x7fffffff + 1  // Integer.MAX_VALUE + 1
};
assertEquals(0,Array.prototype.at.call(arrayLike, 0));
"success"