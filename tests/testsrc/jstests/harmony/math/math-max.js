load("testsrc/assert.js");

assertEquals(Math.max(), -Infinity);
assertEquals(Math.max(1, 2), 2);
assertEquals(Math.max(2, 1), 2);
assertEquals(Math.max(-0, 0), 0);
assertEquals(Math.max(0, -0), 0);
assertEquals(Math.max(NaN, 1), NaN);
assertEquals(Math.max(1, NaN), NaN);
assertEquals(Math.max(-Infinity), -Infinity);
assertEquals(Math.max(Infinity, -Infinity), Infinity);
assertEquals(Math.max(1, 2, 3), 3);
assertEquals(Math.max(-1, -2), -1);

"success";
