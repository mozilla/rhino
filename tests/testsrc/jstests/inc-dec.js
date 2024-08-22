load("testsrc/assert.js");

var v1, v2, v3, v4;

assertEquals(NaN, ++v1);
assertEquals(NaN, --v2);
assertEquals(NaN, v3++);
assertEquals(NaN, v4--);

const c1 = undefined, c2 = undefined, c3 = undefined, c4 = undefined;

assertEquals(NaN, ++c1);
assertEquals(NaN, --c2);
assertEquals(NaN, c3++);
assertEquals(NaN, c4--);
assertEquals(undefined, c1);
assertEquals(undefined, c2);
assertEquals(undefined, c3);
assertEquals(undefined, c4);

let l1, l2, l3, l4;

assertEquals(NaN, ++l1);
assertEquals(NaN, --l2);
assertEquals(NaN, l3++);
assertEquals(NaN, l4--);

(function(a1, a2, a3, a4) {
  var v1, v2, v3, v4;

  assertEquals(NaN, ++v1);
  assertEquals(NaN, --v2);
  assertEquals(NaN, v3++);
  assertEquals(NaN, v4--);

  assertEquals(NaN, ++a1);
  assertEquals(NaN, --a2);
  assertEquals(NaN, a3++);
  assertEquals(NaN, a4--);

  // const c1 = undefined, c2 = undefined, c3 = undefined, c4 = undefined;

  // assertThrows('++c1',TypeError);
  // assertThrows('--c2',TypeError);
  // assertThrows('c3++',TypeError);
  // assertThrows('c4--',TypeError);
  // assertEquals(undefined, c1);
  // assertEquals(undefined, c2);
  // assertEquals(undefined, c3);
  // assertEquals(undefined, c4);

  let l1, l2, l3, l4;

  assertEquals(NaN, ++l1);
  assertEquals(NaN, --l2);
  assertEquals(NaN, l3++);
  assertEquals(NaN, l4--);
})();


"success";
