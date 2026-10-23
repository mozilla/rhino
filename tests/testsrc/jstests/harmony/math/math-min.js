load("testsrc/assert.js");

assertEquals(Math.min(), Infinity);
assertEquals(Math.min(1, 2), 1);
assertEquals(Math.min(2, 1), 1);
assertEquals(Math.min(-0, 0), -0);
assertEquals(Math.min(0, -0), -0);
assertEquals(Math.min(NaN, 1), NaN);
assertEquals(Math.min(1, NaN), NaN);
assertEquals(Math.min(Infinity), Infinity);
assertEquals(Math.min(Infinity, -Infinity), -Infinity);
assertEquals(Math.min(1, 2, 3), 1);
assertEquals(Math.min(-1, -2), -2);

"success";
