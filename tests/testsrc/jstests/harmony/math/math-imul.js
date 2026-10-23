load("testsrc/assert.js");

assertEquals(Math.imul(), 0);
assertEquals(Math.imul(2, 3), 6);
assertEquals(Math.imul(-1, 8), -8);
assertEquals(Math.imul(-5, 12), -60);
assertEquals(Math.imul(4294967295, 5), -5);
assertEquals(Math.imul(4294967294, 5), -10);
assertEquals(Math.imul(2147483647, 2), -2);
assertEquals(Math.imul(NaN, 1), 0);
assertEquals(Math.imul(Infinity, 1), 0);
assertEquals(Math.imul(4294967299, 2), 6);
assertEquals(Math.imul(1.9, 2.9), 2);
assertEquals(Math.imul(-0, 5), 0);

"success";
